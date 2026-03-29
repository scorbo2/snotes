package ca.corbett.snotes.model.filter;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This Filter allows you to filter Notes based on their tags.
 * You can specify a list of tags to filter by (at least one), and a FilterType
 * indicating the type of Tag comparison to perform.
 * For example: "all notes containing the tags tag1 and tag2" or
 * "all notes containing either tag1 or tag2" or "all notes that do not contain either tag1 or tag2".
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class TagFilter extends Filter {

    public enum FilterType {
        ALL, ANY, NONE
    }

    private final List<Tag> tagsToFilter;
    private final FilterType filterType;

    @JsonCreator
    public TagFilter(@JsonProperty("tagsToFilter") List<Tag> tagsToFilter,
                      @JsonProperty("filterType") FilterType filterType) {
        if (tagsToFilter == null || tagsToFilter.isEmpty()) {
            throw new IllegalArgumentException("tagsToFilter cannot be null or empty");
        }
        if (filterType == null) {
            throw new IllegalArgumentException("filterType cannot be null");
        }
        this.tagsToFilter = new ArrayList<>(tagsToFilter); // Defensive copy to prevent external modification
        this.filterType = filterType;
    }

    public List<Tag> getTagsToFilter() {
        return new ArrayList<>(tagsToFilter);
    }

    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public String getDescription() {
        return "Filter by specific tag(s)";
    }

    @Override
    public boolean isFiltered(Note note) {
        // Convert to HashSet for better performance:
        Set<Tag> noteTags = new HashSet<>(note == null ? List.of() : note.getTags());

        // Special handling for when the Note has no tags:
        if (noteTags.isEmpty()) {
            return switch (filterType) {
                case ALL, ANY -> true; // Note does not contain any tags, so it fails ALL and ANY filters.
                case NONE -> false; // If filtering for NONE, then an untagged note always passes.
            };
        }

        return switch (filterType) {
            case ALL -> !noteTags.containsAll(tagsToFilter);
            case ANY -> tagsToFilter.stream().noneMatch(noteTags::contains);
            case NONE -> tagsToFilter.stream().anyMatch(noteTags::contains);
        };
    }

    @Override
    public String toString() {
        TagList tagList = TagList.fromTagList(tagsToFilter);
        String prettyPrinted = "[ " + tagList.getNonDateTagsAsCommaSeparatedString() + " ]";
        return "Tags have " + filterType.toString().toLowerCase() + " of: " + prettyPrinted;
    }
}
