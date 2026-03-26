package ca.corbett.snotes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Represents a list of Tags that can be applied to a Note.
 * There's some logic to the content of a Note's tag list. Namely:
 * <ul>
 * <li>If a DateTag is in the list, there must be at most one.</li>
 * <li>Tags are kept sorted such that the DateTag (if present) shows up first always.</li>
 * <li>Subsequent Tags are listed in alphabetical order.</li>
 * <li>Attempts to add duplicate tags are ignored (tags are unique within a TagList).</li>
 * <li>Tag values are subject to the validation/sanitization rules defined in Tag and DateTag.</li>
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public final class TagList {

    private static final Logger log = Logger.getLogger(TagList.class.getName());

    private DateTag dateTag;
    private final SortedSet<Tag> tags;

    /**
     * Creates an empty, undated tag list.
     */
    public TagList() {
        dateTag = null;
        tags = new TreeSet<>();
    }

    /**
     * Creates a TagList with the given date. If the given date is null,
     * today's date will be used. To create an undated TagList,
     * use the no-args constructor instead.
     *
     * @param date The date for this tag list.
     */
    public TagList(YMDDate date) {
        if (date == null) {
            date = new YMDDate();
        }
        dateTag = new DateTag(date);
        tags = new TreeSet<>();
    }

    /**
     * A factory method to create a new TagList instance from a comma-separated or space-separated string of tags.
     * If the input string is null or blank, an empty TagList will be returned.
     * <p>
     * You can use commas or whitespace to separate tags. Both "tag1, tag2, tag3"
     * and "tag1 tag2 tag3" are valid inputs that will produce the same TagList with
     * three tags: "tag1", "tag2", and "tag3".
     * The input string can also be a mix of commas and whitespace, e.g. "tag1, tag2 tag3,tag4".
     * </p>
     *
     * @param tagString A comma or space separated string of tags. Null or blank gets you an empty list.
     */
    public static TagList fromRawString(String tagString) {
        TagList tagList = new TagList();
        if (tagString == null) {
            return tagList; // return an empty TagList
        }
        String trimmed = tagString.trim();
        if (trimmed.isBlank()) {
            return tagList; // return an empty TagList
        }
        String[] tagArray = trimmed.split("[\\s,]+");
        for (String tag : tagArray) {
            if (tag == null) {
                continue;
            }
            String normalized = tag.trim();
            if (normalized.isEmpty()) {
                continue; // skip empty/blank tokens
            }
            tagList.addTag(normalized);
        }
        return tagList;
    }

    /**
     * Factory method to return a new TagList from a List of tag strings. Each string will be normalized
     * using the same rules as Tag's constructor, so the resulting TagList may not match the input list exactly.
     *
     * @param rawTags A List of tag strings. If null, an empty TagList will be returned.
     * @return A new TagList containing the tags from the input list, normalized according to the rules in Tag's constructor.
     */
    public static TagList fromStringList(List<String> rawTags) {
        TagList tagList = new TagList();
        if (rawTags == null) {
            return tagList; // return an empty TagList
        }
        for (String rawTag : rawTags) {
            tagList.addTag(rawTag);
        }
        return tagList;
    }

    /**
     * Factory method to return a new TagList from a List of Tag objects.
     * If the input list is null, an empty TagList will be returned.
     *
     * @param tags A List of Tag objects. If null, an empty TagList will be returned.
     * @return A new TagList containing the tags from the input list.
     */
    public static TagList fromTagList(List<Tag> tags) {
        TagList tagList = new TagList();
        if (tags == null) {
            return tagList; // return an empty TagList
        }
        for (Tag tag : tags) {
            tagList.addTag(tag);
        }
        return tagList;
    }

    /**
     * Sets the date using the given string in yyyy-MM-dd format.
     * If badly formatted, the date will be set to today's date.
     * If null, the date for this tag list will be removed.
     * Any previous date for this TagList will be replaced -
     * a TagList can only have one DateTag at a time!
     *
     * @param date A String hopefully in yyyy-MM-dd format.
     */
    public void setDate(String date) {
        dateTag = (date == null) ? null : new DateTag(date);
    }

    /**
     * Sets the date using the given YMDDate object.
     * If invalid, today's date will be used.
     * If null, the date for this tag list will be removed.
     * Any previous date for this TagList will be replaced -
     * a TagList can only have one DateTag at a time!
     *
     * @param date The date for this tag list, or null to clear the date.
     */
    public void setDate(YMDDate date) {
        dateTag = (date == null) ? null : new DateTag(date);
    }

    /**
     * Sets the date for this tag list using the given DateTag.
     * A null value is valid here, and means "remove the date from this TagList".
     * Any previous date for this TagList will be replaced -
     * a TagList can only have one DateTag at a time!
     *
     * @param newTag The date for this tag list, or null to clear the date.
     */
    public void setDateTag(DateTag newTag) {
        dateTag = (newTag == null) ? null : new DateTag(newTag);
    }

    /**
     * Returns the DateTag for this tag list, or null if there isn't one.
     */
    public DateTag getDateTag() {
        return dateTag;
    }

    /**
     * Indicates whether this TagList has a DateTag or not.
     */
    public boolean hasDate() {
        return dateTag != null;
    }

    /**
     * Adds the given tag to this list. If the input string is in yyyy-MM-dd format,
     * then this will set the DateTag for this list instance. Otherwise, the
     * input string is treated as a normal Tag.
     */
    public void addTag(String tagString) {
        if (YMDDate.isValidYMD(tagString)) {
            setDateTag(new DateTag(tagString));
            return;
        }
        addTag(new Tag(tagString));
    }

    /**
     * Adds the given tag to this list.
     * If the given Tag is a DateTag, then this is equivalent
     * to calling setDateTag(tag);
     */
    public void addTag(Tag tag) {
        if (tag instanceof DateTag) {
            setDateTag((DateTag)tag);
            return;
        }
        tags.add(tag);
    }

    /**
     * Removes the given tag from this list, if it was present.
     *
     * @return True if the tag was found and removed, false if the tag was not found in this list.
     */
    public boolean removeTag(String tagString) {
        return removeTag(YMDDate.isValidYMD(tagString) ? new DateTag(tagString) : new Tag(tagString));
    }

    /**
     * Removes the given tag from this list, if it was present.
     * If the given tag is a DateTag that matches the DateTag for
     * this tag list, then this is equivalent to setDateTag(null).
     *
     * @param tag The tag to remove from this list.
     * @return True if the tag was found and removed, false if the tag was not found in this list.
     */
    public boolean removeTag(Tag tag) {
        if (tag instanceof DateTag dateTagToRemove) {
            if (dateTagToRemove.equals(dateTag)) {
                dateTag = null;
                return true;
            }
            else {
                log.warning("Attempted to remove a DateTag that is not present in this TagList. No action taken.");
            }
        }
        return tags.remove(tag);
    }

    /**
     * Returns the count of tags (including optional DateTag) in this list.
     *
     * @return How many tags are present.
     */
    public int size() {
        int size = (dateTag != null) ? 1 : 0;
        return size + tags.size();
    }

    /**
     * Removes all tags from this list, including the DateTag.
     */
    public void clear() {
        dateTag = null;
        tags.clear();
    }

    /**
     * Reports whether the specified tag is contained in this tag list.
     * This is shorthand for hasTag(new Tag(tag));
     *
     * @param tag The String value to search.
     * @return True if the given tag is present in this tag list.
     */
    public boolean hasTag(String tag) {
        // normalize the input so we're comparing apples to apples, and also handle DateTags.
        return hasTag(YMDDate.isValidYMD(tag) ? new DateTag(tag) : new Tag(tag));
    }

    /**
     * Reports whether the specified tag is contained in this tag list.
     * If the given tag is a DateTag, then this checks if it matches the DateTag for this TagList.
     * Otherwise, it checks if the given tag is present in the set of non-date tags for this TagList.
     *
     * @param tag The value to search.
     * @return True if the given tag is present in this tag list.
     */
    public boolean hasTag(Tag tag) {
        if (tag instanceof DateTag theDateTag) {
            return theDateTag.equals(dateTag);
        }
        return tags.contains(tag);
    }

    /**
     * Returns a sorted list of tags contained in this list.
     * If a DateTag is present, it is always returned first.
     * All other tags are returned in alphabetical order after the DateTag.
     * To exclude the DateTag, use getNonDateTags() instead.
     *
     * @return A list of all tags in this list.
     */
    public List<Tag> getTags() {
        List<Tag> result = new ArrayList<>();
        if (dateTag != null) {
            result.add(dateTag);
        }
        result.addAll(tags); // tags is a SortedSet, so it's already sorted.
        return result;
    }

    /**
     * Returns a sorted list of all non-date tags contained in this list.
     *
     * @return A list of all non-date tags in this list.
     */
    public List<Tag> getNonDateTags() {
        return new ArrayList<>(tags); // tags is a SortedSet, so it's already sorted.
    }

    /**
     * Returns a machine-readable String representation of this TagList, suitable for persistence to disk.
     * If a date tag is present, it is presented first, in yyyy-MM-dd format.
     * All other tags follow, in alphabetical order, separated by spaces.
     *
     * @return A machine-readable String representation of this TagList, suitable for persistence to disk.
     */
    public String getPersistenceString() {
        StringBuilder sb = new StringBuilder();
        if (dateTag != null) {
            sb.append(dateTag);
            sb.append(" ");
        }
        for (Tag tag : tags) {
            sb.append(tag.toString()); // # plus tag contents
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Returns a human-presentable String representation of this TagList.
     * If a date tag is present, it is presented first, with the day name in parentheses.
     * All other tags follow, in alphabetical order. For example, given a date
     * tag of 1997-04-21 and two normal tags "tag1" and "tag2", the output would be:
     * <pre>
     *     #1997-04-21 (Monday) #tag1 #tag2
     * </pre>
     *
     * @return A human-presentable String representation of this TagList.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (hasDate()) {
            builder.append("#");
            builder.append(dateTag.getDate().toString());
            builder.append(" (");
            builder.append(dateTag.getDate().getDayName());
            builder.append(")");

            if (!tags.isEmpty()) {
                builder.append(" ");
            }
        }
        for (Tag tag : tags) {
            if (tag instanceof DateTag) {
                continue; // skip the date already output above
            }
            builder.append("#");
            builder.append(tag.getTag());
            builder.append(" ");
        }
        return builder.toString().trim() + System.lineSeparator();
    }
}