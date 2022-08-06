package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Represents a collection of the files to be staged for
 * addition and removal.
 * @author Bradley Tian
 */
public class StagingArea implements Serializable {

    /**
     * Mapping of files staged for addition.
     */
    private TreeMap<String, String> addition;

    /**
     * List of files staged for removal.
     */
    private ArrayList<String> removal;

    /**
     * Constructor of a new staging area. Initializes
     * with empty addition / removal stages.
     */
    public StagingArea() {
        addition = new TreeMap<>();
        removal = new ArrayList<>();
    }

    /**
     * Adds a file to the addition stage.
     * @param name Name of the file to add.
     * @param file Hashcode of content blob for the file.
     */
    public void addToStage(String name, String file) {
        addition.put(name, file);
    }

    /**
     * Retrieves the specified file from the addition stage.
     * @param name Name of the file to retrieve.
     * @return Hashcode of the content blob for that file.
     */
    public String getFromAddition(String name) {
        return addition.get(name);
    }

    /**
     * Stages a file for removal.
     * @param file Name of the file to remove.
     */
    public void addToRemove(String file) {
        removal.add(file);
    }

    /**
     * Empties both the addition and removal stage.
     */
    public void clearStage() {
        addition = new TreeMap<>();
        removal = new ArrayList<>();
    }

    /**
     * Retrieves the catalog of all files staged for addition.
     * @return The catalog of all files staged for addition.
     */
    public Object[] additionKeySet() {
        return addition.keySet().toArray();
    }

    /**
     * Retrieves the catalog of all files staged for removal.
     * @return The catalog of all files staged for removal.
     */
    public ArrayList<String> removalKeySet() {
        return removal;
    }

    /**
     * Checks if the specified file is staged for addition.
     * @param fileName Name of the file in question.
     * @return Whether the file is staged for addition.
     */
    public boolean containsInAdd(String fileName) {
        return addition.containsKey(fileName);
    }

    /**
     * Checks if the specified file is staged for removal.
     * @param fileName Name of the file in question.
     * @return Whether the file is staged for removal.
     */
    public boolean containsInRemove(String fileName) {
        return removal.contains(fileName);
    }

    /**
     * Removes the specified file from the addition stage.
     * @param fileName Name of the file in question.
     */
    public void removeFromAddition(String fileName) {
        addition.remove(fileName);
    }

    /**
     * Removes the specified file from the removal stage.
     * @param fileName Name of the file in question.
     */
    public void removeFromRemove(String fileName) {
        removal.remove(fileName);
    }

    /**
     * Checks if the addition stage is empty.
     * @return Whether the addition stage is empty.
     */
    public boolean isAddEmpty() {
        return addition.isEmpty();
    }

    /**
     * Checks if the removal stage is empty.
     * @return Whether the removal stage is empty.
     */
    public boolean isRemoveEmpty() {
        return removal.isEmpty();
    }
}
