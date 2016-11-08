package ch.epfl.trx.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Class managing the Zipping protocol
 * Look at JAVA's Zip package's documentation for the syntax.
 * Protip : Oracle is a shitty company when it comes to documentation
 * so you better look for nice explanations online. (ie : StackOverflow)
 *
 * @author Tristan Deloche
 */

public class ZIPMaker {

    private String outputLocation = "";
    private String dataString = "";

    /**
     * Constructor only needs to know the path for the new FileStream it will use.
     * @param outputLocation Local location to output the zip to.
     */
    public ZIPMaker(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    /**
     * Argument fiesta.
     * This makes a zip with all of this.
     * So basically it will make a map-like out with the two lists. Then add picture. Then rejected curves.
     * Then tweets. Just doesn't need tweets here as we know how to find them.
     * @param keysList List of keys from mapToLinkedListsList()
     * @param valuesList List of values from mapToLinkedListsList()
     * @param T4_S1_PATH Path of the image (or seed for the math name)
     * @param T4_PARAMETER_REJECTED_CURVES Path of the rejected curves text file
     * @throws IOException If it can't access those files, can't write the zip etc.
     */
    public void makeZip(LinkedList<String> keysList, LinkedList<String> valuesList, String T4_S1_PATH, String T4_PARAMETER_REJECTED_CURVES) throws IOException {

        String key, value = null;

        //Create a string version of the data
        for (String aKey: keysList) {
            key = aKey;

            try {
                value = valuesList.get(keysList.indexOf(aKey));
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Faulty one is :"+aKey);
                e.printStackTrace();
            }

            dataString+=key+"="+value+"\n";
        }


        //Basically looks awful but just awkward syntax.
        File zipfile = new File(outputLocation);

        //Buffer to speed it up as there's a relatively decent amount
        //of data to write here and it would be slow without it.
        byte[] buffer = new byte[1024];
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));

            //Create a TXT containing the data from the lists at line 48~60
            out.putNextEntry(new ZipEntry("raw_data.txt"));
            out.write(dataString.getBytes(), 0, dataString.getBytes().length);
            out.closeEntry();

            //Add files to write to a list
            LinkedList<File> filesList = new LinkedList<>();
            filesList.add(new File(T4_S1_PATH));
            filesList.add(new File(T4_PARAMETER_REJECTED_CURVES));
            filesList.add(new File("text/tweets_"+valuesList.get(keysList.indexOf("T0_SECURITY"))+".txt"));

            //Write 1 by 1 and then close fileOutput we open for each.
            for (File file : filesList) {
                FileInputStream currentFileToWrite = new FileInputStream(file.getCanonicalPath());
                out.putNextEntry(new ZipEntry(file.getName()));
                int len;
                while ((len = currentFileToWrite.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.closeEntry();
                currentFileToWrite.close();
            }
            //When all have been written, close general output
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
