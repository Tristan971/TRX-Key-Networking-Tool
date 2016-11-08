package ch.epfl.trx.Tools;

import ch.epfl.trx.Sender.UploadHandler;
import com.jcraft.jsch.JSchException;

import java.io.IOException;
import java.sql.*;
import java.util.*;


/**
 * SQL-Handling class simplifying the JDBC usage in TRX applet project
 * Basically "useless". But since JDBC is a horrific library, meant to be used
 * by a race of super-humans who need no valid documentation to code, and I am
 * not that, this class allows for some easy reuse of similar functions.
 *
 * FIRST ADVICE : NEVER BELIEVE JDBC/ORACLE DOCUMENTATION.
 * IT. IS. WRONG. AND. OUTDATED.
 * (and made by clowns I think)
 *
 * @author Tristan Deloche
 */

public class SQLHandler {

    /**
     * Variables containing authentification data later
     * Could hardcode them but making it this way allows for easier evolution later
     */
    private String username = "";
    private String password = "";
    private String table = "";

    /**
     * Variables used to prepare SQL statements
     * Initialized just because else IDE will be pissed off
     */
    private Connection connection = null;
    private Statement statement = null;

    /**
     * Handler managing SQL connections and calls in background.
     * Makes everything easier.
     *
     * I will try to explain in simple words how JDBC works. Oracle can't do it
     * in less than 97896676578996 documentation pages with retarded examples.
     *
     * So basically:
     * -- The top-level thing is the JDBC driver.
     *
     * -- The JDBC driver needs to be told with what kind of database he's dealing
     * with (PostgreSQL, Oracle SQL, MySQL, etc).
     *
     * -- When you know which one you connect to, create a connection with
     * getConnectionToDatabase() specifying the database to connect to.
     *
     * Even though the idiots at Oracle say it's not necessary anymore, it so happens
     * that Class.forName("com.mysql.jdbc.Driver").newInstance(); is necessary.
     * It will give you DriverNotFoundException else.
     *
     * Use the following format very closely : "jdbc:mysql://ip:port/database"
     * when getting the connection.
     * Of course mysql is meant to
     * be replaced with other things as you use postgre or whatever. Also
     * ip can be domain name if potentially dynamic ip.
     *
     * -- Now you have a connection. Create a Statement with it everytime you
     * want to do something. Every. Time.
     *
     * You HAVE to close old Statements in between.
     *
     * -- When you have your statement, use statement.executeQuery() or
     * statement.executeUpdate() to do things.
     *
     * query is for anythig that only reads (SELECT) and
     * update for anything that writes (UPDATE, INSERT, DELETE, DROP...)
     *
     * ------- That should be a good starting point I wish I had had back then.
     * Good luck you'll need it. -------
     *
     * @throws SQLException If SQL connection/query had issues
     * @throws ClassNotFoundException If JDBC driver didn't load. Nightmare-level exception.
     */
    public SQLHandler() throws SQLException, ClassNotFoundException {
        this.username = "";
        this.password = "";
        getConnectionToDatabase("");
    }

    /**
     * Gets the connection to a specific database. Use closeAllConnections(); then this to switch databases basically.
     * @param database Database to connect to
     * @throws SQLException If database doesn't exist or such things
     */
    private void getConnectionToDatabase(String database) throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            System.out.println("\n\nJDBC Driver not found or failed.\n\n");
            e.printStackTrace();
        }

        //DO NOT TOUCH UNLESS YOU ARE SURE OF WHAT YOU ARE DOING.
        //THIS LINE LOOKS INNOCENT BUT IS YOUR WORST NIGHTMARE EVER.
        //PROCEED WITH !EXTREME! CAUTION.
        connection = DriverManager.getConnection("jdbc:mysql://URL/" + database, username, password);
    }

    /**
     * This function has no real importance whatsoever as it should never ever be needed.
     * BUT. and there's a but. If there is a curve that doesn't finish for some reason one day,
     * this will prevent all the following curves from being wrong too.
     *
     * See class description for how interraction works and why this is needed.
     *
     * Just let it here. It doesn't hurt no one.
     * Please.
     *
     * @param security Security to clear.
     * @throws SQLException If SQL goes wrong. Should never be thrown even if there was a faulty curve.
     */
    public void cleanCurrently(String security) throws SQLException {
        //Template for the command to execute to clean the aborted curves of same-security.
        //READ CLASS DESCRIPTION BEFORE TAMPERING WITH ALL OF THIS.
        String cleanDatabaseSQLRequest = "DELETE FROM CURRENTLY_CALCULATING WHERE T0_SECURITY=";

        statement = connection.createStatement();
        switch (security) {
        case "128" :
            cleanDatabaseSQLRequest+="128";
            break;
        case "192" :
            cleanDatabaseSQLRequest+="192";
            break;
        case "256" :
            cleanDatabaseSQLRequest+="256";
            break;
        default:
            throw new IllegalArgumentException("Security wasn't in the range of expected ones (128,192,256). It was : "+security);
        }

        //Deletes aborted curves of same security.
        statement.executeUpdate(cleanDatabaseSQLRequest);

        //Here I don't use "closeAllConnections" because the same handler is reused.
        // It's an exceptionnal case though in the applet.
        statement.close();
    }

    //T-2 STEP0 -> INSERT INTO CURRENT
    /**
     * This method inserts the data from the wonderfulHashMap as a NEW entry in the database.
     * It does it in the CURRENTLY_CALCULATING table. Doesn't modify but CREATES an entry.
     * This is very important to keep in mind.
     *
     * Welcome to the awful part of comments.
     * mapToLinkedListsList, keySetListToStringWithCommas and valuesListToStringWithCommas are
     * horrific functions used to use HashMaps while keeping the advantages of list numerotation.
     * You should probably not change how they work.
     *
     * @param wonderfulHashMap Map returned from PrepareMapForStep()
     * @throws SQLException If there's any SQL error.
     */
    public void step0(HashMap<String, String> wonderfulHashMap) throws SQLException {
        table = "CURRENTLY_CALCULATING";

        //Sorry.
        //Transforms the map in a list of lists.
        //First-level is Keys.
        //Second level is objects. No arrays weren't good enough here I tried.
        //Basically a map like [[label1, key1],[label2, key2]] will become this :
        //MainList{ListOfKeys{label1, label2}, ListOfValues{key1, key2}}
        //Yes. Again it's very awkward to use but it works, and I was tired when
        //I made this
        LinkedList<LinkedList<String>> ll = mapToLinkedListsList(wonderfulHashMap);

        //Transforms the keys in the list into "key1, key2, key3, ..., keyN"
        //Pay attention to position of commas.
        String keys = keySetListToStringWithCommas(ll.get(0));

        //Transforms valus in the list into "'value1', 'value2', 'value3', ..., 'valueN'"
        //Again the ' are voluntary and are mandatory. Same for commas' placement.
        String values = valuesListToStringWithCommas(ll.get(1));

        //And now we can see why this was so cool.
        //Creates an entry for the curve with the said keys/values
        String sqlQuery = "INSERT INTO "+table+" ("+keys+") VALUES ("+values+")";

        statement = connection.createStatement();
        statement.executeUpdate(sqlQuery);

        //Always close connections !
        closeAllConnections();
    }

    //T-1/T0 STEP1 and 2 -> UPDATE CURRENT
    /**
     * This method UPDATES (doesn't create ANYTHING) the corresponding-security-level
     * curve in CURRENTLY_CALCULATING.
     * Doesn't create or delete nothing !
     *
     * The ugly foreach() is for the syntax of SQL. Like how the horrid functions of
     * step0 do, but it's simple this time around.
     *
     * @param wonderfulHashMap The hashmap from PrepareMapForStep class
     * @param step The step as 3 steps work the same way so we have to differentiate
     * @throws SQLException If anything SQL-related doesn't work.
     */
    public void step123(HashMap<String, String> wonderfulHashMap, String step) throws SQLException {
        String updatedData = "";

        //Formats data in a SQL-friendly way
        boolean firstElem = true;
        for (String theKey : wonderfulHashMap.keySet()) {
            if (firstElem) {
                updatedData+=theKey+"='"+wonderfulHashMap.get(theKey)+"'";
                firstElem = false;
            } else {
                updatedData+=","+theKey+"='"+wonderfulHashMap.get(theKey)+"'";
            }
        }

        //More SQL shenanigans
        updatedData += ",CURRENT_STEP='"+step+"'";

        //Last SQL magic operates...
        String sqlQuery = "UPDATE CURRENTLY_CALCULATING SET "+updatedData+" WHERE T0_SECURITY="+wonderfulHashMap.get("T0_SECURITY");

        statement = connection.createStatement();
        statement.executeUpdate(sqlQuery);

        //Always close connections, m'kay.
        closeAllConnections();
    }

    //T1 STEP 4 -> SELECT FROM CURRENT & INSERT INTO ARCHIVES
    /**
     * This SELECT the corresponding-security curve. Stores temporarily its data, then adds the new one given
     * and inserts the whole into archived curves. It deletes the currently calculating curve once its data
     * has been fully retrieved.
     *
     * It stores the already available data in the SELECTMap HashMap, then adds data to it and uses the full
     * SELECTMap to create the entry for the final curve entry in archived table.
     * @param wonderfulHashMap HashMap returned by PrepareMapForStep
     * @throws SQLException If anything SQL-related fails.
     */
    public void step4(HashMap<String, String> wonderfulHashMap) throws SQLException {
        //Query will basically read the already-available data.
        String sqlQuery = "SELECT * FROM CURRENTLY_CALCULATING WHERE T0_SECURITY="+wonderfulHashMap.get("T0_SECURITY");

        statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sqlQuery);

        //Used to be able to read ResultSet as a map.
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        //Map that will contain what the resultSet contained.
        HashMap<String, String> SELECTMap = new HashMap<>();

        //Parse the resultSet into SELECTMap
        while (resultSet.next()) {
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                String key = resultSetMetaData.getColumnName(i);
                String value = resultSet.getString(key);
                SELECTMap.put(key,value);
            }
        }

        //Delete the older entry since we have retrieved successfully all the data.
        sqlQuery = "DELETE FROM CURRENTLY_CALCULATING WHERE ID = '"+SELECTMap.get("ID")+"'";
        statement.executeUpdate(sqlQuery);

        //Closing all that should be. We need to keep connection here
        //so we don't use closeAllConnections();
        resultSet.close();
        statement.close();

        //We add all the new data into the SELECTMap.
        //This also erases temporary data that we added for other steps as placeholder.
        for (String aKey : wonderfulHashMap.keySet()) {
            SELECTMap.put(aKey,wonderfulHashMap.get(aKey));
        }

        //Here is why we needed to store localImage's path earlier
        String localImage = SELECTMap.get("T4_S1_PATH");
        String remoteFile = SELECTMap.get("ID")+"_"+SELECTMap.get("T0_SECURITY")+"_S1.jpg";

        //Upload the picture to server and replace local paths with remote ones before sending curve data back
        try {
            UploadHandler uploadHandler = new UploadHandler(localImage, remoteFile);
            uploadHandler.uploadBinary("T4_S1_PATH");
            SELECTMap.put("T4_S1_PATH", "curve-data/S1_IMG/"+remoteFile);
        } catch (JSchException e) {
            e.printStackTrace();
        }

        //Same for the rejected curves here
        //Create remote file path here since it was unknown before
        //Also infer current rejected_curves_K.txt file depending on K (security)
        remoteFile = SELECTMap.get("ID")+"_"+SELECTMap.get("T0_SECURITY")+"_REJECTED_CURVES.txt";
        String localRejectedCurves = "text/rejected_curves_"+SELECTMap.get("T0_SECURITY")+".txt";
        try {
            UploadHandler uploadHandler = new UploadHandler(localRejectedCurves, remoteFile);
            uploadHandler.uploadBinary("T4_PARAMETER_REJECTED_CURVES");
            SELECTMap.put("T4_PARAMETER_REJECTED_CURVES", "curve-data/REJECTED_CURVES/"+remoteFile);
        } catch (JSchException e) {
            e.printStackTrace();
        }

        //Switch to upload to the good archive table depending on security
        switch (SELECTMap.get("T0_SECURITY")) {
        case "80" : table = "SECURITY_80";
            break;
        case "128" : table = "SECURITY_128";
            break;
        case "192" : table = "SECURITY_192";
            break;
        case "256" : table = "SECURITY_256";
            break;
        default: throw new IllegalArgumentException("BAD SECURITY LEVEL :"+wonderfulHashMap.get("T0_SECURITY"));
        }

        //We remove a key that was only used to know current step of curve because it doesn't make sense
        //anymore now that the curve is finished calculating.
        SELECTMap.remove("CURRENT_STEP");

        //More ninja things with HashMaps and LinkedLists
        LinkedList<LinkedList<String>> ll = mapToLinkedListsList(SELECTMap);

        //Here we will make the ZIP file that will contain everything.
        //First we make its local location's path (RELATIVE)
        String localZipFile = SELECTMap.get("T0_SECURITY")+"_ZIPARCHIVE.zip";
        //Then its remote one (RELATIVE AGAIN)
        String remoteZipFile = SELECTMap.get("T0_SECURITY")+"_"+SELECTMap.get("ID")+"_ZIPARCHIVE.zip";

        //Create a ZIPMaker object that will be set to output into our localFile
        ZIPMaker zipMaker = new ZIPMaker(localZipFile);
        try {
            //Add the keylists and valuelists, the tweets
            //the picture and the rejected curves text.
            //See javadoc of function for more details.
            zipMaker.makeZip(ll.get(0),
                    ll.get(1),
                    localImage,
                    localRejectedCurves);

            //We create an UploadHandler to upload it and get remote location
            UploadHandler zipUploadHandler = new UploadHandler(localZipFile, remoteZipFile);
            zipUploadHandler.uploadBinary("T5_ZIP_PATH");
            //Change the old local path with the new remote one in the temporary data
            ll.get(0).add("T5_ZIP_PATH");
            //Change the old local path with the new remote one in the temporary data
            ll.get(1).add("curve-data/ARCHIVE_ZIP/"+remoteZipFile);
        } catch (IOException e) {
            System.out.println("Couldn't make zip file !");
            e.printStackTrace();
        } catch (JSchException e) {
            System.out.println("Couldn't upload the zip file !");
            e.printStackTrace();
        }

        //Prepare the final query.
        //This is a fairly long one.
        sqlQuery = "";
        //SQL-friendly strings as always
        String keys = keySetListToStringWithCommas(ll.get(0));
        String values = valuesListToStringWithCommas(ll.get(1));

        //Magic if you think about it for a second.
        sqlQuery += "INSERT INTO "+table+" ("+keys+") VALUES ("+values+")";

        statement = connection.createStatement();
        statement.executeUpdate(sqlQuery);

        //Always close connections etc
        closeAllConnections();
    }

    /**
     * Creates and publish the tweets file on the server
     * @param T0_SECURITY Curve security, to know which curve to update
     * @param hashtag Hashtag to publish as a link and name of file
     * @throws SQLException
     * @throws JSchException
     */
    public void addTweetsFile(String T0_SECURITY, String hashtag) throws SQLException, JSchException {

        String remoteLocation;
        UploadHandler uploadHandler = new UploadHandler("text/tweets_"+T0_SECURITY+".txt", hashtag+"_tweets.txt");
        remoteLocation = uploadHandler.uploadBinary("T2_TWEETS")+hashtag+"_tweets.txt";

        String sqlQuery = "UPDATE CURRENTLY_CALCULATING SET T2_TWEETS='"+remoteLocation+"' WHERE T1_HASHTAG='trx_curve_"+hashtag+"'";
        statement = connection.createStatement();
        statement.executeUpdate(sqlQuery);
    }

    /**
     * Parses a list of Strings into a keys list for SQL usage
     * @param muhKeySetList List of keys to use
     * @return "aKey, anotherKey, anotherKey, ..., anotherKey" with
     *          this specific comma positionning.
     */
    private String keySetListToStringWithCommas(LinkedList<String> muhKeySetList) {
        boolean first = true;
        String joinedString = "";
        for (String aKey : muhKeySetList) {
            if (first) {
                joinedString+=aKey;
                first = false;
            } else {
                joinedString+=","+aKey;
            }
        }

        return joinedString;
    }

    /**
     * Parses a list of Strings into a values list for SQL usage
     * @param muhValueList List of values to use
     * @return "'aValue', 'anotherValue', 'anotherValue', ..., 'anotherValue'" with
     *          this specific comma and ' positionning.
     */
    private String valuesListToStringWithCommas(LinkedList<String> muhValueList) {
        boolean first = true;
        String joinedString = "";
        for (String aKey : muhValueList) {
            if (first) {
                joinedString+="'"+aKey+"'";
                first = false;
            } else {
                joinedString+=",'"+aKey+"'";
            }
        }

        return joinedString;
    }

    /**
     * Transforms a non-ordered HashMap into an orderd list of 2 lists.
     * One of the lists is the keyset.
     * The other is the valueSet BUT corresponding ordinally
     * to the keyset and its values.
     *
     * It is cool and is an essential brick of the whole project.
     * Believe it or not.
     *
     * Makes a hashmap like :
     * key1 - value1
     * key2 - value2
     * key3 - value3
     *
     * into two lists like this :
     * {key1, key2, key3}
     * {value1, value2, value3}
     *
     * @param wonderfulHashmap A raw hashmap not fit for use of ordered items
     * @return A wonderful list of lists fit for use of ordered items
     */
    private LinkedList<LinkedList<String>> mapToLinkedListsList(HashMap<String, String> wonderfulHashmap) {
        LinkedList<LinkedList<String>> linkedLists = new LinkedList<>();

        LinkedList<String> keySetList = new LinkedList<>(wonderfulHashmap.keySet());
        LinkedList<String> valueList = new LinkedList<>();

        for (String aKey : keySetList) {
            valueList.add(wonderfulHashmap.get(aKey));
        }

        linkedLists.add(keySetList);
        linkedLists.add(valueList);

        return linkedLists;
    }

    //Closes all connection. Useless but nice to not put even more code where it's already
    //pretty much hard to read.
    private void closeAllConnections() {
        try {
            statement.close();
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
