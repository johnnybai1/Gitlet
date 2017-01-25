import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class Commit implements Serializable {

    static final long serialVersionUID = 23571100001L;

    // Name files as such: "filename - CommitID"

    private HashMap<String, String> snapshot = new HashMap<String, String>();
    private int ID;
    private final String creationTime = setCreationTime();
    private String message;
    private int parentID;
    final ArrayList<Integer> test = new ArrayList<Integer>();

    /**
     * Default constructor, expected to be only called once. Creates the root of the commit tree with an ID of 0
     * and the message "initial commit."
     */
    public Commit() {
        ID = 0;
        message = "initial commit.";
        parentID = -1;
    }

    /**
     * Constructor that creates a new Commit with it's parent and message specified. The ID counts up from 0, based on
     * how many Commits have been created already.
     */
    public Commit(int parentID, String message) {
        ID = new File(".gitlet/commits").listFiles().length;
        this.message = message;
        this.parentID = parentID;
        snapshot = new HashMap<String, String>(loadCommit(parentID).snapshot);
    }

    public void setParent(int parentID) {
        this.parentID = parentID;
    }

    public int getParentID() {
        return parentID;
    }

    /**
     * Returns the commit's ID, which also happens to be the name of the .ser file saved in .gitlet/commits
     */
    public int getID() {
        return ID;
    }

    /**
     * Returns the commit's creation time
     */

    public String getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the commit's message
     */

    public String getCommitMessage() {
        return message;
    }

    /**
     * Processes the stage. It will update existing files and snapshot to point to the correct file copy.
     */
    public void process(Stage stage) {
        if (stage.isEmpty()) {
            System.out.println("No changes added to the commit.");
        }
        else {
            for (String filename : stage.getFiles()) {
                if (stage.getFlag(filename)) {
                    snapshot.put(filename, toGitletFile(filename));
                    copyFile(filename);
                } else snapshot.remove(filename);
            }
            stage.clear();
        }
    }

    /**
     * Copies the file with the filename in /Gitlet/ into /.gitlet/files/
     */
    private void copyFile(String filename) {
        try {
            Path src = Paths.get(filename);
            Path des = Paths.get("./.gitlet/files/" + toGitletFile(filename));
            Files.copy(src, des, StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(des, Files.getLastModifiedTime(src));
        }
        catch (IOException e) {
            System.out.println("Failed to copy " + filename);
        }
    }

    /**
     * Restores the filename at the current commit to the working directory
     */
    public void restoreFile(String filename) {
        try {
            Path src = Paths.get("./.gitlet/files/" + getGitletFile(filename));
            Path des = Paths.get(filename);
            Files.copy(src, des, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            System.out.println("Failed to restore " + filename);
        }
    }

    public static void mergeCommit(String branchName) {
        /**
         1. BRANCH and CURRENT_BRANCH
         2. Commit COMMON = getCommonAncestor(Branch.headID, CURRENT_BRANCH.headID)
         3. Loop through files of BRANCH.head
         4. COMMON.get(file) != Branch.head.get(file)
         */
        Branch branch = Branch.loadBranch();
        if (branch.getLiveBranch().equals(branchName)) {
            System.out.println("Cannot merge branch with itself.");
        }
        else if (!branch.getBranchNames().contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
        }
        else if (Gitlet.doDangerousRoutine()) {
            Commit branchHead = branch.getBranchHeadCommit(branchName);
            Commit currentHead = branch.getLiveBranchHeadCommit();
            Commit common = Commit.getCommonAncestor(branchHead.getID(), currentHead.getID());

            Stage stage = Stage.loadStage();
            Commit commit = new Commit(currentHead.getID(), "merge");
            // commit.snapshot == commit.parent.snapshot
            // In Master, merge branch --> we have the Master's snapshot
            // File1.txt
            // Master not modified, branch modified
            // commit.put(file1.txt, branch.getFile(file1.txt);
            for (String files : branchHead.getFilesSnapshot()) {
                String commonFile = common.getGitletFile(files); // COMMON ANCESTOR filenme
                String branchFile = branchHead.getGitletFile(files); // BRANCH_HEAD filename
                String currentFile = currentHead.getGitletFile(files); // CURRENT_BRANCH filename
                if (commonFile.equals(currentFile) && !commonFile.equals(branchFile)) {
                    commit.snapshot.put(files, branchFile);
                }
                else if (!commonFile.equals(branchFile) && !commonFile.equals(currentFile)) {
                    commit.snapshot.put(files + ".conflicted", branchFile + ".conflicted");
                    copyConflictedFile(files, branchHead);
                }
            }
            branch.updateHead(commit.getID());
            stage.updateLiveCommitID(commit.getID());
            commit.saveCommit();
            branch.saveBranch();
            stage.saveStage();
        }

    }

    public String getCommitDirectory() {
        return snapshot.toString();
    }

    public static void copyConflictedFile(String filename, Commit from) {
        try {
            Path src = Paths.get("./.gitlet/files/" + from.getGitletFile(filename));
            Path des = Paths.get("./.gitlet/files/" + from.getGitletFile(filename) + ".conflicted");
            Files.copy(src, des, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            System.out.println("Failed to copy conflicted file " + filename);
        }
    }

    /**
     * Returns the common Commit ancestor of commitIDs one and two
     */
    public static Commit getCommonAncestor(int one, int two) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Commit commitOne = loadCommit(one);
        Commit commitTwo = loadCommit(two);
        Date dateOne = sdf.parse(commitOne.creationTime, new ParsePosition(0));
        Date dateTwo = sdf.parse(commitTwo.creationTime, new ParsePosition(0));
        if (dateOne.before(dateTwo)) {
            return getCommonAncestor(one, commitTwo.getParentID());
        }
        else if (dateOne.after(dateTwo)) {
            return getCommonAncestor(commitOne.getParentID(), two);
        }
        else return commitOne;
    }

    public Set<String> getFilesSnapshot() {
        return snapshot.keySet();
    }

    /**
     * Returns the name of the file saved in /.gitlet/files/. It is the file's state in this commit
     */
    public String getGitletFile(String filename) {
        return snapshot.get(filename);
    }

    private String toGitletFile(String filename) {
        String[] split = filename.split("\\.", 2);
        String result = split[0] + "-" + ID;
        if (split.length == 1) {
            return result;
        }
        return result + "." + split[1];
    }

    /**
     * Prints the history of commits, starting with this one (the live one)
     */
    /*
    public void printHistory() {
        printHistory(this);
    }

    private static void printHistory(Commit start) {
        if (start == null) {
            return;
        }
        System.out.println(start.toString());
        printHistory(start.parent);
    }
    */

    /**
     * Copies the Commit nodes (root, node] to the end of the branchHead
     * node --> node.parent ... --> (node.parent == root is not included) branchHead
     * rebase [branch]
     * root = common ancestor between current branch and [branch]
     * current = current branch's node
     * branch = [branch]'s head node
     */
    public static int OLDrebase(Commit root, Commit current, Commit branch) {
        if (current.parentID != root.ID) {
            Commit commit = new Commit(current.ID, current.message);
            commit.setParent(current.parentID);
            commit.saveCommit();
            rebase(root, Commit.loadCommit(current.parentID), branch);
        }
        else {
            Commit commit = new Commit(current.ID, current.message);
            commit.setParent(branch.ID);
            commit.saveCommit();
            return commit.ID;
        }
        return -1;
    }

    public static int rebase(Commit root, Commit current, Commit branch) {
        int i = branch.ID;
        if (current.parentID != root.ID) {
            if (current.ID == branch.ID) {
                System.out.println("Already up-to-date");
                return -1;
            }
            i = rebase(root, Commit.loadCommit(current.parentID), branch);
        }
        Commit commit = new Commit(current.ID, current.message);
        commit.parentID = i;
        commit.saveCommit();
        i = commit.ID;
        return i;
    }

    public static int interactiveRebase(Commit root, Commit current, Commit branch) {
        int i = branch.ID;
        if (current.parentID != root.ID) {
            if (current.ID == branch.ID) {
                System.out.println("Already up-to-date");
                return -1;
            }
            i = rebase(root, Commit.loadCommit(current.parentID), branch);
        }
        System.out.println("Currently replaying:");
        System.out.println(current);
        System.out.println("Would you like to (c)ontinue, (s)kip this commit, or change the commit's (m)essage?");
        Scanner input = new Scanner(System.in);
        String response = input.next();
        switch (response) {
            case "c": {
                Commit commit = new Commit(current.ID, current.message);
                commit.parentID = i;
                commit.saveCommit();;
                i = commit.ID;
                return i;
            }
            case "s": {
            }
                break;
            case "m": {
                System.out.println("Please enter a new message for this commit.");
                String message = input.next();
                current.message = message;
                current.saveCommit();
            }
                break;
        }
        return i;
    }

    private String setCreationTime() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return sdf.format(now);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("====\n");
        sb.append("Commit ");
        sb.append(ID);
        sb.append(".\n");
        sb.append(creationTime);
        sb.append("\n");
        sb.append(message);
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Returns the commit object with the specified ID. Deserializes /.gitlet/commits/ID.ser
     */

    public static Commit loadCommit(int ID) {
        Commit myCommit = null;
        File commitFile = new File("./.gitlet/commits/" + ID + ".ser");
        if (commitFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(commitFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                myCommit = (Commit) ois.readObject();
            }
            catch (IOException e) {
                System.out.println("IOException in loadCommit() " + e);
            }
            catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException in loadCommit() " + e);;
            }
        }
        return myCommit;
    }

    /**
     * Serializes the commit object and saves it as /.gitlet/commits/ID.ser
     */
    public void saveCommit() {
        try {
            File stageFile = new File("./.gitlet/commits/" + ID + ".ser");
            FileOutputStream fos = new FileOutputStream(stageFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
        }
        catch (IOException e) {
            System.out.println("Error saving Commit " + e);
        }
    }



    public static void main(String[] args) {
    }

}
