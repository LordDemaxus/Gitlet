package gitlet;



import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

import static gitlet.Repository.CWD;
import static gitlet.Utils.*;

/**
 * Does some merge functions
 * Finds Latest Common Ancestor using a BFS helper method and handles conflict cases
 *
 * @author Medhaav Chandra Mahesh
 *
 * @Source BFS Method: Lecture 24 */

public class Merge {

    private static TreeMap<Integer, Commit> currentAncestors;
    private static Commit LCA;

    public static Commit findLCA(Commit current, Commit other) {
        currentAncestors = new TreeMap<>();
        LCA = Repository.initialCommit;
        currentAncestors.put(0, current);
        commitBFS(current, 1);
        commitBFS(other, 2);
        return LCA;
    }

    private static void commitBFS(Commit commit, int goTo) {
        TreeMap<Integer, Commit> ancestors = new TreeMap<>();
        ArrayList<Integer> edgeTo = new ArrayList<>();
        PriorityQueue<Integer> fringe = new PriorityQueue<>();
        ancestors.put(0, commit);
        helper(0, commit.getId(), goTo);
        fringe.add(0);
        int i = 1;
        while (!fringe.isEmpty()) {
            if (LCA != Repository.initialCommit) {
                break;
            }
            int v = fringe.remove();
            if (ancestors.get(v) != null) {
                for (String w : ancestors.get(v).getParents()) {
                    if (w != null && !ancestors.containsValue(Commit.getCommit(w))) {
                        ancestors.put(i, Commit.getCommit(w));
                        helper(i, w, goTo);
                        fringe.add(i);
                        edgeTo.add(v);
                        i += 1;
                        if (LCA != Repository.initialCommit) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void helper(int pos, String ancestor, int goTo) {
        if (goTo == 1) {
            currentAncestors.put(pos, Commit.getCommit(ancestor));
        } else if (goTo == 2) {
            Commit otherParent = Commit.getCommit(ancestor);
            if (currentAncestors.containsValue(otherParent) && otherParent != null) {
                LCA = otherParent;
            }
        }
    }

    public static boolean conflict(Commit current, Commit other, String fileName) {
        boolean fileExists = true;
        HashMap<String, String> currentBlobs = current.getBlobs();
        HashMap<String, String> otherBlobs = other.getBlobs();
        String currentContents = "\n";
        String otherContents = "";
        if (currentBlobs.containsKey(fileName)) {
            String currentHash = currentBlobs.get(fileName);
            currentContents = new String(Blobs.readBlob(currentHash), StandardCharsets.UTF_8);
            if (otherBlobs.containsKey(fileName)) {
                if (!otherBlobs.containsValue(currentHash)) {
                    String otherHash = otherBlobs.get(fileName);
                    otherContents = new String(Blobs.readBlob(otherHash), StandardCharsets.UTF_8);
                    File file = join(CWD, fileName);
                    writeContents(file, "<<<<<<< HEAD\n", currentContents,
                            "=======\n", otherContents, ">>>>>>>\n");
                    Repository.add(fileName);
                }
            } else {
                File file = join(CWD, fileName);
                writeContents(file, "<<<<<<< HEAD\n", currentContents,
                        "=======\n", otherContents, ">>>>>>>\n");
                Repository.add(fileName);
            }
        } else {
            if (otherBlobs.containsKey(fileName)) {
                Repository.doCheckout(other.getId(), fileName);
                String otherHash = otherBlobs.get(fileName);
                otherContents = new String(Blobs.readBlob(otherHash), StandardCharsets.UTF_8);
                File file = join(CWD, fileName);
                writeContents(file, "<<<<<<< HEAD\n", currentContents,
                        "=======\n", otherContents, ">>>>>>>\n");
                Repository.add(fileName);
            } else {
                fileExists = false;
            }
        }
        return fileExists;
    }

}
