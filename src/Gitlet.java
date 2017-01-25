import java.io.*;
import java.util.Scanner;

/**
 * Driver for the Gitlet program. Reads I/O only
 */
public class Gitlet {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No command inputted");
            return;
        }
        switch (args[0]) {
            case "init": {
                init();
            }
            break;
            case "add": {
                if (args[1] == null) {
                    break;
                }
                Stage stage = Stage.loadStage();
                stage.markAdd(args[1]);
                stage.saveStage();
            }
            break;
            case "commit": {
                if (args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                Stage stage = Stage.loadStage();
                Commit live = Commit.loadCommit(stage.getLiveCommitID());
                Commit commit = new Commit(live.getID(), args[1]);
                commit.process(stage);
                stage.updateLiveCommitID(commit.getID());
                stage.saveStage();
                commit.saveCommit();
                Branch branch = Branch.loadBranch();
                if (branch.getLiveBranchHeadID() != live.getID()) {
                    branch.createBranch(commit.getID());
                }
                else {
                    branch.updateHead(commit.getID());
                }
                branch.saveBranch();
            }
            break;
            case "rm": {
                if (args[1] == null) {
                    break;
                }
                Stage stage = Stage.loadStage();
                stage.markRemove(args[1]);
                stage.saveStage();
            }
            break;
            case "log": {
                Stage stage = Stage.loadStage();
                int currentCommitID = stage.getLiveCommitID();
                while (currentCommitID != -1) {
                    Commit current = Commit.loadCommit(currentCommitID);
                    System.out.println(current);
                    currentCommitID = current.getParentID();
                }
            }
            break;
            case "global-log": {
                Branch branch = Branch.loadBranch();
                int numCommits = new File("./.gitlet/commits").listFiles().length;
                boolean[] seen = new boolean[numCommits];
                for (String currBranch : branch.getBranchNames()) {
                    int currCommitID = branch.getBranchHeadID(currBranch);
                    while (currCommitID != -1 && !seen[currCommitID]) {
                        Commit currCommit = Commit.loadCommit(currCommitID);
                        System.out.println(currCommit);
                        seen[currCommitID] = true;
                        currCommitID = currCommit.getParentID();
                    }
                    continue;

                }
            }
            break;
            case "find": {

            }
            break;
            case "status": {
                System.out.println("=== Branches ===");
                Branch branch = Branch.loadBranch();
                for (String names : branch.getBranchNames()) {
                    if (branch.getLiveBranch().equals(names)) {
                        System.out.println("*" + names);
                    }
                    else
                        System.out.println(names);
                }
                Stage stage = Stage.loadStage();
                StringBuilder staged = new StringBuilder();
                StringBuilder removed = new StringBuilder();
                for (String files : stage.getFiles()) {
                    if (stage.getFlag(files)) {
                        staged.append(files);
                        staged.append("\n");
                    }
                    else {
                        removed.append(files);
                        removed.append("\n");
                    }
                }
                System.out.println("=== Staged Files ===");
                System.out.println(staged.toString());
                System.out.println("=== Files Marked for Removal ===");
                System.out.println(removed.toString());
            }
            break;
            case "checkout": {
                Stage stage = Stage.loadStage();
                Branch branch = Branch.loadBranch();
                String param1 = args[1];
                if (args.length == 2) {
                    if (!branch.getBranchNames().contains(param1)) {
                        // If we don't have a branch of that name, check to see if we have a file of that name
                        Commit headCommit = branch.getLiveBranchHeadCommit();
                        if (!headCommit.getFilesSnapshot().contains(param1)) {
                            // If we don't have a file either...
                            System.out.println("File does not exist in the most recent commit, or no such branch exists.");
                            break;
                        }
                        else if (doDangerousRoutine()){
                            // If we do have a file in the branch head, restore it
                            headCommit.restoreFile(param1);
                        }
                    }
                    else if (branch.getLiveBranch().equals(param1)) {
                        System.out.println("No need to checkout the current branch");
                    }
                    else if(doDangerousRoutine()){
                        // If we do have a branch of that name, restore the entire commit
                        Commit commit = branch.getBranchHeadCommit(param1);
                        for (String files : commit.getFilesSnapshot()) {
                            commit.restoreFile(files);
                        }
                        // Update current branch and live commit
                        stage.updateLiveCommitID(commit.getID());
                        branch.changeBranch(param1);
                        stage.saveStage();
                        branch.saveBranch();
                    }
                }
                if (args.length == 3) {
                    int commitID = Integer.parseInt(args[1]);
                    String filename = args[2];
                    Commit commit = Commit.loadCommit(commitID);
                    if (commit == null) {
                        System.out.println("No commit with that id exists.");
                    }
                    else if(commit.getGitletFile(args[2]) == null) {
                        System.out.println("File does not exist in that commit");
                    }
                    else {
                        if (doDangerousRoutine()) {
                            commit.restoreFile(args[2]);
                            // No need to update head and branch pointers
                        }
                    }
                }
            }
            break;
            case "branch": {
                Branch branch = Branch.loadBranch();
                Stage stage = Stage.loadStage();
                String branchName = args[1];
                if (branch.getBranchNames().contains(branchName)) {
                    System.out.println("A branch with that name already exists.");
                }
                else {
                    branch.createBranch(branchName, stage.getLiveCommitID());
                    branch.saveBranch();
                }
            }
            break;
            case "rm-branch": {
                Branch branch = Branch.loadBranch();
                String branchName = args[1];
                branch.removeBranch(branchName);
                branch.saveBranch();
            }
            break;
            case "reset": {
                int commitID = Integer.parseInt(args[1]);
                Commit commit = Commit.loadCommit(commitID);
                if (commit == null) {
                    System.out.println("No commit with that id exists");
                }
                else if (doDangerousRoutine()) {
                    for (String files : commit.getFilesSnapshot()) {
                        commit.restoreFile(files);
                    }
                    Stage stage = Stage.loadStage();
                    stage.updateLiveCommitID(commit.getID());
                    stage.saveStage();
                }
            }
            break;
            case "merge": {
                String branchName = args[1];
                Commit.mergeCommit(branchName);
            }
            break;
            case "rebase": {
                String branchName = args[1];
                Branch branch = Branch.loadBranch();
                Stage stage = Stage.loadStage();
                if (branchName.equals(branch.getLiveBranch())) {
                    System.out.println("Cannot rebase a branch onto itself.");
                    break;
                }
                if (!branch.getBranchNames().contains(branchName)) {
                    System.out.println("A branch with that name does not exist.");
                    break;
                }
                Commit root = Commit.getCommonAncestor(branch.getLiveBranchHeadID(), branch.getBranchHeadID(branchName));
                Commit current = branch.getLiveBranchHeadCommit();
                Commit bHead = branch.getBranchHeadCommit(branchName);
                int newHead = Commit.rebase(root, current, bHead);
                if (newHead != -1) {
                    branch.updateHead(newHead);
                    branch.saveBranch();
                    stage.updateLiveCommitID(newHead);
                    stage.saveStage();
                }
            }
            break;
            case "i-rebase": {
                String branchName = args[1];
                Branch branch = Branch.loadBranch();
                Stage stage = Stage.loadStage();
                if (branchName.equals(branch.getLiveBranch())) {
                    System.out.println("Cannot rebase a branch onto itself.");
                    break;
                }
                if (!branch.getBranchNames().contains(branchName)) {
                    System.out.println("A branch with that name does not exist.");
                    break;
                }
                Commit root = Commit.getCommonAncestor(branch.getLiveBranchHeadID(), branch.getBranchHeadID(branchName));
                Commit current = branch.getLiveBranchHeadCommit();
                Commit bHead = branch.getBranchHeadCommit(branchName);
                int newHead = Commit.interactiveRebase(root, current, bHead);
                if (newHead != -1) {
                    branch.updateHead(newHead);
                    branch.saveBranch();
                    stage.updateLiveCommitID(newHead);
                    stage.saveStage();
                }
            }
            break;
        }
    }

    // Everything that needs to be done on Gitlet initialization
    public static void init() {
        File file = new File("./.gitlet/commits");
        if (file.exists()) {
            System.out.println("A gitlet version control system already exists in the current directory.");
        }
        else {
            file.mkdirs(); // Create .gitlet and .gitlet/commits
            File files = new File("./.gitlet/files");
            files.mkdir();
            new Commit().saveCommit(); // create our first commit
            new Branch().saveBranch();
            new Stage().saveStage();
        }
    }

    public static boolean doDangerousRoutine() {
        System.out.println("Warning: The command you entered may alter the files in your working directory.");
        System.out.println("Uncommitted changes may be lost. Are you sure you want to continue? (yes/no)");
        Scanner input = new Scanner(System.in);
        String response = input.next();
        input.close();
        if ("yes".equals(response)) {
            return true;
        }
        return false;
    }


}

