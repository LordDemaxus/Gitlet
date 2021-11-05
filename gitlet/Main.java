package gitlet;


/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Medhaav Chandra Mahesh
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.initialize();
                System.exit(0);
                break;
            default:
                break;
        }
        if (Core.getCore()) {
            initizialized(args);
        } else {
            System.out.print("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static void initizialized(String[] args) {
        String firstArg = args[0];
        switch (firstArg) {
            case "add":
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length == 2 && args[1].length() > 0) {
                    Repository.makeCommit(args[1], 1, null);
                } else {
                    System.out.println("Please enter a commit message.");
                }
                break;
            case "checkout":
                if (args.length == 2) {
                    Repository.doBranchCheckout(args[1]);
                } else if (args.length == 3 && args[1].equals("--")) {
                    Repository.doCheckout(Repository.head, args[2]);
                } else if (args.length == 4 && args[2].equals("--")) {
                    Repository.doCheckout(args[1], args[3]);
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                break;
            case "log":
            case "global-log":
                Repository.getLog(firstArg);
                break;
            case "rm":
                Repository.remove(args[1]);
                break;
            case "find":
                Repository.find(args[1]);
                break;
            case "status":
                Repository.status();
                break;
            case "branch":
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                if (!Core.getActiveBranch().equals(args[1])) {
                    Repository.removeBranch(args[1]);
                } else {
                    System.out.println("Cannot remove the current branch.");
                    System.exit(0);
                }
                break;
            case "reset":
                Repository.reset(args[1]);
                break;
            case "merge":
                Repository.doMerge(args[1]);
                break;
            case "add-remote":
                Remote.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                Remote.removeRemote(args[1]);
                break;
            case "push":
                Remote.push(args[1], args[2]);
                break;
            case "fetch":
                Remote.fetch(args[1], args[2]);
                break;
            case "pull":
                String newBranch = Remote.fetch(args[1], args[2]);
                Repository.doMerge(newBranch);
                break;
            default:
                System.out.print("No command with that name exists");
                System.exit(0);
        }
    }
}
