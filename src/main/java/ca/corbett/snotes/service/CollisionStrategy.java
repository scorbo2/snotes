package ca.corbett.snotes.service;

/**
 * Strategies for handling filename collisions when saving a Note.
 * <p>
 * A collision occurs when the Note's computed save location (which is based
 * on its date and tags) is already occupied by a different Note.
 * This enum is the service-layer equivalent of {@code DataManager.CollisionStrategy}.
 * Implementations of {@link NoteService} are responsible for mapping between the two.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public enum CollisionStrategy {

    /**
     * Delete the existing Note at the save location and save the new one in its place.
     */
    OVERWRITE,

    /**
     * Append the new Note's text to the existing Note at the save location,
     * and discard the new Note's file.
     */
    APPEND,

    /**
     * Abort the save and report an IOException to the caller.
     */
    ABORT;

    public static CollisionStrategy fromString(String input) {
        if (input == null) {
            return null;
        }
        return switch (input.toLowerCase()) {
            case "overwrite" -> OVERWRITE;
            case "append" -> APPEND;
            case "abort" -> ABORT;
            default -> null;
        };
    }
}
