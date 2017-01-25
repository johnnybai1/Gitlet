import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;

public class Stage implements Serializable {

    static final long serialVersionUID = 23571113002L;

    private final HashMap<String, Boolean> stage = new HashMap<>();
    private int liveCommit = 0;

    /**
     * 1. Check if /Gitlet/filename exists
     * 2. Check if /Gitlet/filename's lastModified attribute is different from
     *    /Gitlet/.gitlet/files/liveCommit.getGitletFile(filename)'s lastModified attribute
     */
    public void markAdd(String filename) {
        File file = new File(filename);
        // Fail case 1: if file does not exist
        if (!file.exists()) {
            System.out.println("File does not exist.");
        }
        // Fail case 2: if file has not been modified
        else if (!wasModified(filename)) {
            System.out.println("File has not been modified since the last commit");
        }
        // If our stage doesn't have this file, add it and mark it as add
        else if (!stage.containsKey(filename)) {
            stage.put(filename, true);
        }
        // Otherwise, try to remove the file marked with false from the stage
        else stage.remove(filename, false);

    }

    /**
     * 1. Check if filename exists in liveCommit
     * 2. Check if filename is staged to be added
     */
    public void markRemove(String filename) {
        // If stage doesn't contain the file..
        if (!stage.containsKey(filename)) {
            Commit live = Commit.loadCommit(liveCommit);
            // If our live commit has the file
            if (live.getGitletFile(filename) != null) {
                stage.put(filename, false);
            }
            // If stage doesn't have the file and live commit doesn't have the file
            else System.out.println("No reason to remove the file.");
        }
        else if (stage.get(filename)) {
            stage.remove(filename, true);
        }
    }

    /**
     * Returns true to add the file to the commit or false to remove the file from the comit
     */

    public boolean getFlag(String file) {
        return stage.get(file);
    }

    /**
     * Clears our stage, should be done after processing
     */

    public void clear() {
        stage.clear();
    }

    /**
     * Checks to see if our stage is empty. Should be empty after each commit
     */

    public boolean isEmpty() {
        return stage.isEmpty();
    }

    public Set<String> getFiles() {
        return stage.keySet();
    }

    /**
     * liveCommit will point to the active commit. It will either be the Commit checked out, or the Commit last added
     */
    private boolean wasModified(String filename) {
        Commit live = Commit.loadCommit(liveCommit);
        if (live.getGitletFile(filename) == null) {
            // If the file doesn't exist in liveCommit...
            return true;
        }
        Path currentFile = Paths.get(filename);
        Path commitFile = Paths.get("./.gitlet/files/" + live.getGitletFile(filename));
        try {
            return Files.getLastModifiedTime(currentFile) != Files.getLastModifiedTime(commitFile);
        }
        catch (IOException e){
            System.out.println("IOException in wasModified");
        }
        return false;
    }

    public int getLiveCommitID() {
        return liveCommit;
    }

    public void updateLiveCommitID(int commitID) {
        liveCommit = commitID;
    }

    public static Stage loadStage() {
        Stage myStage = null;
        File stageFile = new File("./.gitlet/STAGE.ser");
        if (stageFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(stageFile);
                ObjectInputStream ois = new ObjectInputStream(fis);
                myStage = (Stage) ois.readObject();
            }
            catch (IOException e) {
                System.out.println("IOException in loadStage() " + e);
            }
            catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException in loadStage() " + e);;
            }
        }
        return myStage;
    }

    public void saveStage() {
        try {
            File stageFile = new File("./.gitlet/STAGE.ser");
            FileOutputStream fos = new FileOutputStream(stageFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
        }
        catch (IOException e) {
            System.out.println("Error saving stage " + e);
        }
    }

}
