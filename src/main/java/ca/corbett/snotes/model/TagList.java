package ca.corbett.snotes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a list of Tags that can be applied to a Snote.
 * There's some logic to the content of a Snote's tag list. Namely:
 * <ul>
 * <li>If a DateTag is in the list, there must be at most one.
 * <li>Tags are kept sorted such that the DateTag (if present) shows up first always.
 * <li>Subsequent Tags are listed in alphabetical order.
 * <li>Attempts to add duplicate tags are ignored (tags are unique within a TagList).
 * </ul>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public final class TagList {

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
     * today's date will be used.
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
     * Sets the date using the given string in yyyy-MM-dd format.
     * If badly formatted, the date will be set to today's date.
     * If null, the date for this tag list will be removed.
     *
     * @param date A String hopefully in yyyy-MM-dd format.
     */
    public void setDate(String date) {
        dateTag = new DateTag(date);
    }

    /**
     * Sets the date using the given YMDDate object.
     * If invalid, today's date will be used.
     * If null, the date for this tag list will be removed.
     *
     * @param date The date for this tag list, or null to clear the date.
     */
    public void setDate(YMDDate date) {
        dateTag = (date == null) ? null : new DateTag(date);
    }

    /**
     * Sets the date for this tag list using the given DateTag.
     * If null, the date for this tag list will be removed.
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
        if (Tag.isDateValue(tagString)) {
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
     */
    public void removeTag(String tagString) {
        removeTag(new Tag(tagString));
    }

    /**
     * Removes the given tag from this list, if it was present.
     * If the given tag is a DateTag that matches the DateTag for
     * this tag list, then this is equivalent to setDateTag(null)
     *
     * @param tag The tag to remove from this list.
     */
    public void removeTag(Tag tag) {
        if (tag instanceof DateTag) {
            DateTag theTag = (DateTag)tag;
            if (theTag.equals(dateTag)) { // TODO so if someone tries to remove a date tag that doesn't match ours, we ignore it?
                dateTag = null; // TODO and shouldn't we return after this, regardless if it matches or not? the remove() call below will do nothing...
            }
        }
        tags.remove(tag);
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
        return hasTag(new Tag(tag));
    }

    /**
     * Reports whether the specified tag is contained in this tag list.
     *
     * @param tag The value to search.
     * @return True if the given tag is present in this tag list.
     */
    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    /**
     * Returns a sorted list of tags contained in this list.
     *
     * @return A list of tags.
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
     * @return A list of non-date tags.
     */
    public List<Tag> getNonDateTags() {
        return new ArrayList<>(tags); // tags is a SortedSet, so it's already sorted.
    }
}