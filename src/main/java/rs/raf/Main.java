package rs.raf;

import rs.raf.core.App;

public class Main {
    public static void main(String[] args) {
        App app = new App();
        app.readConfig();
        app.handleCommands();
    }
}
