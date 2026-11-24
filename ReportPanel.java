import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;

public final class ReportPanel extends JPanel {
    private final AppService service;
    private final AppService.Report report;
    private final JTable table = Ui.table();
    private final JComboBox<String> genre = new JComboBox<>();
    private final JLabel status = new JLabel("Choose a period and generate the report.");
    private final Ui.Period period;

    public ReportPanel(AppService service, AppService.Report report) {
        super(new BorderLayout(8, 8));
        this.service = service;
        this.report = report;
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        period = new Ui.Period(controls);
        if (report == AppService.Report.GENRE) {
            controls.add(new JLabel("Genre:")); controls.add(genre);
            Ui.button(controls, "Refresh Genres", this, this::loadGenres);
        }
        Ui.button(controls, "Generate Report", this, this::generate);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public void onShown() {
        if (report == AppService.Report.GENRE) Ui.run(this, this::loadGenres);
    }

    private void loadGenres() throws Exception {
        Object previous = genre.getSelectedItem();
        genre.removeAllItems();
        for (String value : service.filterValues(AppService.Kind.ARTIST)) genre.addItem(value);
        if (previous != null) genre.setSelectedItem(previous);
        if (genre.getSelectedIndex() < 0 && genre.getItemCount() > 0) genre.setSelectedIndex(0);
    }

    private void generate() throws Exception {
        YearMonth month = period.value();
        String selectedGenre = (String) genre.getSelectedItem();
        DataAccess.Rows rows = service.report(report, month, selectedGenre);
        Ui.load(table, rows);
        status.setText("Report for " + month + (report == AppService.Report.GENRE ? " — " + selectedGenre : "")
                + " — " + rows.values().size() + " row(s)");
    }
}
