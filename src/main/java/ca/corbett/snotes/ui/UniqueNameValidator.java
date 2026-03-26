package ca.corbett.snotes.ui;

import ca.corbett.forms.fields.ShortTextField;
import ca.corbett.forms.validators.FieldValidator;
import ca.corbett.forms.validators.ValidationResult;
import ca.corbett.snotes.model.Query;

/**
 * A generic FieldValidator implementation to check to see if a user-entered name is unique.
 * You can optionally specify an existing name to exclude from the check,
 * which is useful when editing an existing item. For example, the user edits
 * "My Item" - we want to skip the uniqueness check for "My Item", since
 * it represents no change from the current name, and is therefore valid.
 * <p>
 * This validator will also ensure that the given value is
 * shorter than a specified length limit, to keep names to a reasonable length.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class UniqueNameValidator implements FieldValidator<ShortTextField> {

    public static final String ERR_NOT_UNIQUE = "That name is already in use. Please choose a different name.";
    public static final String ERR_TOO_LONG = "Name cannot be longer than " + Query.NAME_LENGTH_LIMIT + " characters.";

    /**
     * Supply an instance of this interface, and it will be used to check uniqueness
     * of the given name. Your implementation should tolerate null values for
     * the nameToExclude, which means "don't exclude any name from the check".
     * The given name will never be null or blank.
     */
    public interface UniqueNameChecker {
        boolean isNameAvailable(String name, String nameToExclude);
    }

    private final UniqueNameChecker nameChecker;
    private final int nameLengthLimit;
    private final String nameToExclude;

    /**
     * Creates a new UniqueNameValidator with no excluded name and the given name length limit,
     * which must be positive.
     */
    public UniqueNameValidator(UniqueNameChecker nameChecker, int nameLengthLimit) {
        this(nameChecker, nameLengthLimit, null);
    }

    /**
     * Creates a new UniqueNameValidator with the specified name to exclude from the check
     * and the given name length limit, which must be positive. The name to exclude can be null to disable exclusion.
     */
    public UniqueNameValidator(UniqueNameChecker nameChecker, int nameLengthLimit, String nameToExclude) {
        if (nameChecker == null) {
            throw new IllegalArgumentException("nameChecker cannot be null");
        }
        if (nameLengthLimit <= 0) {
            throw new IllegalArgumentException("nameLengthLimit must be positive");
        }
        this.nameChecker = nameChecker;
        this.nameLengthLimit = nameLengthLimit;
        this.nameToExclude = nameToExclude; // Okay if null
    }

    @Override
    public ValidationResult validate(ShortTextField fieldToValidate) {
        // Note that we don't need to check for blank or null values here,
        // since the ShortTextField itself will already enforce that.
        // We are therefore guaranteed to get a non-blank value here.
        String value = fieldToValidate.getText();
        if (!nameChecker.isNameAvailable(value, nameToExclude)) {
            return ValidationResult.invalid(ERR_NOT_UNIQUE);
        }
        if (value.length() > nameLengthLimit) {
            return ValidationResult.invalid(ERR_TOO_LONG);
        }
        return ValidationResult.valid();
    }
}
