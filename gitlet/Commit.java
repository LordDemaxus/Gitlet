package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  @author Medhaav Chandra Mahesh
 *
 * @Source format date: https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
 */
public class Commit implements Serializable {

    /**
     * The folder where commits are stored
     */
    static final File COMMIT_FOLDER = join(Repository.GITLET_DIR, "commits");

    /**
     * The message of this Commit.
     */
    private String message;
    /**
     * the hash id of the commit
     */
    private String id;
    /**
     * the date the commit was the made
     */
    private String date;
    /**
     * The author of the commit - always PC
     */
    private String author;
    /**
     * the specific log of just this commit
     */
    private String log;
    /**
     * Hashmap containining all the blob files stored in the commit
     */
    private HashMap<String, String> blobs;
    /**
     * ArrayList containing parents of the commit. Contains two parents if it is a merge commit
     */
    private ArrayList<String> parents = new ArrayList<>(2);

    public Commit(String s, String parent, HashMap<String, String> blobs) {
        this.message = s;
        parents.add(parent);
        Date tempDate;
        if (s.equals("initial commit")) {
            tempDate = new Date(0);
        } else {
            tempDate = new Date();

        }
        this.date = String.format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz", tempDate);
        this.author = "PC";
        this.blobs = blobs;
        this.id = sha1(serialize(this));
        this.log = String.format("===\ncommit %s\nDate: %s\n%s\n", id, date, message);
    }
    public Commit(String s, String parent1, String parent2, HashMap<String, String> blobs) {
        this(s, parent1, blobs);
        parents.add(parent2);
        this.log = String.format("===\ncommit %s\nMerge: %s %s\nDate: %s\n%s\n",
                id, parent1.substring(0, 7), parent2.substring(0, 7), date, message);
    }


    public void saveCommit() {
        File commitFile = join(COMMIT_FOLDER, id);
        writeObject(commitFile, this);
    }



    public static Commit getCommit(String s) {

        File commit = join(COMMIT_FOLDER, s);
        if (commit.exists()) {
            Commit c = readObject(commit, Commit.class);
            return c;
        } else {
            System.out.print("No commit with that id exists.");
            System.exit(0);
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public String getLog() {
        return log;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<String> getParents() {
        return parents;
    }

    public String getParents(int i) {
        if (i == 0) {
            return parents.get(0);
        } else if (i == 1) {
            return parents.get(1);
        }
        return null;
    }


    public HashMap<String, String> getBlobs() {
        return blobs;
    }

    public void logs() {
        if (parents.get(0) == null) {
            System.out.print(getLog());
        } else {
            Commit parentCommit = getCommit(parents.get(0));
            System.out.println(getLog());
            parentCommit.logs();
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Commit commit = (Commit) o;
        return id.equals(commit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
