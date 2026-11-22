import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public final class VenuePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String VENUE_QUERY =
            "SELECT venue_id, venue_name, street_address, city, venue_capacity "
                    + "FROM venue ORDER BY venue_id";

    private final JTable table = new JTable(createTableModel());
    private final JButton refreshButton = new JButton("Refresh");
    private final JLabel status = new JLabel();

    public VenuePanel() {
        super(new BorderLayout(8, 8));
        table.setFillsViewportHeight(true);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(refreshButton);
        controls.add(status);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshButton.addActionListener(event -> loadVenues());
        loadVenues();
    }

    private static DefaultTableModel createTableModel() {
        String[] columns = {"ID", "Venue Name", "Address", "City", "Capacity"};
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void loadVenues() {
        refreshButton.setEnabled(false);
        status.setToolTipText(null);
        status.setText("Loading venues...");

        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() throws SQLException {
                DefaultTableModel model = createTableModel();
                try (Connection connection = Database.getConnection();
                     PreparedStatement statement = connection.prepareStatement(VENUE_QUERY);
                     ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        model.addRow(new Object[]{
                                rows.getInt("venue_id"),
                                rows.getString("venue_name"),
                                rows.getString("street_address"),
                                rows.getString("city"),
                                rows.getInt("venue_capacity")
                        });
                    }
                }
                return model;
            }

            @Override
            protected void done() {
                try {
                    DefaultTableModel model = get();
                    table.setModel(model);
                    status.setText(model.getRowCount() + " venue(s) loaded.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Venue loading interrupted.");
                } catch (ExecutionException e) {
                    String message = e.getCause().getMessage();
                    status.setText("Could not load venues: " + message);
                    status.setToolTipText(message);
                } finally {
                    refreshButton.setEnabled(true);
                }
            }
        }.execute();
    }
}