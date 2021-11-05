package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static gitlet.Utils.*;
import static gitlet.Repository.*;


/**
 * Contains core functions and branch related functions
 * Initialize head everytime gitlet is used. Also creates a branch
 * , gets head of any branch or change the current branch.
 *
 * @author Medhaav Chandra Mahesh
 *
 */
public class Core implements Serializable {

    static File headFile = join(GITLET_DIR, "HEAD");


    public static void saveHead() {
        if (!headFile.exists()) {
            try {
                headFile.createNewFile();
            } catch (IOException io) {
                System.out.println("io error");
            }
            changeHead("master");
        }
        File branchFile = join(BRANCHES_FOLDER, getActiveBranch());
        writeObject(branchFile, head);
    }

    public static void createBranch(String branchName) {
        File branchFile = join(BRANCHES_FOLDER, branchName);
        if (!branchFile.exists()) {
            try {
                branchFile.createNewFile();
            } catch (IOException io) {
                System.out.println("io error");
            }
            writeObject(branchFile, head);
        } else {
            System.out.println("A branch with that name already exists.");
        }

    }

    public static void changeHead(String branchName) {
        head = getHead(branchName);
        writeObject(headFile, branchName);
    }

    public static String getHead(String branchName) {
        return getHeadRemote(branchName, BRANCHES_FOLDER);
    }

    public static String getHeadRemote(String branchName, File gitDir) {
        File branchHead = join(gitDir, branchName);
        if (branchHead.exists()) {
            return readObject(branchHead, String.class);
        } else {
            if (gitDir.equals(BRANCHES_FOLDER)) {
                System.out.println("No such branch exists.");
            } else {
                System.out.println("That remote does not have that branch.");
            }
            System.exit(0);
        }
        return null;
    }


    public static boolean getCore() {
        if (!headFile.exists()) {
            return false;
        }
        File branchHead = join(BRANCHES_FOLDER, getActiveBranch());
        head = readObject(branchHead, String.class);
        return true;
    }

    public static String getActiveBranch() {
        return readObject(headFile, String.class);
    }


}
