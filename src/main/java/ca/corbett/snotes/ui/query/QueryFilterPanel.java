package ca.corbett.snotes.ui.query;

import ca.corbett.extras.MessageUtil;
import ca.corbett.forms.Alignment;
import ca.corbett.forms.FormPanel;
import ca.corbett.forms.fields.ButtonField;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.filter.Filter;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contains QueryFilterFields for building a query.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryFilterPanel extends JPanel {

    private static final Logger log = Logger.getLogger(QueryFilterPanel.class.getName());
    private MessageUtil messageUtil;

    /**
     * An arbitrary limit on the number of filters you can define
     * for a single Query. The Query class itself doesn't actually
     * have a limit, but the UI gets clunky if we allow limitless filters.
     */
    private static final int MAX_FILTERS = 8;

    private final FormPanel formPanel;
    private final Query queryToEdit; // null if we're creating a new Query
    private final List<QueryFilterField> filterFields;
    private int filterCount;

    /**
     * Builds a new, empty QueryFilterPanel. A single text filter field will be visible.
     * Controls will be presented to add additional fields (up to 8), or to remove fields (minimum 1).
     */
    public QueryFilterPanel() {
        this(null);
    }

    /**
     * Builds a QueryFilterPanel populated with the filters from the given Query.
     * If the given Query is null, this behaves the same as the no-arg constructor and creates a blank panel.
     * If the given Query has more than 8 filters, only the first 8 will be used to populate the panel, and the user
     * will not be able to add additional filters beyond those 8.
     *
     * @param query The Query to populate the panel with. If null, the panel will be blank.
     */
    public QueryFilterPanel(Query query) {
        formPanel = new FormPanel(Alignment.TOP_LEFT);
        formPanel.setBorderMargin(8);
        filterFields = new ArrayList<>(MAX_FILTERS);
        this.queryToEdit = query;
        List<Filter> filters = (queryToEdit == null) ? List.of() : queryToEdit.getFilters();
        for (int i = 0; i < MAX_FILTERS; i++) {
            QueryFilterField filterField = new QueryFilterField();
            if (i < filters.size()) {
                filterField.setValuesFrom(filters.get(i));
            }
            filterFields.add(filterField);
        }
        initComponents();
    }

    /**
     * Will try to set keyboard focus to the first filter field's value field.
     * If the first filter doesn't have a filter field (like the undated filter), does nothing.
     */
    public void requestKeyboardFocus() {
        QueryFilterField firstField = filterFields.get(0);
        firstField.getFilterValueField().requestFocusInWindow();
    }

    /**
     * Builds and returns a Query based on the current values in our filter fields.
     * If the constructor was supplied a Query instance to edit, the return Query
     * will be the same instance, but updated with our filter settings. Otherwise,
     * a new Query instance is created each time this method is invoked.
     *
     * @return A Query instance with filters corresponding to our current values.
     */
    public Query getQuery() {
        List<Filter> filters = new ArrayList<>(filterCount);
        for (int i = 0; i < filterCount; i++) {
            filters.add(filterFields.get(i).getFilter());
        }
        Query query;
        if (queryToEdit == null) {
            // This is a new Query:
            query = new Query();
            for (Filter filter : filters) {
                query.addFilter(filter);
            }
        }
        else {
            // We're editing an existing query, let's return the same instance with the updated values:
            query = queryToEdit;
            query.clear(); // nuke and pave all filters to ensure we have what the user specified
            for (Filter filter : filters) {
                query.addFilter(filter);
            }
        }

        return query;
    }

    /**
     * Delegates to our FormPanel's isFormValid() method, which checks that all visible fields are valid.
     * Validation errors will be shown inline if anything is wrong.
     */
    public boolean isFormValid() {
        return formPanel.isFormValid();
    }

    private void initComponents() {
        formPanel.add(filterFields.get(0)); // Default label is "Filter:"
        filterCount = (queryToEdit == null)
            ? 1 // There's always at least one filter visible, even if blank
            : Math.max(1, Math.min(queryToEdit.size(), MAX_FILTERS)); // keep between 1 and MAX_FILTERS, inclusive
        for (int i = 1; i < filterFields.size(); i++) {
            QueryFilterField filterField = filterFields.get(i);
            filterField.getFieldLabel().setText("AND:"); // only supported option currently, so make it the label.
            if (i >= filterCount) {
                filterField.setVisible(false); // we'll show them as the user hits "add filter"
            }
            formPanel.add(filterField);
        }
        ButtonField buttonField = new ButtonField();
        buttonField.addButton(new AddFilterAction());
        buttonField.addButton(new RemoveFilterAction());
        formPanel.add(buttonField);

        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
    }

    private class AddFilterAction extends AbstractAction {

        public AddFilterAction() {
            super("Add Filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (filterCount >= MAX_FILTERS) {
                getMessageUtil().info("Maximum filters reached",
                                      "You can only have " + MAX_FILTERS + " filters in a query.");
                return;
            }
            filterFields.get(filterCount++).setVisible(true);
        }
    }

    private class RemoveFilterAction extends AbstractAction {

        public RemoveFilterAction() {
            super("Remove Filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (filterCount <= 1) {
                getMessageUtil().info("Minimum filters reached",
                                      "A query must have at least one filter.");
                return;
            }
            filterFields.get(--filterCount).setVisible(false);
        }
    }

    private MessageUtil getMessageUtil() {
        if (messageUtil == null) {
            messageUtil = new MessageUtil(this, log);
        }
        return messageUtil;
    }
}
