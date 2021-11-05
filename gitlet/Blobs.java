package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;


import static gitlet.Utils.*;

/**
 * Does blob related functions
 * save and read Blobs
 *
 * @author Medhaav Chandra Mahesh
 *
 */

public class Blobs implements Serializable {
    static final File BLOBS_FOLDER = join(Repository.GITLET_DIR, "blobs");

    //* creates a hash of a file based on its contents and file name/
    public static String hashBlob(File f) {
        HashSet<String> b = new HashSet<>();
        b.add(readContentsAsString(f));
        b.add(f.getName());
        return sha1(serialize(b));
    }

    //* saves the file as a blob/
    public static String saveBlob(File f) {
        String hash = hashBlob(f);
        File blobFile = join(BLOBS_FOLDER, hash);
        writeContents(blobFile, readContents(f));
        return hash;
    }

    //* reads the blob with that has the hash as its file name/
    public static byte[] readBlob(String fileHash) {

        File foundFile = join(BLOBS_FOLDER, fileHash);
        if (foundFile.exists()) {
            byte[] convertFile = readContents(foundFile);
            return convertFile;
        } else {
            System.out.print("No file with that id exists.");
        }
        return null;
    }


}
