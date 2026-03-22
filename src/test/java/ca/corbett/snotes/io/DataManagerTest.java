package ca.corbett.snotes.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

class DataManagerTest {

    static DataManager dataManager;

    @TempDir
    static File tempDir;

    @BeforeAll
    public static void setup() {
        // Create our data manager in our tempDir, and NOT in the actual dir:
        dataManager = new DataManager(tempDir);
    }

    // TODO add tests for load and save methods
    // TODO we need to generate some test files in our tempDir - perhaps our setup method can generate some?

}