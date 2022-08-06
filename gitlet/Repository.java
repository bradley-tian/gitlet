package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

/**
 * Represents the working Gitlet repository; the primary class
 * carrying out most operations of Gitlet.
 *
 * @author Bradley Tian
 */
public class Repository {

    /**
     * Path to the current working directory.
     */
    private static File cwd = new File(System.getProperty("user.dir"));

    /**
     * Path to the index file, which contains the staging area.
     */
    private static File index = new File("./.gitlet/index");

    /**
     * Path to the HEAD file, which contains the paths to the
     * currently active branch.
     */
    private static File head = new File("./.gitlet/HEAD");

    /**
     * Path to the refs directory, which contains all branches
     * within the repository.
     */
    private static File refs = new File("./.gitlet/refs");

    /**
     * Path to the commits directory, which contains all commits
     * within the repository.
     */
    private static File commits = new File("./.gitlet/commits");

    /**
     * Path to the blobs directory, which contains all content blobs
     * within the repository.
     */
    private static File blobs = new File("./.gitlet/blobs");

    /**
     * Path to the remotes directory, which contains all records of
     * connected remote gitlet repositories.
     */
    private static File remotes = new File("./.gitlet/remotes");

    /**
     * The staging area, which tracks files to add and files to remove.
     */
    private StagingArea stage;

    /**
     * Mapping of all branches and the commits they point to.
     */
    private TreeMap<String, Commit> branches;

    /**
     * Constructor of a new repository. Reads in necessary information
     * from local files.
     */
    public Repository() {
        stage = Utils.readObject(index, StagingArea.class);
        branches = new TreeMap<>();

        for (String name : Utils.plainFilenamesIn(refs)) {
            String commitHash = Utils.readContentsAsString(Utils.join(
                    refs + "/" + name));
            Commit comm = Utils.readObject(Utils.join(
                    commits + "/" + commitHash), Commit.class);
            branches.put(name, comm);
        }
    }

    /**
     * Initializes a new .gitlet repository within the current working
     * directory. Can be called without precedent existence of a .gitlet
     * repository.
     */
    public static void init() {
        StagingArea stage = new StagingArea();
        File gitlet = new File("./.gitlet");
        if (gitlet.exists()) {
            System.out.println("A Gitlet version-control system already"
                    + " exists in the current directory.");
            System.exit(0);
        }
        gitlet.mkdir();
        try {
            index.createNewFile();
            Utils.writeObject(index, stage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        refs.mkdir();
        commits.mkdir();
        blobs.mkdir();
        remotes.mkdir();
        try {
            head.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Commit initial = new Commit("initial commit", null);
        String initHash = initial.getHashName();
        File master = new File("./.gitlet/refs/master");
        try {
            master.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(master, initHash);
        File commit = new File("./.gitlet/commits/" + initHash);
        try {
            commit.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(commit, initial);
        Utils.writeContents(head, master.getPath());
    }

    /**
     * Parses user commands and calls on the respective methods to carry
     * out proper operations.
     * @param args the series of user commands.
     */
    public void parseCommands(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
            case "log":
                this.log();
                break;
            case "global-log":
                this.globalLog();
                break;
            case "status":
                this.status();
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
            }
        } else if (args.length == 2) {
            switch (args[0]) {
            case "add":
                this.add(args[1]);
                break;
            case "commit":
                this.commit(args[1], null);
                break;
            case "rm":
                this.rm(args[1]);
                break;
            case "checkout":
                this.checkout(args[1], false);
                break;
            case "find":
                this.find(args[1]);
                break;
            case "branch":
                this.branch(args[1]);
                break;
            case "rm-branch":
                this.rmBranch(args[1]);
                break;
            case "reset":
                this.reset(args[1]);
                break;
            case "merge":
                this.merge(args[1]);
                break;
            case "rm-remote":
                this.rmRemote(args[1]);
            default:
                System.out.println("No command with that name exists.");
                break;
            }
        } else if (args.length == 3) {
            switch (args[0]) {
            case "checkout":
                this.checkoutParse(args);
                break;
            case "add-remote":
                this.addRemote(args[1], args[2]);
                break;
            case "push":
                this.push(args[1], args[2]);
            case "fetch":
                this.fetch(args[1], args[2]);
            case "pull":
                this.pull(args[1], args[2]);
            }
        } else {
            System.out.println("Incorrect operands");
            System.exit(0);
        }
    }

    /**
     * Check if the current working file is the same as any files
     * existing in the current working directory of the head commit.
     * If there is a match, refrain from adding the file to stage/
     * remove the file from stage if it is already staged.
     * If there is no match, add the current working file to the staging area.
     * @param fileName Name of the file to be staged.
     */
    public void add(String fileName) {
        stage = Utils.readObject(index, StagingArea.class);
        if (stage.containsInRemove(fileName)) {
            stage.removeFromRemove(fileName);
            Utils.writeObject(index, stage);
            return;
        }

        File target = Utils.join(cwd, fileName);
        if (!target.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Commit headCommit = getHeadCommit();
        String content = Utils.readContentsAsString(target);
        String blobHash = Utils.sha1(content);
        if (headCommit.containsBlob(fileName)
                && headCommit.getBlob(fileName).equals(blobHash)) {
            return;
        }
        File blob = Utils.join(blobs, blobHash);
        if (!blob.exists()) {
            try {
                blob.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeContents(blob, content);
        stage.addToStage(fileName, blobHash);
        Utils.writeObject(index, stage);
    }

    /**
     * Create a new commit with the log message denoted by the
     * input parameters. By default, the new commit's content tree
     * should refer back to its parent commits for all files that
     * remain unchanged. For those files changed, added to the staging
     * and removal areas, change create, or remove corresponding nodes
     * in the commit's content blob mapping.
     * @param message the log message of the commit.
     * @param otherParent the second parent, if any, of the new commit.
     */
    public void commit(String message, Commit otherParent) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        Commit currentHead = getHeadCommit();
        Commit newCommit = currentHead.clone();
        newCommit.setMessage(message);
        stage = Utils.readObject(index, StagingArea.class);
        if (stage.isAddEmpty() && stage.isRemoveEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        for (Object key : stage.additionKeySet()) {
            String name = (String) key;
            newCommit.setBlob(name, stage.getFromAddition(name));
        }
        for (Object key : stage.removalKeySet()) {
            String name = (String) key;
            newCommit.removeBlob(name);
        }
        if (otherParent != null) {
            newCommit.setOtherParent(otherParent.getHashName());
        }
        stage.clearStage();
        Utils.writeObject(index, stage);
        String hash = newCommit.getHashName();

        File newCommitFile = Utils.join(commits, hash);
        try {
            newCommitFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(newCommitFile, newCommit);
        File activeBranch = new File(Utils.readContentsAsString(head));
        Utils.writeContents(activeBranch, hash);
    }

    /**
     * If the input file is in the staging area, remove it.
     * Additionally, add the input file to the removal staging area.
     * If the input file is not found within the head commit's
     * contents tree, print error message.
     * @param fileName name of the file to remove.
     */
    public void rm(String fileName) {
        Commit comm = getHeadCommit();
        stage = Utils.readObject(index, StagingArea.class);
        if (!stage.containsInAdd(fileName)
                && !comm.containsBlob(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (stage.containsInAdd(fileName)) {
            stage.removeFromAddition(fileName);
        }
        if (comm.containsBlob(fileName)) {
            stage.addToRemove(fileName);
            Utils.restrictedDelete(Utils.join(cwd, fileName));
        }
        Utils.writeObject(index, stage);
    }

    /**
     * Recursively print out the commit id, timestamp, and commit log
     * message of each commits in the historical sequence beginning
     * with the head commit. Ignore any second parents.
     */
    public void log() {
        Commit comm = getHeadCommit();
        while (comm != null) {
            System.out.println("===");
            System.out.println("commit " + comm.getHashName());
            if (comm.hasSecondParent()) {
                System.out.println("Merge: "
                        + comm.getParent().substring(0, 7)
                        + " "
                        + comm.getOtherParent().substring(0, 7));
            }
            System.out.println("Date: " + comm.getTimeStamp() + " -0800");
            System.out.println(comm.getMessage());
            System.out.println();
            if (comm.getParent() == null) {
                comm = null;
            } else {
                comm = Utils.readObject(Utils.join(
                        commits + "/" + comm.getParent()), Commit.class);
            }
        }
    }

    /**
     * Print out the aforementioned id, timestamp, and message
     * of every commit made in history.
     */
    public void globalLog() {
        for (String name : Utils.plainFilenamesIn(commits)) {
            Commit comm = Utils.readObject(
                    Utils.join(commits + "/" + name), Commit.class);
            System.out.println("===");
            System.out.println("commit " + comm.getHashName());
            if (comm.hasSecondParent()) {
                System.out.println("Merge: "
                        + comm.getParent().substring(0, 7)
                        + " "
                        + comm.getOtherParent().substring(0, 7));
            }
            System.out.println("Date: " + comm.getTimeStamp() + " -0800");
            System.out.println(comm.getMessage());
            System.out.println();
        }
    }

    /**
     * Prints out the ids of all commits that have the matching commit message.
     * @param message the log message to match commits with.
     */
    public void find(String message) {
        boolean found = false;
        for (String name : Utils.plainFilenamesIn(commits)) {
            Commit comm = Utils.readObject(
                    Utils.join(commits + "/" + name), Commit.class);
            if (comm.getMessage().equals(message)) {
                System.out.println(comm.getHashName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /**
     * Display all branches that currently exist,
     * indicating the head branch head with "*". Also displays all files
     * added to addition and removal staging areas.
     */
    public void status() {
        System.out.println("=== Branches ===");
        Object[] contentBranches = Utils.plainFilenamesIn(refs).toArray();
        File currBranch = new File(Utils.readContentsAsString(head));
        for (String branch : sortNames(contentBranches)) {
            if (branch.equals(currBranch.getName())) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println("\n=== Staged Files ===");
        stage = Utils.readObject(index, StagingArea.class);
        for (String file : sortNames(stage.additionKeySet())) {
            System.out.println(file);
        }
        System.out.println("\n=== Removed Files ===");
        String[] sortedRem = sortNames(stage.removalKeySet().toArray());
        for (String file : sortedRem) {
            System.out.println(file);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        ArrayList<String> changed = new ArrayList<>();
        for (String file : sortNames(stage.additionKeySet())) {
            String stagedContent = Utils.readContentsAsString(
                    Utils.join(blobs, stage.getFromAddition(file)));
            if (!Utils.join(cwd, file).exists()) {
                changed.add(file + " (deleted)");
            } else if (!stagedContent.equals(
                    Utils.readContentsAsString(Utils.join(cwd, file)))) {
                changed.add(file + " (modified)");
            }
        }
        for (Object file : getHeadCommit().getBlobKeys()) {
            String name = (String) file;
            String content = Utils.readContentsAsString(
                    Utils.join(blobs, getHeadCommit().getBlob(name)));
            File cwdFile = Utils.join(cwd, name);
            if (!cwdFile.exists() && !stage.containsInRemove(name)) {
                changed.add(name + " (deleted)");
            } else if (cwdFile.exists() && !stage.containsInAdd(name)
                    && !content.equals(Utils.readContentsAsString(cwdFile))) {
                changed.add(name + " (modified)");
            }
        }
        for (Object file : sortNames(changed.toArray())) {
            System.out.println((String) file);
        }
        System.out.println("\n=== Untracked Files ===");
        ArrayList<String> untracked = new ArrayList<>();
        for (String file : Utils.plainFilenamesIn(cwd)) {
            if (!getHeadCommit().containsBlob(file)
                    && !stage.containsInAdd(file)) {
                untracked.add(file);
            }
        }
        for (Object file : sortNames(untracked.toArray())) {
            System.out.println((String) file);
        }
    }

    /**
     * Sort the names of a String series in lexicographic order.
     * @param series the unsorted series of Strings.
     * @return the sorted array of Strings.
     */
    public String[] sortNames(Object[] series) {
        String[] sorted = new String[series.length];
        for (int i = 0; i < series.length; i++) {
            sorted[i] = (String) series[i];
        }
        Arrays.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    /**
     * Parses user commands and calls on the proper version of
     * checkout operations.
     * @param params the record of user commands.
     */
    public void checkoutParse(String[] params) {
        if (params.length == 3 && params[1].equals("--")) {
            checkout(params[2], true);
        } else if (params.length == 4 && params[2].equals("--")) {
            checkout(params[1], params[3]);
        } else {
            System.out.println("Incorrect operand");
            System.exit(0);
        }
    }

    /**
     * For checking out single files, this overwrites the file
     * in the working directory with the matching file within
     * the content tree of the head commit. If the given file
     * does not exist in the contents tree, throw error message.
     * For checking out an entire branch, this finds the head of the branch
     * with the given name (referring the head branch pointer) and
     * overwrites the working directory with all files from that head
     * commit's content tree. If the branch in question is the
     * current working branch, error messages are then thrown. If there
     * is any untracked files in the current directory, asks the user
     * to stage or remove the file first.
     * @param fileName name of the file to check out.
     * @param isFile checks whether a file or an entire branch
     *               is being checked out.
     */
    public void checkout(String fileName, boolean isFile) {
        if (isFile) {
            Commit headComm = getHeadCommit();
            String checkout = Utils.readContentsAsString(
                    Utils.join(blobs, headComm.getBlob(fileName)));
            File replace = Utils.join(cwd, fileName);
            if (!replace.exists()) {
                try {
                    replace.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(replace, checkout);
        } else {
            File branch = Utils.join(refs, fileName);
            if (!branch.exists()) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }

            String newHeadHash = Utils.readContentsAsString(branch);
            Commit newComm = Utils.readObject(
                    Utils.join(commits + "/" + newHeadHash), Commit.class);
            stage = Utils.readObject(index, StagingArea.class);

            if (fileName.equals(new File(
                    Utils.readContentsAsString(head)).getName())) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            for (String file : Utils.plainFilenamesIn(cwd)) {
                if (!getHeadCommit().containsBlob(file)
                        && !stage.containsInAdd(file)) {
                    System.out.println(
                            "There is an untracked file in the way; delete"
                                    + " it, or add and commit it first.");
                    System.exit(0);
                }
            }
            checkoutCommit(newHeadHash);
            Utils.writeContents(
                    head, Utils.join(refs + "/" + fileName).getPath());
        }
    }

    /**
     * Handles the operation for checking out an entire commit.
     * @param commitHash Hashcode of the commit to be checked out.
     */
    public void checkoutCommit(String commitHash) {
        File commitFile = Utils.join(commits + "/" + commitHash);
        Commit newComm = Utils.readObject(
                Utils.join(commitFile), Commit.class);

        for (Object key : newComm.getBlobKeys()) {
            String file = newComm.getBlob((String) key);
            String checkout = Utils.readContentsAsString(
                    Utils.join(blobs + "/" + file));
            File replace = Utils.join(cwd, (String) key);
            if (!replace.exists()) {
                try {
                    replace.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(replace, checkout);
        }

        for (String file : Utils.plainFilenamesIn(cwd)) {
            if (!newComm.containsBlob(file)) {
                Utils.restrictedDelete(Utils.join(cwd, file));
            }
        }
        stage.clearStage();
        Utils.writeObject(index, stage);
    }

    /**
     * Overwrites the file in the working directory with the
     * matching file from the commit with the matching commit ID.
     * If no commits with such an ID exists, throw error message.
     * @param commit Hashcode of the commit to be searched.
     * @param fileName Name of the specific file to be checked
     *                 out in the commit.
     */
    public void checkout(String commit, String fileName) {
        File targetCommitFile = null;
        for (Object commitFile : Utils.plainFilenamesIn(commits)) {
            if ((((String) commitFile).substring(
                    0, commit.length())).equals(commit)) {
                targetCommitFile = Utils.join(commits, (String) commitFile);
            }
        }

        if (targetCommitFile == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit targetCommit = Utils.readObject(
                targetCommitFile, Commit.class);
        String checkout = Utils.readContentsAsString(
                Utils.join(blobs + "/" + targetCommit.getBlob(fileName)));
        File replace = new File("./" + fileName);

        if (!replace.exists()) {
            try {
                replace.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeContents(replace, checkout);
    }

    /**
     * Create a new branch with the name denoted by the input parameter.
     * The current commit head (acquired by searching through the heads
     * map with the current branch pointer) is assigned to the newly
     * created branch. No real changes are applied unless committing is
     * performed with the new branch.
     * @param branchName Name of the branch to be created.
     */
    public void branch(String branchName) {
        File tgtBranch = Utils.join(refs + "/" + branchName);
        if (tgtBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        try {
            tgtBranch.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(tgtBranch, getHeadCommit().getHashName());
    }

    /**
     * Remove the branch with the current name. The branch
     * cannot be the current working branch.
     * @param branchName Name of the branch to be removed.
     */
    public void rmBranch(String branchName) {
        File tgtBranch = Utils.join(refs, branchName);
        if (!tgtBranch.exists()) {
            System.out.println(
                    "A branch with that name does not exist.");
            System.exit(0);
        }
        if (tgtBranch.getPath().equals(Utils.readContentsAsString(head))) {
            System.out.println(
                    "Cannot remove the current branch.");
            System.exit(0);
        }
        tgtBranch.delete();
    }

    /**
     * Checks out all files from the commit denoted by the input
     * parameter and changes the head commit pointer to the given commit.
     * Clean the staging area for addition.
     * @param commitHash Hashcode of the commit to be checked out and
     *                   pointed to as the new head commit.
     */
    public void reset(String commitHash) {
        File commitFile = Utils.join(commits + "/" + commitHash);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        for (String file : Utils.plainFilenamesIn(cwd)) {
            if (!getHeadCommit().containsBlob(file)
                    && !stage.containsInAdd(file)) {
                System.out.println(
                        "There is an untracked file in the way;"
                                + " delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        checkoutCommit(commitHash);
        File currBranch = new File(Utils.readContentsAsString(head));
        Utils.writeContents(currBranch, commitHash);
    }

    /**
     * Merges the given branch to the current branch. If a merge
     * conflict occurs, the user will manually resolve it by editing
     * the files. In the case that two split points of equal distance
     * exist, the method chooses the first one found.
     * @param branch Name of the branch to merge with the current branch.
     */
    public void merge(String branch) {
        if (!initialErrorChecks(branch)) {
            System.exit(0);
        }
        Commit headComm = getHeadCommit();
        Commit givenComm = getGivenCommit(branch);
        if (!secondaryErrorChecks(headComm, givenComm)) {
            System.exit(0);
        }
        Commit splitComm = findSplitPoint(branch);
        if (!splitErrorChecks(branch, splitComm, headComm, givenComm)) {
            System.exit(0);
        }
        stage = Utils.readObject(index, StagingArea.class);
        for (Object key : givenComm.getBlobKeys()) {
            String name = (String) key;
            if (!splitComm.containsBlob(name)) {
                if (!headComm.containsBlob(name)) {
                    String blobHash = givenComm.getBlob(name);
                    overwriteFile(name, blobHash);
                    stage.addToStage(name, blobHash);
                    Utils.writeObject(index, stage);
                } else {
                    if (!givenComm.getBlob(name).equals(
                            headComm.getBlob(name))) {
                        createConflictFile(name, headComm, givenComm);
                    }
                }
            } else if (!splitComm.getBlob(name).equals(
                    givenComm.getBlob(name))) {
                if (splitComm.getBlob(name).equals(
                        headComm.getBlob(name))) {
                    String blobHash = givenComm.getBlob(name);
                    overwriteFile(name, blobHash);
                    stage.addToStage(name, blobHash);
                    Utils.writeObject(index, stage);
                } else if (!headComm.containsBlob(name)) {
                    createConflictFile(name, headComm, givenComm);
                } else if (!headComm.getBlob(name).equals(
                        splitComm.getBlob(name))) {
                    if (!headComm.getBlob(name).equals(
                            givenComm.getBlob(name))) {
                        createConflictFile(name, headComm, givenComm);
                    }
                }
            }
        }
        for (Object key : headComm.getBlobKeys()) {
            String name = (String) key;
            if (splitComm.containsBlob(name) && !givenComm.containsBlob(name)) {
                if (splitComm.getBlob(name).equals(headComm.getBlob(name))) {
                    rm(name);
                } else if (!splitComm.getBlob(name).equals(
                        headComm.getBlob(name))) {
                    createConflictFile(name, headComm, givenComm);
                }
            }
        }
        commitMerge(branch, givenComm);
    }
    private void commitMerge(String branch, Commit givenComm) {
        Utils.writeObject(index, stage);
        String currName = new File(
                Utils.readContentsAsString(head)).getName();
        commit("Merged " + branch + " into " + currName + ".", givenComm);
    }

    /**
     * Performs initial error checks for merging
     * before commits are retrieved.
     * @param branch Name of the branch to merge with the
     *               current branch.
     * @return whether all error checks pass.
     */
    private boolean initialErrorChecks(String branch) {
        if (!stage.isAddEmpty() || !stage.isRemoveEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        }
        File givenBranch = Utils.join(refs, branch);
        if (!givenBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        return true;
    }

    /**
     * Performs secondary error checks before the split point
     * is identified.
     * @param headComm Head commit of the currently active branch.
     * @param givenComm Head commit of the given branch to merge.
     * @return whether all secondary error checks pass.
     */
    private boolean secondaryErrorChecks(Commit headComm, Commit givenComm) {
        if (headComm.equals(givenComm)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }
        for (String name : Utils.plainFilenamesIn(cwd)) {
            if (!headComm.containsBlob(name)) {
                System.out.println(
                        "There is an untracked file in the way;"
                                + "delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    /**
     * Performs tertiary error checks after the split point is identified.
     * @param branch Name of the branch to merge with the current branch.
     * @param splitComm The latest common ancestor of the two branches.
     * @param headComm Head commit of the currently active branch.
     * @param givenComm Head commit of the given branch to merge.
     * @return Whether all tertiary error checks pass.
     */
    private boolean splitErrorChecks(
            String branch, Commit splitComm,
            Commit headComm, Commit givenComm) {
        if (splitComm.equals(givenComm)) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            return false;
        }
        if (splitComm.equals(headComm)) {
            checkout(branch, false);
            System.out.println("Current branch fast-forwarded.");
            return false;
        }
        return true;
    }

    /**
     * In the case of a merge conflict, create a file containing contents
     * from the corresponding files from both commits.
     * @param name Name of the conflicting file.
     * @param headComm Head commit of the currently active branch.
     * @param givenComm Head commit of the given branch to merge.
     */
    private void createConflictFile(
            String name, Commit headComm, Commit givenComm) {
        System.out.println("Encountered a merge conflict.");
        String newContent = "<<<<<<< HEAD\n";
        if (headComm.containsBlob(name)) {
            String currHash = headComm.getBlob(name);
            String curr = Utils.readContentsAsString(
                    Utils.join(blobs, currHash));
            newContent += curr;
        }
        newContent += "=======\n";
        if (givenComm.containsBlob(name)) {
            String givenHash = givenComm.getBlob(name);
            String given = Utils.readContentsAsString(
                    Utils.join(blobs, givenHash));
            newContent += given;
        }
        newContent += ">>>>>>>\n";
        File replace = Utils.join(cwd, name);
        Utils.writeContents(replace, newContent);
        String blobHash = Utils.sha1(newContent);
        File newBlob = Utils.join(blobs, blobHash);
        try {
            newBlob.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(newBlob, newContent);
        stage.addToStage(name, blobHash);
    }

    /**
     * Overwrites the file in the current working directory with
     * contents denoted by the input blob, creating a new file if
     * one of the matching name cannot be found.
     * @param fileName Name of the file to be overwritten.
     * @param blobHash Hashcode of the corresponding content blob.
     */
    private void overwriteFile(String fileName, String blobHash) {
        File replace = Utils.join(cwd, fileName);
        String blob = Utils.readContentsAsString(
                Utils.join(blobs, blobHash));
        Utils.writeContents(replace, blob);
    }

    /**
     * Finds the latest common ancestor between the current active
     * branch and the given branch to merge.
     * @param branchName Name of the merging branch.
     * @return the latest common ancestor commit between the
     * two branches.
     */
    private Commit findSplitPoint(String branchName) {
        Commit headComm = getHeadCommit();
        TreeMap<Commit, Integer> distanceMapCurr = new TreeMap<>();
        distanceMapCurr = toInitCurr(headComm, distanceMapCurr, 0);
        Commit givenComm = Utils.readObject(Utils.join(
                commits, Utils.readContentsAsString(
                        Utils.join(refs, branchName))), Commit.class);
        Commit splitPoint = toInitGiven(
                givenComm, distanceMapCurr, givenComm, Integer.MAX_VALUE);
        return splitPoint;
    }

    /**
     * Performs a traversal of the current active branch, mapping each
     * commit node with their respective distance from the head commit.
     * @param currCommit Head commit of the current active branch.
     * @param distances Mapping of commits and distances.
     * @param dis The current distance away from the head commit.
     * @return Mapping of the commits and their distances.
     */
    private TreeMap<Commit, Integer> toInitCurr(
            Commit currCommit, TreeMap<Commit, Integer> distances, int dis) {
        distances.put(currCommit, Integer.valueOf(dis));
        if (currCommit.getMessage().equals("initial commit")) {
            return distances;
        }
        dis++;
        Commit firstParent = Utils.readObject(
                Utils.join(commits, currCommit.getParent()), Commit.class);
        distances = toInitCurr(firstParent, distances, dis);
        if (currCommit.hasSecondParent()) {
            Commit otherParent = Utils.readObject(
                    Utils.join(commits, currCommit.getOtherParent()),
                    Commit.class);
            distances = toInitCurr(otherParent, distances, dis);
        }
        return distances;
    }

    /**
     * Performs a traversal through the given branch to merge, locating
     * the common ancestor that has the shortest distance from the head
     * commit of the current active branch.
     * @param comm The current commit node along the given branch.
     * @param distances Mapping of commit nodes and distancecs.
     * @param split The currently identified split point.
     * @param bestDis The shortest distance from head commit found so far.
     * @return the latest common ancestor of the two branches.
     */
    private Commit toInitGiven(
            Commit comm, TreeMap<Commit, Integer> distances,
            Commit split, int bestDis) {
        if (distances.containsKey(comm)) {
            if (distances.get(comm) < bestDis) {
                split = comm;
                bestDis = distances.get(comm);
            }
        }
        if (comm.getMessage().equals("initial commit")) {
            return split;
        }
        Commit firstParent = Utils.readObject(
                Utils.join(commits, comm.getParent()), Commit.class);
        Commit newSplit1 = toInitGiven(firstParent, distances, split, bestDis);
        if (distances.get(newSplit1) < bestDis) {
            split = newSplit1;
            bestDis = distances.get(newSplit1);
        }
        if (comm.hasSecondParent()) {
            Commit otherParent = Utils.readObject(
                    Utils.join(commits, comm.getOtherParent()),
                    Commit.class);
            Commit newSplit2 = toInitGiven(
                    otherParent, distances, split, bestDis);
            if (distances.get(newSplit2) < bestDis) {
                split = newSplit2;
            }
        }
        return split;
    }

    /**
     * Retrieves the head commit of the current active branch.
     * @return The head commit.
     */
    private Commit getHeadCommit() {
        String headPath = Utils.readContentsAsString(head);
        File commit = new File("./.gitlet/commits/"
                + Utils.readContentsAsString(new File(headPath)));
        return Utils.readObject(commit, Commit.class);
    }

    /**
     * Retrieves the head commit of the specified branch.
     * @param branch Name of the specified branch.
     * @return The head commit.
     */
    private Commit getGivenCommit(String branch) {
        return Utils.readObject(Utils.join(
                commits, Utils.readContentsAsString(
                        Utils.join(refs, branch))), Commit.class);
    }

    /**
     * Saves a new remote repository under the given name.
     * @param name Name of the new remote repository.
     * @param directory Path to the remote repository.
     */
    public void addRemote(String name, String directory) {

        if (Utils.plainFilenamesIn(remotes).contains(name)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        } else {
            String sep = File.separator;
            directory.replace("/", sep);
            directory.replace("\\", sep);
            File newRemote = Utils.join(remotes, name);
            Utils.writeContents(newRemote, directory);
        }
    }

    /**
     * Removes the remote repository denoted by the input name.
     * @param name Name of the remote repository to remove.
     */
    public void rmRemote(String name) {

        if (!Utils.plainFilenamesIn(remotes).contains(name)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        } else {
            File remRemote = Utils.join(remotes, name);
            remRemote.delete();
        }
    }

    /**
     * Inserts or fast-forwards the given remote branch with current active
     * branch's commits.
     * @param remoteName Name of the remote repository.
     * @param tgtBranch Name of the branch to append.
     */
    public void push(String remoteName, String tgtBranch) {
        if (!Utils.plainFilenamesIn(remotes).contains(remoteName)) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remote = new File(
                Utils.readContentsAsString(Utils.join(remotes, remoteName)));
        Commit headComm = getHeadCommit();
        File remoteBranch = Utils.join(remote, "refs/" + tgtBranch);
        if (!remoteBranch.exists()) {
            try {
                remoteBranch.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(remoteBranch, headComm.getHashName());
            return;
        }
        Commit remoteComm = Utils.readObject(Utils.join(
                remote, ("refs/" + tgtBranch)), Commit.class);
        ArrayList<Commit> appendList = new ArrayList<>();
        appendList = findPushList(headComm, remoteComm, appendList);
        if (appendList == null) {
            System.out.println(
                    "Please pull down remote changes before pushing.");
            System.exit(0);
        }
        /*appendList.remove(appendList.size() - 1);
        File remoteComms = Utils.join(remote, "commits");
        for (Commit comm : appendList) {
            File tgtComm = Utils.join(remoteComms, comm.getHashName());
            try {
                tgtComm.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(tgtComm, comm);
        }*/
        Utils.writeContents(Utils.join(
                remote, "refs/" + tgtBranch), headComm.getHashName());
    }

    /**
     * Recursively compiles a list of commits in the current branch that is missing
     * in the remote branch.
     * @param head Head commit of the current branch.
     * @param remote Head commit of the remote branch.
     * @param append The existing list of absent commits.
     * @return The list of absent commits. Returns null if the remote commit is not
     * in the current history.
     */
    private ArrayList<Commit> findPushList(
            Commit head, Commit remote,
            ArrayList<Commit> append) {
        if (head == null) {
            return null;
        }
        append.add(head);
        if (head.equals(remote)) {
            return append;
        }
        Commit parent = Utils.readObject(Utils.join(
                commits, head.getParent()), Commit.class);
        append = findPushList(parent, remote, append);
        return append;
    }

    /**
     * Fetch the remote branch denoted by the input, copying all
     * associated commits and blobs to the local repository.
     * @param remote Name of the remote repository.
     * @param branch Name of the remote branch to fetch.
     */
    public void fetch(String remote, String branch) {
        if (Utils.plainFilenamesIn(remotes).contains(remote)) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        File remoteBranch = Utils.join(Utils.readContentsAsString(
                Utils.join(remotes, remote)), "refs/" + branch);
        if (!remoteBranch.exists()) {
            System.out.println("That remote does not have that branch.");
        }
        String commHash = Utils.readContentsAsString(remoteBranch);
        Commit remoteComm = Utils.readObject(Utils.join(Utils.readContentsAsString(
                        Utils.join(remotes, remote)),
                "commits/" + commHash), Commit.class);
        copyCommsAndBlobs(remoteComm, remote);
        File fetchedBranch = Utils.join(refs, remote + "/" + branch);
        if (!fetchedBranch.exists()) {
            try {
                fetchedBranch.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Utils.writeContents(fetchedBranch, commHash);
    }

    /**
     * Copies all foreign commits and blobs from the fetched branch.
     * @param remoteComm Head commit of the remote branch.
     * @param remote Name of the remote repository.
     */
    private void copyCommsAndBlobs(Commit remoteComm, String remote) {
        Commit headComm = getHeadCommit();
        while (remoteComm != null) {
            if (headComm.equals(remoteComm)) {
                return;
            }
            File copyComm = Utils.join(commits, remoteComm.getHashName());
            try {
                copyComm.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(copyComm, remoteComm);
        }
        for (Object key : remoteComm.getBlobKeys()) {
            String blobHash = remoteComm.getBlob((String) key);
            String content = Utils.readContentsAsString(
                    Utils.join(Utils.readContentsAsString(
                            Utils.join(remotes, remote)),
                            "blobs/" + blobHash));
            File copyBlob = Utils.join(blobs, blobHash);
            try {
                copyBlob.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeContents(copyBlob, content);
            Commit parent = Utils.readObject(Utils.join(
                    Utils.readContentsAsString(Utils.join(remotes, remote)),
                            "commits/" + remoteComm.getParent()),
                    Commit.class);
            remoteComm = parent;
        }
    }

    /**
     * Fetches and merges the remote branch with the current branch.
     * @param remote Name of the remote repository.
     * @param branch Name of the remote branch.
     */
    public void pull(String remote, String branch) {
        fetch(remote, branch);
        merge (remote + "/" + branch);
    }
}
