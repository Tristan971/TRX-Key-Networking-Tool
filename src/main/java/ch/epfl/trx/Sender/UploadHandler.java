package ch.epfl.trx.Sender;

import com.jcraft.jsch.*;

/**
 * Class managing upload of binary files to server using a Secure FTP connection (SFTP) with JSCH's library
 * See jcraft's JSCH library's documentation for syntax description
 *
 * @author Tristan Deloche
 */
public class UploadHandler {

    String localFilePath;
    String remoteFileName;

    /**
     * Constructor which only initializes String variables.
     * @param localFilePath Local file's PATH
     * @param remoteFileName Remote file's upcoming FILE NAME.
     */
    public UploadHandler(String localFilePath, String remoteFileName) {
        this.localFilePath = localFilePath;

        //Becomes the remote file's /PATH/ later on, just because it's clear enough like that and avoids useless temp variables
        this.remoteFileName = remoteFileName;
    }

    /**
     * Uploads the file and returns its remote path.
     * @param fileType The type of file we are going to upload, for location-awareness
     * @return The file's remote !!! RELATIVE !!! path from the /var/www/public_html/ directory.
     *          It is NOT the absolute path (for obvious security reasons)
     * @throws JSchException If file couldn't be uploaded for a reason or another, probably related to SFTP errors.
     */
    public String uploadBinary(String fileType) throws JSchException {

        //Returned string. Relative location. See Javadoc of the method.
        String remoteLocation;

        //Ugliest switch in the history of humanity, but it's better to do it that way in the end
        //What it does is pretty self-explanatory.
        switch (fileType) {
        case "T4_S1_PATH":
            //Absolute path for SFTP
            remoteFileName = "/var/www/html/curve-data/S1_IMG/" + remoteFileName;
            //Relative path, for Apache and public
            remoteLocation = "curve-data/S1_IMG/" + remoteFileName;
            break;
        case "T4_PARAMETER_REJECTED_CURVES":
            //Absolute path for SFTP
            remoteFileName = "/var/www/html/curve-data/REJECTED_CURVES/" + remoteFileName;
            //Relative path, for Apache and public
            remoteLocation = "curve-data/REJECTED_CURVES/" + remoteFileName;
            break;
        case "T5_ZIP_PATH":
            //Absolute path for SFTP
            remoteFileName = "/var/www/html/curve-data/ARCHIVE_ZIP/" + remoteFileName;
            //Relative path, for Apache and public
            remoteLocation = "curve-data/ARCHIVE_ZIP/" + remoteFileName;
            break;
        case "T2_TWEETS":
            //Absolute path for SFTP
            remoteFileName = "/var/www/html/curve-data/TWEETS_FILES/" + remoteFileName;
            //Relative path, for Apache and public
            remoteLocation = "curve-data/TWEETS_FILES/";
            break;
        default:
            throw new IllegalArgumentException("BAD FILETYPE : " + fileType);
        }

        JSch jsch = new JSch();
        Session session;
        try {
            //Connects to the cerver with credentials
            //The "StrictHostKeyChecking" parameters makes the server's fingerprint automatically trusted in case
            //it's reinstalled/updated by EPFL.
            session = jsch.getSession("", "", 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword("");
            session.connect();

            //Starts the data exchange, uploads the file and closes the channel.
            //If not close it could lead in unaccessible server for a few minutes potentially making some steps buggy
            //Also always clean Streams anyway.
            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.put(localFilePath, remoteFileName);
            sftpChannel.exit();
            session.disconnect();
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }

        //Returns the location that will be used on the website to load the files. This one is public, meant to be public and safe to be seen by
        //the public in contrary to remoteFileName.
        return remoteLocation;
    }
}
