package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Stack;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/**
 * Does remote functions
 * Adds and removes remotes, and fetch, pull, and push
 *
 * @author Medhaav Chandra Mahesh
 */

public class Remote implements Serializable {
    static final File REMOTE_FOLDER = join(GITLET_DIR, "remotes");

    public static void addRemote(String remoteName, String remote) {
        File newRemote = join(REMOTE_FOLDER, remoteName);
        if (!newRemote.exists()) {
            String remoteDirectory = new File(remote).getParent();
            writeObject(newRemote, remoteDirectory);
        } else {
            System.out.println("A remote with that name already exists.");
        }
    }

    public static void removeRemote(String remoteName) {
        File oldRemote = join(REMOTE_FOLDER, remoteName);
        if (oldRemote.exists()) {
            oldRemote.delete();
        } else {
            System.out.println("A remote with that name does not exist.");
        }
    }

    public static String getRemoteDir(String remoteName) {
        File remote = join(REMOTE_FOLDER, remoteName);
        if (remote.exists()) {
            String dir = readObject(remote, String.class);
            if (!new File(dir + File.separator + ".gitlet").exists()) {
                System.out.println("Remote directory not found.");
                System.exit(0);
                return null;
            }
            return dir;
        } else {
            System.out.println("Remote directory not found.");
            System.exit(0);
            return null;
        }
    }

    public static void push(String remoteName, String branchName) {
        File gitDir = new File(getRemoteDir(remoteName) + File.separator + ".gitlet");
        String branchHead = Core.getHeadRemote(branchName, join(gitDir, "branches"));
        Commit c = Commit.getCommit(head);
        Stack<String> commits = new Stack<>();
        Boolean inHistory = false;
        while (c.getParents(0) != null) {
            commits.push(c.getId());
            if (c.getParents(0).equals(branchHead)) {
                inHistory = true;
                break;
            }
            c = Commit.getCommit(c.getParents().get(0));
        }
        if (inHistory) {
            for (String file : commits) {
                File fromFile = join(Commit.COMMIT_FOLDER, file);
                File toFile = join(join(gitDir, "commits"), file);
                if (!toFile.exists()) {
                    Commit contents = readObject(fromFile, Commit.class);
                    writeObject(toFile, contents);
                }
            }
            File branchFile = join(join(gitDir, "branches"), branchName);
            writeObject(branchFile, head);
        } else {
            System.out.println("Please pull down remote changes before pushing.");
        }
    }

    public static String fetch(String remoteName, String branchName) {
        File gitDir = new File(getRemoteDir(remoteName) + File.separator + ".gitlet");
        String branchHead = Core.getHeadRemote(branchName, join(gitDir, "branches"));
        Commit remoteHead = getRemoteCommit(branchHead, gitDir);
        String newName = remoteName + "%" + branchName;
        File branchFile = join(BRANCHES_FOLDER, newName);
        writeObject(branchFile, branchHead);
        while (remoteHead.getParents(0) != null) {
            File fromFile = join(join(gitDir, "commits"), remoteHead.getId());
            File toFile = join(Commit.COMMIT_FOLDER, remoteHead.getId());
            if (!toFile.exists()) {
                Commit contents = readObject(fromFile, Commit.class);
                writeObject(toFile, contents);
                addBlobs(remoteHead, gitDir);
            }
            remoteHead = getRemoteCommit(remoteHead.getParents(0), gitDir);
        }
        return newName;
    }

    public static Commit getRemoteCommit(String commmitName, File gitDir) {
        File commit = join(join(gitDir, "commits"), commmitName);
        if (commit.exists()) {
            Commit c = readObject(commit, Commit.class);
            return c;
        } else {
            System.out.print("No commit with that id exists.");
            System.exit(0);
        }
        return null;
    }

    public static void addBlobs(Commit commit, File gitDir) {
        HashMap<String, String> commitBlobs = commit.getBlobs();
        for (HashMap.Entry<String, String> pair : commitBlobs.entrySet()) {
            String hash = pair.getValue();
            File fromFile = join(join(gitDir, "blobs"), hash);
            File toFile = join(Blobs.BLOBS_FOLDER, hash);
            if (!toFile.exists()) {
                byte[] contents = readContents(fromFile);
                writeObject(toFile, contents);
            }
        }
    }
}
