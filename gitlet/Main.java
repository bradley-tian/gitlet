package gitlet;

import java.io.File;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Bradley Tian
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS = <COMMAND> <OPERAND> ...
     */
    public static void main(String... args) {
        File repository = new File("./.gitlet");
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else if (args[0].equals("init")) {
            Repository.init();
        } else {
            if (!repository.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
            } else {
                Repository repo = new Repository();
                repo.parseCommands(args);
            }
        }
        System.exit(0);
    }
}
