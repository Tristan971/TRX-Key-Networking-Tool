package ch.epfl.trx.Fetcher;

import ch.epfl.trx.Tools.SQLHandler;
import com.jcraft.jsch.JSchException;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Class modelizing Twitter data
 * Does anything twitter-only related
 *
 * @author Tristan Deloche
 */

public class TwitterHandler {

    String output = "BEGINNING OF TWEETS COLLECTED FOR HASHTAG #trx_curve_";
    Twitter twitter = null;

    /**
     * Constructon embedding credentials for the twitter account. Do not disclose bla bla bla.
     * See dev.twitter.com's "my apps" panel for more info it's pretty obvious once you're there.
     */
    public TwitterHandler() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("")
                .setOAuthConsumerSecret("")
                .setOAuthAccessToken("")
                .setOAuthAccessTokenSecret("");
        TwitterFactory tf = new TwitterFactory(cb.build());
        this.twitter = tf.getInstance();
    }

    /**
     * Publishes the "beginning entropy..." tweet for said security/hastag.
     * @param security Security of the curve
     * @param hashtag Hashtag to use. Format is "XXXXX" and /!NOT!/ "#trx_curve_XXXXX".
     */
    public void startEntropyStatus(String security, String hashtag) {
        try {
            twitter.updateStatus("New elliptic curve calculation beginning. Security : "+security+" bits. Add entropy by tweeting with #trx_curve_"+hashtag);
        } catch (TwitterException e) {
            System.out.println("Couldn't update status");
            e.printStackTrace();
        }
    }

    /**
     * Same as startEntropyStatus() except for the "entropy collection finished..." tweet.
     * @param hashtag hashtag to "end"
     */
    public void closeEntropyStatus(String hashtag) {
        try {
            twitter.updateStatus("Entropy collection for the following curve is now over -> #trx_curve_"+hashtag);
        } catch (TwitterException e) {
            System.out.println("Couldn't update status");
            e.printStackTrace();
        }
    }

    /**
     * Searches for all the tweets published with a said hashtag (takes in account tweets published until around 30 SECONDS BEFORE THIS ACTIVATES)
     * This is a twitter limitation. I hate major big data services always trying to save processing time here and there but what can you do ?
     *
     * Anyway look into the twitter4J library for more info on the syntax.
     * Basically setting up to 100 (max) number of tweets taken into account (thank you again twitter...)
     * Then formats all the tweets so they look nice (twitter API is in JSON and JSON is pretty ugly). Again see Twitter4J's Javadoc.
     * prints the number of tweets and which of them. Mostly for debugging purposes but it shouldn't show too much ever so you can let it here.
     *
     * Then calls closeEntropyStatus() to automatically close after search is done.
     *
     * @param hashtag Hahstag to search for. Format XXXX and not #trx_curve_XXXX again.
     */
    public void executeSearch(String hashtag) {
        output+=hashtag+" ---\n";
        Query query = new Query("#trx_curve_"+hashtag);
        query.count(100);

        QueryResult result = null;
        try {
            result = twitter.search(query);
        } catch (TwitterException e) {
            System.out.println("Something happened during the tweet query.");
            e.printStackTrace();
        }
        if (result != null) {
            for (Status status : result.getTweets()) {
                output+="@" + status.getUser().getScreenName() + ":" + status.getText() +"\n";
            }
            System.out.println("Number of tweets found : "+result.getTweets().size());
            System.out.println("Found tweets are : "+output);
        } else {
            System.out.println("No tweets found.");
        }

        closeEntropyStatus(hashtag);
    }

    /**
     * Writes the tweets to a single text file under the text subdirectory.
     * Throws a bunch of exceptions related to file handling in JAVA. If you have permission to write all will go well.
     *
     * Mostly creates a fileoutput. Converts the "output" class-level variable into bytes and write them one by one into the file.
     * Could use a buffer but 100 tweets is max 14000 characters for it wouldn't even be worth it due to the small number of bytes.
     *
     * After writing it the sqlhandler.addTweetsFile will automaticall upload it and add the resulting URL into the SQL database.
     *
     * @param T0_SECURITY Related security, only here for the sqlHandler to know what to update.
     * @param hashtag The hashtag since the tweets file needs to have it in its filename, and for sqlHandler to find it afterwards too.
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws JSchException
     */
    public void writeTweetsToFile(String T0_SECURITY, String hashtag) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, JSchException {
        File file = new File("text/tweets_"+T0_SECURITY+".txt");

        try (FileOutputStream fop = new FileOutputStream(file)) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();

            byte[] contentInBytes = output.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();

            System.out.println("Tweets file : written.");

            SQLHandler sqlHandler = new SQLHandler();
            sqlHandler.addTweetsFile(T0_SECURITY, hashtag);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
