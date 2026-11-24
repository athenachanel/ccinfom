import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public final class RecordPanel extends JPanel {
    private final AppService service;
    private final AppService.Kind kind;
    private final JTable table = Ui.table();
    private final JComboBox<String> filter = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private String appliedFilter;

    public RecordPanel(AppService service, AppService.Kind kind) {
        super(new BorderLayout(8, 8));
        this.service = service;
        this.kind = kind;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel(Ui.label(kind.filter) + ":"));
        top.add(filter);
        Ui.button(top, "Filter", this, () -> {
            appliedFilter = (String) filter.getSelectedItem(); refresh();
        });
        Ui.button(top, "View All", this, () -> { appliedFilter = null; refresh(); });
        Ui.button(top, "Add", this, () -> edit(null));
        Ui.button(top, "View", this, this::view);
        Ui.button(top, "Edit", this, () -> edit(Ui.selectedId(table)));
        Ui.button(top, "Delete", this, this::delete);
        Ui.button(top, "Refresh", this, this::refresh);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public void onShown() { Ui.run(this, this::refresh); }

    private void refresh() throws Exception {
        Object old = filter.getSelectedItem();
        filter.removeAllItems();
        for (String value : service.filterValues(kind)) filter.addItem(value);
        if (old != null) filter.setSelectedItem(old);
        if (filter.getSelectedIndex() < 0 && filter.getItemCount() > 0) filter.setSelectedIndex(0);
        DataAccess.Rows rows = service.records(kind, appliedFilter);
        Ui.load(table, rows);
        status.setText(rows.values().size() + " record(s)" +
                (appliedFilter == null ? " — All" : " — Filter: " + appliedFilter));
    }

    private void view() throws Exception {
        Object[] row = service.record(kind, Ui.selectedId(table));
        String[] columns = new String[kind.fields.length + 1];
        columns[0] = kind.id;
        System.arraycopy(kind.fields, 0, columns, 1, kind.fields.length);
        Ui.view(this, "Record Details", Ui.details(columns, row));
    }

    private void edit(Integer id) throws Exception {
        Object[] previous = id == null ? null : service.record(kind, id);
        JPanel form = Ui.form();
        JComponent[] inputs = new JComponent[kind.fields.length];
        for (int i = 0; i < inputs.length; i++) {
            String name = kind.fields[i];
            String value = previous == null ? "" : Objects.toString(previous[i + 1], "");
            String[] options = name.endsWith("_status") ? AppService.STATUSES :
                    name.equals("supplier_type") ? AppService.SUPPLIER_TYPES : null;
            if (options == null) inputs[i] = new JTextField(value, 22);
            else {
                JComboBox<String> box = new JComboBox<>(options);
                if (previous != null) box.setSelectedItem(value);
                inputs[i] = box;
            }
            Ui.field(form, Ui.label(name), inputs[i]);
        }
        while (Ui.confirm(this, id == null ? "Add Record" : "Edit Record", form)) {
            String[] values = new String[inputs.length];
            for (int i = 0; i < values.length; i++)
                values[i] = inputs[i] instanceof JTextField text ? text.getText() :
                        Objects.toString(((JComboBox<?>) inputs[i]).getSelectedItem(), "");
            try { service.saveRecord(kind, id, values); }
            catch (Exception e) { Ui.error(this, e); continue; }
            appliedFilter = null;
            refresh();
            return;
        }
    }

    private void delete() throws Exception {
        int id = Ui.selectedId(table);
        if (Ui.confirm(this, "Delete Record", "Delete record #" + id + "?")) {
            service.deleteRecord(kind, id);
            refresh();
        }
    }
}
