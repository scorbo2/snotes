package ca.corbett.snotes.model.filter;

/**
 * Represents the type of date comparison to be performed by a DateFilter.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public enum DateFilterType {
    BEFORE_EXCLUSIVE("Before"),
    BEFORE_INCLUSIVE("Before (inclusive)"),
    ON("Equal to"),
    AFTER_INCLUSIVE("After (inclusive)"),
    AFTER_EXCLUSIVE("After");

    private final String label;

    DateFilterType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
