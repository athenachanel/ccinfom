import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        JFrame frame = new JFrame("Concert Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Venues", new VenuePanel());
        tabs.addTab("Artist Performance", new ArtistPerformancePanel());

        frame.setContentPane(tabs);
        frame.setSize(900, 550);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}