package ch.epfl.trx.Sender;

import ch.epfl.trx.Fetcher.TwitterHandler;
import hirondelle.date4j.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TimeZone;

/**
 * Class containing parsing methods for upload steps
 * Meant to be used statically (like an utility class mostly)
 *
 * Does a lot of things, more repetitive than complicated.
 *
 * @author Tristan Deloche
 */
final public class PrepareMapForStep {

    /***
     * Do not use this constructor. Static class not meant to be instantiated.
     */
    private PrepareMapForStep() {}

    //Class-level variable to be used by multiple functions instead of passing arguments everywhere.
    public static HashMap<String, String> dataMap = new HashMap<>();

    /**
     * Formats the data from the file into a HashMap that is easy to use with SQL requests.
     *
     * @param stepNumber Current step to prepare
     * @param dataFilePath Path to the text file describing the step's data to format
     * @return The formatted HashMap. Called wonderfulHashMap in this project.
     *
     * @throws IOException If the text file can't be accessed for some reason (not here, no permissions etc)
     */
    public static HashMap<String, String> exportMap(int stepNumber, String dataFilePath) throws IOException {
        /**
         * Read data file and parse each line into a String list
         */

        //Makes a Path for the File object out of a String version of it.
        //Maybe it's unnecessary but it can't be bad and also ensures that any system will be compatible
        //since different OS have different path styles
        //Base directory is the JAR file's one.
        Path path = Paths.get(dataFilePath);

        //Read using UTF-8. Very important because it will accept accentuated characters etc.
        //Also because only Windows is limited to Unicode in standard. Thank you based fucking retarded american idiots.
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
            String line;

            //List that will contain each line one by one. Starts at /!1!/
            //the 0 index is used for bedugging purposes do not try to access it it will often give
            //and ArrayIndexOutOfBounds exception or useless data.
            LinkedList<String> dataList = new LinkedList<>();
            dataList.add(0,"ERROR ACCESS FORBIDDEN");

            //Add lines to list as strings, one by one.
            while ((line = reader.readLine()) != null) {
                dataList.addLast(line);
            }
            //Closes the reader. VERY important to prevent huge memory leaks.
            //If not here, you can be using 30+ GB of memory in only a few calls to the function. (resulting in system crashing usually)
            reader.close();

            /**
             * Entering conditional behavior
             */
            if (stepNumber == 0) {
                //Useless but debugging is important.
                System.out.println("STEP0\n");

                //Step 0 is T-2 in this program, for simplicity purposes. Write it down somewhere cause it can fastly get confusing.

                //Add lines following pre-arranged order made with the person in charge of the math side of the project
                dataMap.put("T0_SECURITY", dataList.get(1)); //K
                dataMap.put("T0_A", dataList.get(2)); //A
                dataMap.put("T0_PRIME_TYPE", dataList.get(3)); //P
                dataMap.put("T0_COFACTOR", dataList.get(4)); //M (montgomery strategy, not an actual cofactor)

                //Adds the "beginning calculation" time with this. There is a few microseconds lost between when the math side
                //created the curve but since we only need a second-level precision it's MUCH better to calculate it here.
                //Also directly formatting it correctly for website/databases
                //See hirondelle-date4j website for informations on how this works. Java 8 has a cool new date API but I wrote this
                //before Java 8 was out so didn't have it yet.
                DateTime T1_TIMESTAMP = DateTime.now(TimeZone.getDefault()).plus(0,0,0,0,5,0,0, DateTime.DayOverflow.FirstDay);
                dataMap.put("T1_TIMESTAMP", T1_TIMESTAMP.format("YYYY-M-D hh:mm:ss")); //T-1


                //Calculating and adding 10 min to T-1 to get T0
                //Always be careful of the DayOverflow (28->29 days for leap years etc)
                DateTime T2_TIMESTAMP = T1_TIMESTAMP.plus(0,0,0,0,10,0,0, DateTime.DayOverflow.FirstDay);
                dataMap.put("T2_TIMESTAMP", T2_TIMESTAMP.format("YYYY-M-D hh:mm:ss"));

                return dataMap;

            } else if (stepNumber == 1) {
                System.out.println("STEP1\n");

                //T-1 (Step 1 = T-1)

                //Add lines following pre-arranged order made with the person in charge of the math side of the project
                dataMap.put("T0_SECURITY", dataList.get(1));
                dataMap.put("T1_HASHTAG", "trx_curve_" + dataList.get(2));

                //ADD PICTURE LOCAL PATH
                //Seems useless here since we should update with the remote picture location but look at SQL handler for explanation on this.
                //basically we will need the local path there and re-reading the file was a loss of memory/processing time for no reason
                dataMap.put("T4_S1_PATH", "pics/img_"+dataList.get(2)+".jpg");

                //Publish the tweet with the said hashtag right away. Doesn't do anything else for this step.
                new TwitterHandler().startEntropyStatus(dataList.get(1), dataList.get(2));
                return dataMap;

            } else if (stepNumber == 2) {
                System.out.println("STEP2\n");
                // T0

                //Add lines following pre-arranged order made with the person in charge of the math side of the project
                dataMap.put("T0_SECURITY", dataList.get(1));
                dataMap.put("T2_S0_TWEETS", dataList.get(2));
                dataMap.put("T2_COMMITMENT", dataList.get(3));

                return dataMap;

            } else if (stepNumber == 3) {
                System.out.println("STEP3\n");
                // T0.5

                //Add lines following pre-arranged order made with the person in charge of the math side of the project

                dataMap.put("T0_SECURITY", dataList.get(1));
                dataMap.put("T3_SLOTH_HASH", dataList.get(2));
                dataMap.put("T3_WITNESS", dataList.get(3));

                return dataMap;

            } else if (stepNumber == 4) {
                System.out.println("STEP4\n");
                // T1

                //Add lines following pre-arranged order made with the person in charge of the math side of the project
                //This one is pretty much rich in data. Tried to describe each variable.

                dataMap.put("T0_SECURITY", dataList.get(1));

                //Specific curve parameters, see descrption on the side
                dataMap.put("T4_PARAMETER_Q", dataList.get(2)); //ORDER
                dataMap.put("T4_PARAMETER_P", dataList.get(3)); //PRIME
                dataMap.put("T4_PARAMETER_A", dataList.get(4)); //A
                dataMap.put("T4_PARAMETER_B", dataList.get(5)); //B
                dataMap.put("T4_PARAMETER_INDEX", dataList.get(6)); //INDEX
                dataMap.put("T4_PARAMETER_COFACTOR", dataList.get(7)); //COFACTOR
                dataMap.put("T4_PARAMETER_DISCRIMINANT", dataList.get(8)); //DISCRIMINANT
                dataMap.put("T4_PARAMETER_TRACE", dataList.get(9)); //TRACE
                dataMap.put("T4_PARAMETER_RUNTIME", formatRuntime(dataList.get(10))); // RUNTIME
                dataMap.put("T4_PARAMETER_COORD_X", dataList.get(11)); // X
                dataMap.put("T4_PARAMETER_COORD_Y", dataList.get(12)); // Y
                dataMap.put("T4_PARAMETER_DEGREE", dataList.get(13)); //DEGREE
                dataMap.put("T4_PARAMETER_REJECTED_CURVES", "text/rejected_curves_"+dataList.get(1)+".txt");


                return dataMap;

            } else {
                throw new IllegalArgumentException("Bad step number.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Couldn't access file !");
        }
    }

    /**
     * Basically the runtim is in miliseconds which is pretty much WAY too detailed. So we conver it in a more usable scale : seconds.
     * @param originalString Original time, in string type.
     * @return Formatted running time.
     */
    private static String formatRuntime(String originalString) {
        //Runtime not being just "XXXX" but "XXXX ms" we only care about the left side of the blank space, thus the split here.
        String[] stringArray = originalString.split(" ");

        //Miliseconds -> Seconds
        int runtime = Integer.valueOf(stringArray[3]);
        runtime/=1000;

        //The string returned will be displayed as-is on the website so this is why it's directly formatted here instead of
        //just returning the scaled number. (Could have done that in other function but it's clunky enough back there)
        return "Calculation duration: "+runtime+" seconds";
    }
}
