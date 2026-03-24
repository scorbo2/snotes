package ca.corbett.snotes.ui.query;

import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Query;

/**
 * A quick FieldValidator implementation to check to see if a user-entered Query name
 * is available (that is, not already in use by some other Query).
 * You can optionally specify an existing Query name to exclude from the check,
 * which is useful when editing an existing Query. For example, the user edits
 * "My Query" - we want to skip the uniqueness check for "My Query", since
 * it represents no change from the current name, and is therefore valid.
 * <p>
 * This validator will also ensure that the given value is
 * shorter than Query.NAME_LENGTH_LIMIT, to keep names to a reasonable length.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class QueryNameValidator implements FieldValidator<ShortTextField> {

    public static final String ERR_NOT_UNIQUE = "That name is already in use. Please choose a different name.";
    public static final String ERR_TOO_LONG = "Name cannot be longer than " + Query.NAME_LENGTH_LIMIT + " characters.";

    private final DataManager dataManager;
    private final String nameToExclude;

    /**
     * Creates a new QueryNameValidator with no excluded name. This is intended
     * for use when creating a new Query! It will ensure that the user-entered
     * name is not already in use.
     *
     * @param dataManager The DataManager instance to use for uniqueness checks. This cannot be null.
     */
    public QueryNameValidator(DataManager dataManager) {
        this(dataManager, null);
    }

    /**
     * Creates a new QueryNameValidator with the specified name to exclude from the check.
     * This is intended for use when editing an existing Query. It will ensure that the user-entered
     * name is not already in use by some Query other than the one with the excluded name.
     *
     * @param dataManager   The DataManager instance to use for uniqueness checks. This cannot be null.
     * @param nameToExclude the name of the Query to exclude from the uniqueness check. Can be null to disable exclusion.
     */
    public QueryNameValidator(DataManager dataManager, String nameToExclude) {
        if (dataManager == null) {
            throw new IllegalArgumentException("dataManager cannot be null");
        }
        this.dataManager = dataManager;
        this.nameToExclude = nameToExclude;
    }

    @Override
    public ValidationResult validate(ShortTextField fieldToValidate) {
        // Note that we don't need to check for blank or null values here,
        // since the ShortTextField itself will already enforce that.
        // We are therefore guaranteed to get a non-blank value here.
        String value = fieldToValidate.getText();
        if (!dataManager.isQueryNameAvailable(value, nameToExclude)) {
            return ValidationResult.invalid(ERR_NOT_UNIQUE);
        }
        if (value.length() > Query.NAME_LENGTH_LIMIT) {
            return ValidationResult.invalid(ERR_TOO_LONG);
        }
        return ValidationResult.valid();
    }
}
