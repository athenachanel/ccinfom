import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.Objects;

public final class Ui {
    private Ui() {}

    @FunctionalInterface
    public interface Action { void run() throws Exception; }

    public static void run(Component parent, Action action) {
        try { action.run(); }
        catch (Exception e) { error(parent, e); }
    }

    public static void error(Component parent, Exception e) {
        String message = e.getMessage();
        if (e instanceof SQLException sql && sql.getErrorCode() == 1451)
            message = "Cannot delete this record because other records reference it.";
        JOptionPane.showMessageDialog(parent, Objects.toString(message, "Operation failed."),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static JButton button(JPanel bar, String label, Component parent, Action action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> run(parent, action));
        bar.add(button);
        return button;
    }

    public static JTable table() {
        JTable table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        return table;
    }

    public static void load(JTable table, DataAccess.Rows rows) {
        String[] labels = rows.columns().clone();
        for (int i = 0; i < labels.length; i++) labels[i] = label(labels[i]);
        table.setModel(new DefaultTableModel(rows.values().toArray(Object[][]::new), labels) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                for (int i = 0; i < getRowCount(); i++) {
                    Object value = getValueAt(i, col);
                    if (value != null) return value.getClass();
                }
                return Object.class;
            }
        });
        for (int i = 0; i < labels.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(Math.max(120, labels[i].length()*9));
    }

    public static int selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a record first.");
        return ((Number) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0)).intValue();
    }

    public static String label(String name) {
        StringBuilder result = new StringBuilder();
        for (String word : name.split("_")) {
            if (result.length() > 0) result.append(' ');
            result.append(word.equals("id") ? "ID" :
                    Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }

    public static boolean confirm(Component parent, String title, Object content) {
        return JOptionPane.showConfirmDialog(parent, content, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static JPanel form() { return new JPanel(new GridBagLayout()); }

    public static void field(JPanel form, String label, Component input) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = form.getComponentCount() / 2;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 4, 3, 8);
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, c);
    }

    public static String details(String[] columns, Object[] row) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < columns.length; i++)
            result.append(label(columns[i])).append(": ").append(Objects.toString(row[i], ""))
                    .append('\n');
        return result.toString();
    }

    public static void view(Component parent, String title, String content) {
        JTextArea text = new JTextArea(content, 16, 44);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        JOptionPane.showMessageDialog(parent, new JScrollPane(text), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static final class Period {
        private final JComboBox<String> month = new JComboBox<>(new String[]{
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"});
        private final JSpinner year = new JSpinner(new SpinnerNumberModel(
                YearMonth.now().getYear(), 1900, 2100, 1));
        public Period(JPanel panel) {
            month.setSelectedIndex(YearMonth.now().getMonthValue() - 1);
            year.setEditor(new JSpinner.NumberEditor(year, "0"));
            panel.add(new JLabel("Month:")); panel.add(month);
            panel.add(new JLabel("Year:")); panel.add(year);
        }
        public YearMonth value() throws java.text.ParseException {
            year.commitEdit();
            return YearMonth.of((Integer) year.getValue(), month.getSelectedIndex() + 1);
