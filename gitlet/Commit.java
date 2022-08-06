package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;

/**
 * Represents an entire commit. A combination of log messages,
 * timestamps, other metadata, content trees, and parent commits.
 *
 * @author Bradley Tian
 */

public class Commit implements Serializable, Comparable<Commit> {

    /**
     * log message of the commit.
     */
    private String message;

    /**
     * timestamp of the message, specified to seconds.
     */
    private String timestamp;

    /**
     * mapping of tracked files and their corresponding contents.
     */
    private TreeMap<String, String> blobs;

    /**
     * the first parent of the commit.
     */
    private String parent;

    /**
     * the second parent, if any, of the commit.
     */
    private String otherParent;

    /**
     * the hashcode of the commit, generated by the SHA-1 algorithm.
     */
    private String hashName;

    /**
     * the universal ID of the commit; randomly generated.
     */
    private String uid;

    /**
     * Constructor of a new commit. Initializes metadata.
     *
     * @param logMessage the log message of the commit.
     * @param newParent  the first parent of the commit.
     */
    @SuppressWarnings({"deprecation"})
    public Commit(String logMessage, String newParent) {
        this.message = logMessage;
        this.parent = newParent;
        this.otherParent = null;
        SimpleDateFormat formatter
                = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy");
        if (this.parent == null) {
            Date date;
            date = new Date(0, 0, 1, 0, 0);
            this.timestamp = formatter.format(date);
        } else {
            Date date = new Date();
            this.timestamp = formatter.format(date);
        }
        this.blobs = new TreeMap<String, String>();
        Random rm = new Random();
        this.uid = Integer.toString(rm.nextInt());
        Object[] hashSource = {
            this.message, timestamp, uid, blobs, this.parent};
        this.hashName = Utils.sha1(Utils.serialize(hashSource));
    }

    /**
     * Alternative version of the commit constructor that considers a
     * second parent.
     *
     * @param logMessage the log message of the commit.
     * @param newParent the first parent of the commit.
     * @param secondParent the second parent of the commit.
     */

    public Commit(String logMessage, String newParent, String secondParent) {
        this(logMessage, newParent);
        this.otherParent = secondParent;
    }

    /**
     * Retrieves the log message of the commit.
     *
     * @return the log message.
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Sets the log message of the commit.
     *
     * @param logMessage the log message.
     */
    public void setMessage(String logMessage) {
        this.message = logMessage;
    }

    /**
     * Retrieves the first parent of the commit.
     *
     * @return Hashcode of the first parent.
     */
    public String getParent() {
        return this.parent;
    }

    /**
     * Retrieves the second parent of the commit.
     *
     * @return Hashcode of the second parent.
     */
    public String getOtherParent() {
        return this.otherParent;
    }

    /**
     * Retrieves the hashcode of the commit.
     *
     * @return Hashcode of the commit.
     */
    public String getHashName() {
        return this.hashName;
    }

    /**
     * Retrieves the name of the corresponding content blob from
     * the commit's content tree.
     *
     * @param fileName the name of the file to retrieve the content blob for.
     * @return the hashcode of the corresponding content blob.
     */
    public String getBlob(String fileName) {
        if (!blobs.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        return blobs.get(fileName);
    }

    /**
     * Creates a new commit based on the content of the current commit. The
     * new commit will to point to the current commit as its first parent.
     *
     * @return the newly generated commit.
     */
    public Commit clone() {
        Commit clone = new Commit(this.message, this.hashName);
        for (String file : this.blobs.keySet()) {
            clone.blobs.put(file, this.blobs.get(file));
        }
        return clone;
    }

    /**
     * Retrieves the timestamp of the commit.
     *
     * @return A string representation of the timestamp.
     */
    public String getTimeStamp() {
        return this.timestamp;
    }

    /**
     * Add a new file and its corresponding content blob into the
     * content tree of the commit.
     *
     * @param fileName the name of the new file to track.
     * @param blob     the content blob of the file.
     */
    public void setBlob(String fileName, String blob) {
        this.blobs.put(fileName, blob);
    }

    /**
     * Checks if the commit tracks the given file, denoted by the
     * input parameter.
     *
     * @param fileName the name of the file in question.
     * @return whether the file is tracked by the commit.
     */
    public boolean containsBlob(String fileName) {
        return blobs.containsKey(fileName);
    }

    /**
     * Untracks the file from the content tree of the commit.
     *
     * @param fileName name of the file to untrack.
     */
    public void removeBlob(String fileName) {
        blobs.remove(fileName);
    }

    /**
     * Checks if the commit is equal (has the same hashcode) as the
     * commit given by the input.
     *
     * @param comm the commit to compare with self.
     * @return whether the two commits are the same.
     */
    public boolean equals(Commit comm) {
        return this.hashName.equals(comm.getHashName());
    }

    /**
     * Retrieves a catalog of files tracked by the commit.
     *
     * @return the tracked files catalog.
     */
    public Object[] getBlobKeys() {
        return blobs.keySet().toArray();
    }

    /**
     * Checks if the commit has a second parent.
     *
     * @return whether the commit has a second parent.
     */
    public boolean hasSecondParent() {
        return this.otherParent != null;
    }

    /**
     * Compare self to the commit given by input by the lexicographic
     * order of their hashcodes.
     *
     * @param other the commit to compare with self.
     * @return the order of priority between the two commits.
     */
    public int compareTo(Commit other) {
        return this.hashName.compareTo(other.hashName);
    }

    /**
     * Sets the commit's first parent.
     * @param firstParent Hashcode of the new first parent.
     */
    public void setParent(String firstParent) {
        this.parent = firstParent;
    }
    /**
     * Sets the hashcode of the commit's second parent to that denoted
     * by the input parameter.
     *
     * @param secondParent the hashcode of the second parent.
     */
    public void setOtherParent(String secondParent) {
        this.otherParent = secondParent;
    }
}