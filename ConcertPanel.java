import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.List;

public final class ConcertPanel extends JPanel {
    private final AppService service;
    private final JTable table = Ui.table();
    private final Ui.Period period;
    private final JLabel status = new JLabel(" ");
    private YearMonth appliedMonth;

    public ConcertPanel(AppService service) {
        super(new BorderLayout(8, 8));
        this.service = service;
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        period = new Ui.Period(controls);
        Ui.button(controls, "Filter", this, () -> { appliedMonth = period.value(); refresh(); });
        Ui.button(controls, "View All", this, () -> { appliedMonth = null; refresh(); });
        Ui.button(controls, "Add", this, () -> edit(null));
        Ui.button(controls, "View", this, this::view);
        Ui.button(controls, "Edit", this, () -> edit(Ui.selectedId(table)));
        Ui.button(controls, "Delete", this, this::delete);
        Ui.button(controls, "Refresh", this, this::refresh);
        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public void onShown() { Ui.run(this, this::refresh); }

    private void refresh() throws Exception {
        DataAccess.Rows rows = service.concerts(appliedMonth);
        Ui.load(table, rows);
        status.setText(rows.values().size() + " concert(s) — " +
                (appliedMonth == null ? "All" : appliedMonth));
    }

    private void view() throws Exception {
        int id = Ui.selectedId(table);
        DataAccess.Rows row = service.concertDetails(id);
        if (row.values().isEmpty()) throw new IllegalArgumentException("Concert not found.");
        StringBuilder text = new StringBuilder(Ui.details(row.columns(), row.values().get(0)));
        text.append("\nAssigned suppliers:\n");
        for (Object[] supplier : service.concertSuppliers(id).values())
            text.append(supplier[1]).append(" — ").append(supplier[2]).append('\n');
        Ui.view(this, "Concert Details", text.toString());
    }

    private static void select(JComboBox<AppService.Item> box, int id) {
        for (int i = 0; i < box.getItemCount(); i++)
            if (box.getItemAt(i).id() == id) { box.setSelectedIndex(i); return; }
    }

    private void edit(Integer id) throws Exception {
        AppService.Concert old = id == null ? null : service.concert(id);
        JComboBox<AppService.Item> artist = new JComboBox<>(
                service.items(AppService.Kind.ARTIST).toArray(AppService.Item[]::new));
        JComboBox<AppService.Item> venue = new JComboBox<>(
                service.items(AppService.Kind.VENUE).toArray(AppService.Item[]::new));
        JList<AppService.Item> suppliers = new JList<>(
                service.items(AppService.Kind.SUPPLIER).toArray(AppService.Item[]::new));
        suppliers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        suppliers.setVisibleRowCount(5);
        if (old != null) {
            select(artist, old.artist()); select(venue, old.venue());
            for (int i = 0; i < suppliers.getModel().getSize(); i++)
                if (old.suppliers().contains(suppliers.getModel().getElementAt(i).id()))
                    suppliers.addSelectionInterval(i, i);
        }
        JTextField name = new JTextField(old == null ? "" : old.name(), 22);
        JTextField date = new JTextField(old == null ? LocalDate.now().toString() : old.date().toString());
        JTextField start = new JTextField(old == null ? "18:00" : old.start().toString());
        JTextField end = new JTextField(old == null ? "21:00" : old.end().toString());
        JTextField capacity = new JTextField(old == null ? "" : String.valueOf(old.capacity()));
        JPanel form = Ui.form();
        Ui.field(form, "Concert Name", name);
        Ui.field(form, "Artist", artist);
        Ui.field(form, "Venue", venue);
        Ui.field(form, "Date (YYYY-MM-DD)", date);
        Ui.field(form, "Start Time (HH:MM)", start);
        Ui.field(form, "End Time (HH:MM)", end);
        Ui.field(form, "Ticket Capacity", capacity);
        Ui.field(form, "Suppliers (Ctrl-click: 1–10)", new JScrollPane(suppliers));
        while (Ui.confirm(this, id == null ? "Add Concert" : "Edit Concert", form)) {
            try {
                AppService.Item a = (AppService.Item) artist.getSelectedItem();
                AppService.Item v = (AppService.Item) venue.getSelectedItem();
                if (a == null || v == null) throw new IllegalArgumentException("Add an artist and venue first.");
                List<Integer> ids = suppliers.getSelectedValuesList().stream().map(AppService.Item::id).toList();
                service.saveConcert(id, new AppService.Concert(name.getText(), a.id(), v.id(),
                        LocalDate.parse(date.getText().trim()), LocalTime.parse(start.getText().trim()),
                        LocalTime.parse(end.getText().trim()), Integer.parseInt(capacity.getText().trim()), ids));
            } catch (Exception e) { Ui.error(this, e); continue; }
            appliedMonth = null;
            refresh();
            return;
        }
    }

    private void delete() throws Exception {
        int id = Ui.selectedId(table);
        if (Ui.confirm(this, "Delete Concert", "Delete concert #" + id + "?")) {
            service.deleteConcert(id);
            refresh();
        }
    }
}
