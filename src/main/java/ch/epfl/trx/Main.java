package ch.epfl.trx;

import ch.epfl.trx.Fetcher.TwitterHandler;
import ch.epfl.trx.Sender.PrepareMapForStep;
import ch.epfl.trx.Tools.SQLHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * Main class of the TRX project
 * Good luck with maintaining this.
 *
 * @author Tristan Deloche
 * Contact at tristandeloche@gmail.com if anything ever happens that you can't deal with.
 */

public class Main {

    /**
     * General logic is as follows.
     *
     * Publish :
     * You get a file describing the data to publish at current step.
     * You format it in a nice HashMap so SQL requests are easier with the "PrepareMapForStep" function
     * You then use the returned HashMap with the SQLHandler object which (so surprising right) handles the SQL side of things.
     *
     * Fetch :
     * In case of a fetch (tweets) request.
     * Hashtag is used to perform search witht he TwitterHandler.executeSearch() method.
     * Results are written in a text file, which is uploaded to the server with writeTweetsToFile() method, the uploaded file's URL is then published.
     *
     * @param args Arguments, duh
     * @throws Exception In a million cases but only because of lower-level methods, so look into them rather than this one.
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 3) {

            /**
             * IF ARGS[0] == 1, GOTO SEND MODE ; IF ARGS[0] == 0 or tweets, GO TO FETCH MODE. ELSE, THROW EXCEPTION.
             */
            System.out.println("ARGUMENTS USED : \n"+Arrays.toString(args));
            //Check arguments
            if (Objects.equals(args[0], "1")) {
                int step = Integer.valueOf(args[1]);
                String dataFilePath = args[2];

                //Hashmap containing the data. Used to be called statically but prevented autoclean feature,
                //thus the apparently useless variable here.
                HashMap<String, String> wonderfulHashMap;

                //Makes things clearer than static calls
                SQLHandler handler = new SQLHandler();

                switch (step) {
                //Add new curve to CURRENTLY_CALCULATING table.
                case 0:
                    wonderfulHashMap = PrepareMapForStep.exportMap(0,dataFilePath);

                    //Removes the remnants of any aborted curve of this security. Avoids everything being fucked up in case one in a million curves
                    //fails, since the applet updates curve data on DB instead of recreating every time and then can get lost if multiple
                    // same-security curves exist in the CURRENTLY_CALCULATING table (it's ugly but MUCH faster than recreating)
                    handler.cleanCurrently(wonderfulHashMap.get("T0_SECURITY"));

                    handler.step0(wonderfulHashMap);
                    break;
                case 1:
                    //Since steps 123 are similar from an SQL aspect they are basically the same method with a few IFs
                    //Thus pls don't remove these apparently redundant 1/2/3 arguments.
                    //Or copypaste 90% of the function and make 3 different ones. As you wish.
                    wonderfulHashMap = PrepareMapForStep.exportMap(1, dataFilePath);
                    handler.step123(wonderfulHashMap, "1");
                    break;
                case 2:
                    wonderfulHashMap = PrepareMapForStep.exportMap(2, dataFilePath);
                    handler.step123(wonderfulHashMap, "2");
                    break;
                case 3:
                    wonderfulHashMap = PrepareMapForStep.exportMap(3, dataFilePath);
                    handler.step123(wonderfulHashMap, "3");
                    break;
                case 4:
                    wonderfulHashMap = PrepareMapForStep.exportMap(4, dataFilePath);
                    handler.step4(wonderfulHashMap);
                    break;
                default: throw new IllegalArgumentException("BAD STEP NUMBER : "+step);
                }



                // Having two different arguments for the same thing is in no way normal
                // but is very useful for testing.
                // If you need to test that the twitter search works, instead of trying with our hashtags you can change
                // one of these two and try it locally with a more "popular" hashtag to be sure of how many tweets
                // it is able to retrieve etc etc.
                // Now which one you can safely change... Ask Benjamin/Patricia cause idk anymore.
            } else if (Objects.equals(args[0], "0")) {
                TwitterHandler twitterHandler = new TwitterHandler();
                twitterHandler.executeSearch(args[2]);
                twitterHandler.writeTweetsToFile(args[1], args[2]);
            } else if (Objects.equals(args[0], "tweets")) {
                TwitterHandler twitterHandler = new TwitterHandler();
                twitterHandler.executeSearch(args[2]);
                twitterHandler.writeTweetsToFile(args[1], args[2]);
            } else {
                throw new IllegalArgumentException("Illegal argument. 1 = SEND. 0 = FETCH. !{1,0} = THROW THIS EXCEPTION.");
            }

        // ENTER IF NO (OR NOT ENOUGH) ARGUMENTS.
        // Also VERY useful to check if JAVA works correctly on the computer. We had awkward issues
        // with it and it helped a lot knowing if the program actually even ran. You'll be happy to see this error one day.
        } else {
            throw new IllegalArgumentException("Missing parameters, please specify the requested actions to perform.");
        }
    }
}
