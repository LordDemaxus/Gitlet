package gitlet;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

import static gitlet.Core.*;
import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * Does initialize, stage a file, make a commit, checkout files and branches
 * get the logs and status, find, reset, remove, create branches, and merge
 *
 * @author Medhaav Chandra Mahesh
 *
 * @Source Lab 6
 * Iterate through hashmap: https://stackoverflow.com/a/6797672,
 * Iterate through Folder: https://edstem.org/us/courses/3735/discussion/268672?comment=704320
 * Copy file to different folder:
 *                               https://edstem.org/us/courses/3735/discussion/268672?comment=703962
 * Change a character in a string: https://stackoverflow.com/a/1234518
 */
public class Repository {

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    static final File GITLET_DIR = join(CWD, ".gitlet");
    /**
     * The staging area
     */
    static final File STAGING_FOLDER = join(GITLET_DIR, "staging");
    /**
     * The staging area for removals
     */
    static final File REMOVAL_FOLDER = join(GITLET_DIR, "remove");
    /**
     * Folder where head pointer of each branch is stored
     */
    static final File BRANCHES_FOLDER = join(GITLET_DIR, "branches");
    /**
     * The initial commit
     */
    static Commit initialCommit;
    /**
     * The head of the current active branch
     */
    static String head;


    public static void initialize() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            STAGING_FOLDER.mkdir();
            REMOVAL_FOLDER.mkdir();
            BRANCHES_FOLDER.mkdir();
            Blobs.BLOBS_FOLDER.mkdir();
            Remote.REMOTE_FOLDER.mkdir();
            initialCommit = new Commit("initial commit", null, new HashMap<>());
            Commit.COMMIT_FOLDER.mkdir();
            initialCommit.saveCommit();
            head = initialCommit.getId();
            createBranch("master");
            saveHead();
        } else {
            System.out.println("Gitlet version-control system already exists in the current "
                    + "directory");
        }
    }

    public static void branch(String s) {
        createBranch(s);
    }

    public static void add(String s) {
        File fileFinder = join(CWD, s);
        if (fileFinder.exists()) {
            File stagingFile = join(STAGING_FOLDER, s);
            Commit c = Commit.getCommit(head);
            String hashblob = Blobs.hashBlob(fileFinder);
            if (!c.getBlobs().containsValue(hashblob)) {
                try {
                    Files.copy(fileFinder.toPath(), stagingFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException io) {
                    System.out.println("io error");
                }
            } else {
                if (stagingFile.exists()) {
                    stagingFile.delete();
                }
            }
            File removalFile = join(REMOVAL_FOLDER, s);
            if (removalFile.exists()) {
                removalFile.delete();
            }
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    public static void makeCommit(String s, int parents, String branchId) {
        Commit c = Commit.getCommit(head);
        HashMap<String, String> stagedBlobs = new HashMap<>();
        List<String> blobs = plainFilenamesIn(STAGING_FOLDER);
        List<String> exclude = plainFilenamesIn(REMOVAL_FOLDER);
        if (blobs.size() > 0 || exclude.size() > 0) {
            for (String blob : blobs) {
                File stagedFile = join(STAGING_FOLDER, blob);
                String savedBlob = Blobs.saveBlob(stagedFile);
                stagedBlobs.put(blob, savedBlob);
                stagedFile.delete();
            }

            for (HashMap.Entry<String, String> pair : c.getBlobs().entrySet()) {
                String k = pair.getKey();
                String v = pair.getValue();
                if (!stagedBlobs.containsKey(k) && !exclude.contains(k)) {
                    stagedBlobs.put(k, v);
                } else if (exclude.contains(k)) {
                    File removalFile = join(REMOVAL_FOLDER, k);
                    removalFile.delete();
                }
            }
            Commit newCommit = null;
            if (parents == 1) {
                newCommit = new Commit(s, head, stagedBlobs);
            } else {
                newCommit = new Commit(s, head, branchId, stagedBlobs);
            }
            newCommit.saveCommit();
            head = newCommit.getId();
            saveHead();
        } else {
            System.out.println("No changes added to the commit.");
        }
    }

    public static void getLog(String s) {
        if (s.equals("global-log")) {
            List<String> commits = plainFilenamesIn(Commit.COMMIT_FOLDER);
            for (String commitHash : commits) {
                Commit commit = Commit.getCommit(commitHash);
                System.out.println(commit.getLog());
            }
        } else if (s.equals("log")) {
            Commit commitHead = Commit.getCommit(head);
            commitHead.logs();
        }
    }

    public static void doCheckout(String commitId, String fileName) {
        Commit c = null;
        if (commitId.length() == 40) {
            c = Commit.getCommit(commitId);
        } else {
            c = shortcommit(commitId);
        }
        if (c != null && c.getBlobs().containsKey(fileName)) {
            String fileHash = c.getBlobs().get(fileName);
            byte[] fileFinder = Blobs.readBlob(fileHash);
            File replaceFile = join(CWD, fileName);
            writeContents(replaceFile, fileFinder);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    private static Commit shortcommit(String commitId) {
        List<String> files = plainFilenamesIn(Commit.COMMIT_FOLDER);
        int i = 0;
        String commit = null;
        for (String file : files) {
            if (file.contains(commitId)) {
                return Commit.getCommit(file);
            }
        }
        return null;
    }

    public static void doBranchCheckout(String branchName) {
        branchName = branchName.replace("/", "%");
        if (!getActiveBranch().equals(branchName)) {
            String previousBranch = getActiveBranch();
            String previousHead = head;
            changeHead(branchName);
            changeCWD(previousHead);
            changeHead(previousBranch);
        } else {
            System.out.println("No need to checkout the current branch.");
        }
    }

    private static void changeCWD(String previousHead) {
        HashMap<String, String> previousHeadBlobs = Commit.getCommit(previousHead).getBlobs();
        HashMap<String, String> currentHeadBlobs = Commit.getCommit(head).getBlobs();
        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String file : currentFiles) {
            if (!previousHeadBlobs.containsKey(file) && currentHeadBlobs.containsKey(file)) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, or add and commit it first");
                return;
            }
        }
        for (HashMap.Entry<String, String> pair : currentHeadBlobs.entrySet()) {
            String k = pair.getKey();
            doCheckout(head, k);
        }
        for (HashMap.Entry<String, String> pair : previousHeadBlobs.entrySet()) {
            String k = pair.getKey();
            if (!currentHeadBlobs.containsKey(k)) {
                File deleteFile = join(CWD, k);
                deleteFile.delete();
            }
        }
        clearStaging();
        System.exit(0);
    }

    private static void clearStaging() {
        for (File file : STAGING_FOLDER.listFiles()) {
            file.delete();
        }
        for (File file : REMOVAL_FOLDER.listFiles()) {
            file.delete();
        }

    }

    public static void remove(String fileName) {
        File stageFile = join(STAGING_FOLDER, fileName);
        Commit c = Commit.getCommit(head);
        if (stageFile.exists() || c.getBlobs().containsKey(fileName)) {
            stageFile.delete();
            removeFromHead(c, fileName);
        } else {
            System.out.print("No reason to remove the file.");
        }
    }

    //* stages a deleted file for removal and removes
    // it from working directory if it hasn't been deleted/
    private static void removeFromHead(Commit c, String fileName) {
        File file = join(CWD, fileName);
        if (c.getBlobs().containsKey(fileName)) {
            File removalFile = join(REMOVAL_FOLDER, fileName);
            if (file.exists()) {
                try {
                    Files.copy(file.toPath(), removalFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException io) {
                    System.out.println("io error");
                }
                file.delete();
            } else {
                try {
                    removalFile.createNewFile();
                } catch (IOException io) {
                    System.out.println("io error");
                }
            }
        }
    }

    public static void removeBranch(String branchName) {
        File branch = join(BRANCHES_FOLDER, branchName);
        if (branch.exists()) {
            if (!branch.equals(getActiveBranch())) {
                branch.delete();
            } else {
                System.out.println("Cannot remove the current branch.");
            }
        } else {
            System.out.println("A branch with that name does not exist.");
        }


    }

    public static void find(String message) {
        List<String> commits = plainFilenamesIn(Commit.COMMIT_FOLDER);
        int i = 0;
        for (String commitHash : commits) {
            Commit commit = Commit.getCommit(commitHash);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getId());
                i += 1;
            }
        }
        if (i == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void reset(String commitID) {
        Commit.getCommit(commitID);
        String previousHead = head;
        head = commitID;
        saveHead();
        changeCWD(previousHead);
        head = previousHead;
        saveHead();
    }

    public static void status() {
        branchStatus();
        System.out.println("");
        stagedStatus();
        System.out.println("");
        removedStatus();
        System.out.println("");
        modifiedStatus();
        System.out.println("");
        untrackedStatus();
    }

    private static void branchStatus() {
        System.out.println("=== Branches ===");
        List<String> files = plainFilenamesIn(BRANCHES_FOLDER);
        for (String file : files) {
            if (getActiveBranch().equals(file)) {
                System.out.println("*" + file.replace("%", "/"));
            } else {
                System.out.println(file.replace("%", "/"));
            }
        }
    }

    private static void stagedStatus() {
        System.out.println("=== Staged Files ===");
        List<String> files = plainFilenamesIn(STAGING_FOLDER);
        for (String file : files) {
            System.out.println(file);
        }
    }

    private static void removedStatus() {
        System.out.println("=== Removed Files ===");
        List<String> files = plainFilenamesIn(REMOVAL_FOLDER);
        for (String file : files) {
            System.out.println(file);
        }
    }


    private static boolean mergeCheck(Commit current, String branchName) {
        File branch = join(BRANCHES_FOLDER, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        Commit other = Commit.getCommit(Core.getHead(branchName));
        if (plainFilenamesIn(STAGING_FOLDER).size() > 0
                || plainFilenamesIn(REMOVAL_FOLDER).size() > 0) {
            System.out.println("You have uncommitted changes.");
            return false;
        } else if (current.equals(other)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }
        List<String> currentFiles = plainFilenamesIn(CWD);
        for (String file : currentFiles) {
            if (!current.getBlobs().containsKey(file)) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, or add and commit it first");
                return false;
            }
        }
        return true;
    }

    private static void modifiedStatus() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        HashMap<String, String> commitFiles = Commit.getCommit(head).getBlobs();
        for (HashMap.Entry<String, String> pair : commitFiles.entrySet()) {
            String file = pair.getKey();
            String hash = pair.getValue();
            if (join(CWD, file).exists()) {
                String fileHash = Blobs.hashBlob(join(CWD, file));
                if (!hash.equals(fileHash)) {
                    System.out.println(file + " (modified)");
                }
            } else {
                if (!plainFilenamesIn(REMOVAL_FOLDER).contains(file)) {
                    System.out.println(file + " (deleted)");
                }
            }
        }
        List<String> stageFiles = plainFilenamesIn(STAGING_FOLDER);
        for (String file : stageFiles) {
            if (join(CWD, file).exists()) {
                String fileHash = Blobs.hashBlob(join(CWD, file));
                String stageHash = Blobs.hashBlob(join(STAGING_FOLDER, file));
                if (!stageHash.equals(fileHash)) {
                    System.out.println(file + " (modified)");
                }
            } else {
                System.out.println(file + " (deleted)");
            }
        }
    }

    private static void untrackedStatus() {
        System.out.println("=== Untracked Files ===");
        HashMap<String, String> commitFiles = Commit.getCommit(head).getBlobs();
        List<String> files = plainFilenamesIn(CWD);
        for (String file : files) {
            if (!commitFiles.containsKey(file) && !join(STAGING_FOLDER, file).exists()) {
                System.out.println(file);
            }
        }
    }

    public static void doMerge(String branchName) {
        Commit current = Commit.getCommit(head);
        if (!mergeCheck(current, branchName)) {
            System.exit(0);
        }
        Commit other = Commit.getCommit(Core.getHead(branchName));
        Commit lca = Merge.findLCA(current, other);
        boolean hadConflict = false;
        if (other.equals(lca)) {
            System.out.println("Given branch is an ancestor of the current branch.");
        } else if (current.equals(lca)) {
            System.out.println("Current branch fast-forwarded.");
            doBranchCheckout(branchName);
        } else {
            HashMap<String, String> lcaBlobs = lca.getBlobs();
            HashMap<String, String> currentBlobs = current.getBlobs();
            HashMap<String, String> otherBlobs = other.getBlobs();
            for (HashMap.Entry<String, String> pair : lcaBlobs.entrySet()) {
                String file = pair.getKey();
                String hash = pair.getValue();
                if (currentBlobs.containsValue(hash) || otherBlobs.containsValue(hash)) {
                    if (!otherBlobs.containsValue(hash)) {
                        if (otherBlobs.containsKey(file)) {
                            doCheckout(other.getId(), file);
                            add(file);
                        } else {
                            remove(file);
                        }
                    }
                } else {
                    boolean fileExists = Merge.conflict(current, other, file);
                    if (fileExists) {
                        hadConflict = true;
                        add(file);
                    }
                }
            }

            for (HashMap.Entry<String, String> pair : otherBlobs.entrySet()) {
                String file = pair.getKey();
                if (!lcaBlobs.containsKey(file)) {
                    if (!currentBlobs.containsKey(file)) {
                        doCheckout(other.getId(), file);
                    } else {
                        boolean fileExists = Merge.conflict(current, other, file);
                        if (fileExists) {
                            hadConflict = true;
                            add(file);
                        }
                    }
                    add(file);
                }
            }
            String s = String.format("Merged %s into %s.", branchName.replace("%", "/"),
                    getActiveBranch().replace("%", "/"));
            makeCommit(s, 2, other.getId());
            if (hadConflict) {
                System.out.println("Encountered a merge conflict.");
            }
        }

    }

}
