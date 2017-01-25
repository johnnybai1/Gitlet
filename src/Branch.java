import java.io.*;
import java.util.HashMap;
import java.util.Set;

// To be created upon Gitlet init
// Will store branches and CURRENT pointer
// Current = *
public class Branch implements Serializable {

    static final long serialVersionUID = 23571113173L;

    private final HashMap<String, Integer> heads = new HashMap<>();
    private String liveBranch;

    public Branch() {
        liveBranch = "master";
        heads.put("master", 0);
    }

    public void createBranch(String branchName, int ID) {
        if (heads.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        }
        else {
            heads.put(branchName, ID);
        }
    }

    /**
     * Our default create branch, which will be used whenever we have a fork
     */
    public void createBranch(int ID) {
        String branchName = generateBranchName();
        heads.put(branchName, ID);
        changeBranch(branchName);

    }

    public void removeBranch(String branchName) {
        if (branchName.equals(liveBranch)) {
            System.out.println("Cannot remove the current branch.");
        }
        else if (!heads.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        }
        else {
            heads.remove(branchName);
        }
    }

    public void updateHead(int ID) {
        heads.put(liveBranch, ID);
    }

    private String generateBranchName() {
        String branch = "branch";
        int i = 0;
        do {
            if (i > 0) {
                branch = branch + "-" + i;
            }
            i++;
        }
        while(heads.containsKey(branch));
        return branch;
    }

    /**
     * Changes the "live branch" to the specified branch
     */
    public void changeBranch(String newBranch) {
        liveBranch = newBranch;
    }

    /**
     * Returns the Commit ID of the specified branch's head
     */
    public int getBranchHeadID(String branchName) {
        return heads.get(branchName);
    }

    /**
     * Returns the Commit Object of the specified branch's head
     */
    public Commit getBranchHeadCommit(String branchName) {
        return Commit.loadCommit(heads.get(branchName));
    }

    /**
     * Returns the name of the current liveBranch
     */
    public String getLiveBranch() {
        return liveBranch;
    }

    /**
     * Returns the Commit ID of the current branch's head
     */
    public int getLiveBranchHeadID() {
        return getBranchHeadID(liveBranch);
    }

    /**
     * Returns the Commit Object of the current branch's head
     */
    public Commit getLiveBranchHeadCommit() {
        return getBranchHeadCommit(liveBranch);
    }

    public Set<String> getBranchNames() {
        return heads.keySet();
    }

    public static Branch loadBranch() {
        Branch myBranch = null;
        File branchFile = new File("./.gitlet/HEAD.ser");
        if (branchFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(branchFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                myBranch = (Branch) ois.readObject();
            }
            catch (IOException e) {
                System.out.println("IOException in loadBranch() " + e);
            }
            catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException in loadBranch() " + e);;
            }
        }
        return myBranch;
    }

    public void saveBranch() {
        try {
            File branchFile = new File("./.gitlet/HEAD.ser");
            FileOutputStream fos = new FileOutputStream(branchFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
        }
        catch (IOException e) {
            System.out.println("Error saving Branch " + e);
        }
    }

}
