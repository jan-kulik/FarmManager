// Einzelner Menueeintrag mit einem Anzeigetext und einer Aktion.
public class MenuItem {

    private final String text;
    private final Runnable action;

    public MenuItem(String text, Runnable action) {
        this.text = text;
        this.action = action;
    }

    public String text() {
        return text;
    }

    public void run() {
        action.run();
    }
}
