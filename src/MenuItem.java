public class MenuItem {

    private final String text;
    private final Runnable action;
    private final boolean pauseAfter;

    public MenuItem(String text, Runnable action, boolean pauseAfter) {
        this.text = text;
        this.action = action;
        this.pauseAfter = pauseAfter;
    }

    public String text() {
        return text;
    }

    public void run() {
        action.run();
    }

    public boolean pauseAfter() {
        return pauseAfter;
    }
}
