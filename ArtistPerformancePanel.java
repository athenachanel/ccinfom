
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public final class ArtistPerformancePanel extends JPanel {

    private static final String REPORT_QUERY =
        "SELECT a.artist_id, a.artist_name, " +
        "COUNT(c.concert_id) AS total_concerts " +
        "FROM artist a LEFT JOIN concert c " +
        "ON a.artist_id = c.artist_id " +
        "AND c.concert_date >= ? " +
        "AND c.concert_date < ? " +
        "GROUP BY a.artist_id, a.artist_name " +
        "ORDER BY total_concerts DESC, a.artist_name ASC";

    private final JComboBox<String> monthBox =
        new JComboBox<>(new String[]{
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        });

    private final JComboBox<Integer> yearBox =
        new JComboBox<>();

    private final JButton generateButton =
        new JButton("Generate Report");

    private final JLabel status =
        new JLabel("Select a month and year.");

    private final JTable table =
        new JTable(newReportModel());

    public ArtistPerformancePanel() {
        super(new BorderLayout(8, 8));

        for (int year = 2020; year <= 2040; year++) {
            yearBox.addItem(year);
        }

        monthBox.setSelectedIndex(10);
        yearBox.setSelectedItem(2026);

        JPanel controls =
            new JPanel(new FlowLayout(FlowLayout.LEFT));

        controls.add(new JLabel("Month:"));
        controls.add(monthBox);
        controls.add(new JLabel("Year:"));
        controls.add(yearBox);
        controls.add(generateButton);
        controls.add(status);

        table.setFillsViewportHeight(true);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        generateButton.addActionListener(
            event -> generateReport()
        );
    }

    private static DefaultTableModel newReportModel() {
        return new DefaultTableModel(
            new String[]{
                "Artist ID",
                "Artist Name",
                "Total Concerts"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(
                int row, int column
            ) {
                return false;
            }
        };
    }

    private void generateReport() {
        int month = monthBox.getSelectedIndex() + 1;
        int year = (Integer) yearBox.getSelectedItem();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);

        generateButton.setEnabled(false);
        status.setText("Generating report...");
        status.setToolTipText(null);

        new SwingWorker<DefaultTableModel, Void>() {

            @Override
            protected DefaultTableModel doInBackground()
                    throws SQLException {

                DefaultTableModel results = newReportModel();

                try (Connection connection =
                         Database.getConnection();
                     PreparedStatement statement =
                         connection.prepareStatement(REPORT_QUERY)) {

                    statement.setDate(1, Date.valueOf(start));
                    statement.setDate(2, Date.valueOf(end));

                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            results.addRow(new Object[]{
                                rows.getInt("artist_id"),
                                rows.getString("artist_name"),
                                rows.getInt("total_concerts")
                            });
                        }
                    }
                }

                return results;
            }

            @Override
            protected void done() {
                try {
                    DefaultTableModel results = get();
                    table.setModel(results);

                    status.setText(
                        results.getRowCount() +
                        " artist(s) shown."
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Report interrupted.");

                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();

                    String message = cause == null
                        ? e.getMessage()
                        : cause.getMessage();

                    status.setText("Report failed. Hover for details.");
                    status.setToolTipText(message);

                    javax.swing.JOptionPane.showMessageDialog(
                        ArtistPerformancePanel.this,
                        message,
                        "Database Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );

                } finally {
                    generateButton.setEnabled(true);
                }
            }

        }.execute();
    }
}
