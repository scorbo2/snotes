package ca.corbett.snotes.model.filter;

/**
 * Represents a simple "is" or "is not" filter that can be used for simple comparisons.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public enum BooleanFilterType {
    IS("Is"), IS_NOT("Is not");

    private final String label;

    BooleanFilterType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
