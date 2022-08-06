# Gitlet Design Document
Author: Bradley Tian

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

### Repository
Represents the working gitlet repository; primary class carrying out most operations of Gitlet.

#### Fields
TreeMap<String, String> branches: a map of head pointers to current commits (denoted by their names).   
StagingArea stage: the staging area for addition and removal operations.   
String currentBranch: a pointer to the name of the current working branch.   

### StagingArea
TreeMap<String, String> addStage: a map of any blobs (denoted by their names) that have been staged for adding.   
ArrayList<String> removeStage: a list of any files that have been staged for removal.   

### Commit
Represents an entire commit. A combination of log messages, metadata, and references to trees and parent commits. 

#### Fields
1. String log: the commit's log message.
2. String timestamp: the commit's time stamp.
3. TreeMap<String, String> blobs: a mapping of names to blob references.
4. String parent: name of the parent commit.
5. String otherParent: name of the other parent commit, if any.

### Blobs
Represents the contents of a single file.

#### Fields
String content: a string representation containing the blob's contents.

## 2. Algorithms

### Repository Class
1. init(): Initializes a gitlet repository by creating an empty commit with the log message "initial commit
 and a single master branch. The timestamp of this commit is set to 1/1/1970, 00:00am. If an initialized system already exists, it should print out "A Gitlet version-control system already exists in the current directory." and abort. 
2. add(File input): Check if the current working file is the same as any files existing in the current working directory of the head commit. If there is a match, refrain from adding the file to stage / remove the file from stage if it is already staged. If there is no match, add the current working file to the staging area.
3. commit(String message): Create a new commit with the log message denoted by the input parameters. By default, the new commit's content tree should refer back to its parent commits for all files that remain unchanged. For those files changed, added to the staging and removal areas, change create, or remove corresponding nodes in the commit's blog mapping. 
4. rm(File input): If the input file is in the staging area, remove it. Additionally, add the input file to the removal staging area. If the input file is not found within the head commit's contents tree, print error message.
5. log(): Recursively print out the commit id, timestamp, and commit message of each commits in the historical sequence beginning with the head commit. Ignore any second parents.
6. global-log(): Print out the aforementioned id, timestamp, and message of every commit made in history. 
7. find(String message): Prints out the ids of all commits that have the matching commit message. 
8. status(): Display all branches that currently exist, indicating the head branch head with "*". Also displays all files added to addition and removal staging areas.
9. checkout(File input): Overwrites the file in the working directory with the matching file within the contents tree of the head commit. If the given file does not exist in the contents tree, throw error message. 
10. checkout(String commitId, File input): Overwrites the file in the working directory with the matching file from the commit with the matching commit ID. If no commits with such an ID exists, throw error message.
11. checkout(String branch): Find the head of the branch with the given name (referring the head branch pointer) and overwrite the working directory with all files from that head commit's content tree. If the branch in question is the current working branch, throw error message. If there is any untracked files in the current directory, ask the user to stage or remove the file first.
12. branch(String name): Create a new branch with the name denoted by the input parameter. The current commit head (acquired by searching through the heads map with the current branch pointer) is assigned to the newly created branch. No real changes are applied unless committing is performed with the new branch. 
13. rm-branch(String name): Remove the branch with the current name. The branch cannot be the current working branch.
14. reset(String commitId): Checks out all files from the commit denoted by the input parameter and changes the head commit pointer to the given commit. Clean the staging area for addition.
15. merge(branch name): merges the given branch to the current branch. If a merge conflict occurs, the user will manually resolve it by editing the files. In the case that two split points exist, choose the closest one. 
## 3. Persistence
`java gitlet.Main add fileName`
1. A blob object will be created with the contents of the file whose name is denoted by the input.
2. Get the hash value of the blob and use it as the name of the file within .gitlet that will store the serialized content.
3. If no identical blob with the same hash value exists, create a subdirectory within "objects" using the first two letters of the hash value and create a new file. 
4. Write the serialized content into the file and name it with the has value of the contents starting from the third character.
5. Record this addition into the staging area and serialize the StagingArea object into the index file within .gitlet.

`java gitlet.Main commit message`
1. Create a new commit object with the input message. 
2. Update the commit with all stages denoted in the "index" file.
3. If no identical file with the same hash value exists, create a subdirectory within "objects" using the first two letters of the hash value and create a new file.
4. Write the serialized content into the file and name it with the hash value of the contents starting from the third character.
5. Update "heads" to contain the new commit hash.

`java gitlet.Main checkout branchName`
1. Update what HEAD points to.

`java gitlet.Main branch branchName`
1. Create a new file in "heads" with the name denoted by the input.
2. The content will be the hashcode of the commit that HEAD is pointing to.

`java gitlet.Main rm-branch branchName`
1. Delete the file in "heads" with the name denoted by the input.

`java gitlet.Main reset id`
1. Change HEAD to the commit hash denoted by the input.
2. Change the head commit tracked within the Repository class. 

`java gitlet.Main merge branch`
1. Create a new commit.
2. Change content of files in "head" with names of the current branch to point to the new commit.

## 4. Design Diagram
![](./Gitlet%20Design%20Diagram.jpg)
