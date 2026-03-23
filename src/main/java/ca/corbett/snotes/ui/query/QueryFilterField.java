package ca.corbett.snotes.ui.query;

import ca.corbett.forms.fields.FormField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DayOfMonthFilter;
import ca.corbett.snotes.model.filter.DayOfWeekFilter;
import ca.corbett.snotes.model.filter.Filter;
import ca.corbett.snotes.model.filter.MonthFilter;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;
import ca.corbett.snotes.model.filter.UndatedFilter;
import ca.corbett.snotes.model.filter.YearFilter;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Locale;

/**
 * A custom FormField implementation for viewing or editing a Query filter.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryFilterField extends FormField {

    public enum FilterType {
        TEXT_INSENSITIVE("Text",
                         "Contains text:",
                         "Matches all notes that contain the specified text, ignoring case."),
        TEXT_SENSITIVE("Text (exact)",
                       "Contains text:",
                       "Matches all notes that contain the specified text, respecting case."),
        TAG("Tag",
            "Has tag(s):",
            "Comma or space separated list of tags. Matches notes that have all of the specified tags."),
        DATE("Date", "Specific date:", "Enter a specific date in format yyyy-MM-dd."),
        DAY_OF_MONTH("Day of Month",
                     "Day of month:",
                     "Enter a two-digit day of month (01-31). Matches all notes in that month, regardless of year."),
        DAY_OF_WEEK("Day of Week",
                    "Day of week:",
                    "Enter a day of week (e.g. Monday). Matches all notes on that day, regardless of date."),
        MONTH("Month",
              "Month:",
              "Enter a two-digit month (01-12). Matches all notes in that month, regardless of year."),
        YEAR("Year",
             "Year:",
             "Enter a four-digit year (e.g. 2024). Matches all notes in that year, regardless of month or day."),
        UNDATED("Undated only",
                null,
                "If selected, will only match notes that do not have a date tag.");

        private final String label;
        private final String prompt;
        private final String helpText;

        FilterType(String label, String prompt, String helpText) {
            this.label = label;
            this.prompt = prompt;
            this.helpText = helpText;
        }

        public String getLabel() {
            return label;
        }

        public String getPrompt() {
            return prompt;
        }

        public String getHelpText() {
            return helpText;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private FilterType filterType;
    private final JComboBox<FilterType> filterTypeCombo;
    private final JLabel promptLabel;
    private final JTextField filterValueField;

    public QueryFilterField() {
        filterTypeCombo = new JComboBox<>(FilterType.values());
        filterTypeCombo.setEditable(false);
        filterTypeCombo.setSelectedIndex(0);
        filterValueField = new JTextField(14);
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        fieldLabel.setText("Filter:");
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new java.awt.Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.WEST;
        wrapperPanel.add(filterTypeCombo, gbc);
        promptLabel = new JLabel("Prompt:");
        gbc.gridx = 1;
        gbc.insets = new java.awt.Insets(0, 5, 0, 5);
        wrapperPanel.add(promptLabel, gbc);
        gbc.gridx = 2;
        gbc.insets = new java.awt.Insets(0, 5, 0, 0);
        wrapperPanel.add(filterValueField, gbc);
        fieldComponent = wrapperPanel;
        filterTypeCombo.addActionListener(e -> updateFilterType());
        updateFilterType();
        addFieldValidator(new ValueValidator());
    }

    /**
     * Builds a Filter object based on the current state of this QueryFilterField.
     * Note: this UI presents a subset of the available filter options. For example,
     * DateFilter supports filtering "before" or "after", and TagFilter offers
     * options like "Any", "None", and "All". This simple UI is good enough
     * to get the application on its feet, and will be revisited later.
     *
     * @return One of the subclasses of Filter, populated based on this field's current values.
     */
    public Filter getFilter() {
        return switch (filterType) {
            case TEXT_INSENSITIVE -> new TextFilter(filterValueField.getText().trim(), false);

            case TEXT_SENSITIVE -> new TextFilter(filterValueField.getText().trim(), true);

            case TAG -> new TagFilter(Arrays.stream(filterValueField.getText().trim().split("[,\\s]+"))
                                            .filter(s -> !s.isBlank())
                                            .map(String::trim)
                                            .map(Tag::new)
                                            .toList(), TagFilter.FilterType.ALL);

            case DATE -> new DateFilter(new YMDDate(filterValueField.getText().trim()), DateFilter.FilterType.ON);

            case DAY_OF_MONTH -> new DayOfMonthFilter(Integer.parseInt(filterValueField.getText().trim()),
                                                      DayOfMonthFilter.FilterType.IS);

            case DAY_OF_WEEK ->
                // Note: assuming English locale for now, which is probably good enough for V2 release.
                new DayOfWeekFilter(DayOfWeek.valueOf(filterValueField.getText().toUpperCase(Locale.ROOT).trim()),
                                    DayOfWeekFilter.FilterType.IS);

            case MONTH ->
                new MonthFilter(Integer.parseInt(filterValueField.getText().trim()), MonthFilter.FilterType.IS);

            case YEAR -> new YearFilter(Integer.parseInt(filterValueField.getText().trim()), YearFilter.FilterType.ON);

            case UNDATED -> new UndatedFilter();

        };
    }

    /**
     * When our filter type combo changes, we'll update our prompt, input field,
     * and help text to reflect the new filter type.
     */
    private void updateFilterType() {
        Object selected = filterTypeCombo.getSelectedItem();
        if (selected instanceof FilterType selectedType) {
            filterType = selectedType;
            promptLabel.setText(filterType.getPrompt() != null ? filterType.getPrompt() : "");
            promptLabel.setVisible(filterType != FilterType.UNDATED);
            filterValueField.setVisible(filterType != FilterType.UNDATED);
            setHelpText(filterType.getHelpText());
        }
    }

    /**
     * Used internally by our validator.
     * To retrieve the filter for this field, use getFilter() instead.
     */
    private FilterType getFilterType() {
        return filterType;
    }

    /**
     * Used internally by our validator.
     * To retrieve the filter for this field, use getFilter() instead.
     */
    private String getFilterValue() {
        return filterValueField.getText();
    }

    /**
     * An internal field validator to ensure that whatever our filter type is,
     * the given value is valid.
     */
    private static class ValueValidator implements FieldValidator<QueryFilterField> {

        @Override
        public ValidationResult validate(QueryFilterField fieldToValidate) {
            String value = fieldToValidate.getFilterValue().trim();
            switch (fieldToValidate.getFilterType()) {
                case TEXT_INSENSITIVE, TEXT_SENSITIVE, TAG -> {
                    if (value.isBlank()) {
                        return ValidationResult.invalid("Must enter a value for this filter.");
                    }
                }
                case DATE -> {
                    if (!YMDDate.isValidYMD(value)) {
                        return ValidationResult.invalid("Please enter a valid date in format yyyy-MM-dd.");
                    }
                }
                case DAY_OF_MONTH -> {
                    if (!value.matches("0[1-9]|[12][0-9]|3[01]")) {
                        return ValidationResult.invalid("Please enter a valid day of month (01-31).");
                    }
                }
                case DAY_OF_WEEK -> {
                    // TODO this assumes English locale... might be good enough for V2 release:
                    if (!value.matches("(?i)Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday")) {
                        return ValidationResult.invalid("Please enter a valid day of week (e.g. Monday).");
                    }
                }
                case MONTH -> {
                    if (!value.matches("0[1-9]|1[0-2]")) {
                        return ValidationResult.invalid("Please enter a valid month (01-12).");
                    }
                }
                case YEAR -> {
                    if (!value.matches("\\d{4}")) {
                        return ValidationResult.invalid("Please enter a valid year (e.g. 2024).");
                    }
                }
            }
            return ValidationResult.valid();
        }
    }
}
