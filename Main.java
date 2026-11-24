import javax.swing.*;
import java.awt.*;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createWindow);
    }

    private static void createWindow() {
        AppService service = new AppService();
        JFrame frame = new JFrame("Concert Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Artists", new RecordPanel(service, AppService.Kind.ARTIST));
        tabs.addTab("Venues", new RecordPanel(service, AppService.Kind.VENUE));
        tabs.addTab("Suppliers", new RecordPanel(service, AppService.Kind.SUPPLIER));
        tabs.addTab("Concerts", new ConcertPanel(service));
        tabs.addTab("Artist Performance", new ReportPanel(service, AppService.Report.ARTIST));
        tabs.addTab("Venue Utilization", new ReportPanel(service, AppService.Report.VENUE));
        tabs.addTab("Genre Distribution", new ReportPanel(service, AppService.Report.GENRE));
        tabs.addTab("Ticket Sales", new ReportPanel(service, AppService.Report.TICKET));
        tabs.addChangeListener(e -> refresh(tabs.getSelectedComponent()));
        frame.setContentPane(tabs);
        frame.setSize(1220, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        refresh(tabs.getSelectedComponent());
    }

private static void refresh(Component panel) {
        if (panel instanceof RecordPanel p) p.onShown();
        else if (panel instanceof ConcertPanel p) p.onShown();
        else if (panel instanceof ReportPanel p) p.onShown();
    }
}
