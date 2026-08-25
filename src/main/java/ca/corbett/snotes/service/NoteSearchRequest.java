package ca.corbett.snotes.service;

import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.TagList;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilter;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.TagFilter;
import ca.corbett.snotes.model.filter.TextFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An immutable collection of criteria for searching Notes through a {@link NoteService}.
 * <p>
 * Each criterion is optional. A request with no criteria returns all (non-scratch) Notes.
 * Multiple criteria are ANDed together, matching {@link Query} semantics:
 * </p>
 * <ul>
 *     <li>{@code text} - Notes must contain this text (case-insensitive).</li>
 *     <li>{@code tags} - Notes must have ALL of these tags.</li>
 *     <li>{@code startDate} - Notes must be dated on or after this date.</li>
 *     <li>{@code endDate} - Notes must be dated on or before this date.</li>
 *     <li>{@code limit} - Only the most recent N results are returned.</li>
 * </ul>
 * <p>
 * <b>Note:</b> as with all date-based filtering in Snotes, any date criterion
 * automatically excludes undated Notes.
 * </p>
 * <p>
 * Tag strings are normalized the same way as everywhere else in Snotes
 * (lower-cased, spaces/slashes/hashes replaced with underscores, etc.).
 * A tag string in yyyy-MM-dd format is treated as a DateTag, matching the
 * behavior of the simple search field in the UI.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.3
 */
public final class NoteSearchRequest {

    /**
     * A limit value meaning "no upper limit on results", matching
     * {@link Query#execute(List, int)}.
     */
    public static final int NO_LIMIT = Integer.MAX_VALUE;

    private final String text;
    private final List<String> tags;
    private final YMDDate startDate;
    private final YMDDate endDate;
    private final int limit;

    private NoteSearchRequest(Builder builder) {
        this.text = builder.text;
        this.tags = List.copyOf(builder.tags); // defensive copy; Builder has already skipped null/blank entries
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.limit = builder.limit;
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0: " + limit);
        }
        // Note: YMDDate.isAfter(null) returns true (null sorts as "oldest"),
        // so both bounds must be non-null before comparing them.
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                "startDate (" + startDate + ") must not be after endDate (" + endDate + ")");
        }
    }

    /**
     * Creates a new builder for NoteSearchRequest instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the text criterion, or null if no text search was requested.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns an unmodifiable view of the raw (unnormalized) tag strings.
     * Null/blank entries are never present.
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Returns the inclusive lower date bound, or null if none was set.
     */
    public YMDDate getStartDate() {
        return startDate;
    }

    /**
     * Returns the inclusive upper date bound, or null if none was set.
     */
    public YMDDate getEndDate() {
        return endDate;
    }

    /**
     * Returns the maximum number of results to return. Defaults to {@link #NO_LIMIT}.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Reports whether no search criteria have been set at all.
     * The limit does not count as a criterion here.
     */
    public boolean isEmpty() {
        return (text == null || text.isBlank())
            && tags.isEmpty()
            && startDate == null
            && endDate == null;
    }

    /**
     * Expresses this request's criteria as a {@link Query}.
     * <p>
     * This is used internally by {@link DefaultNoteService} when executing the search,
     * and by the UI to display the search criteria (for example, in a ReaderFrame's
     * query panel). The returned Query is a fresh object - it is safe to keep or discard.
     * </p>
     *
     * @return A Query expressing exactly the criteria in this request.
     */
    public Query toQuery() {
        Query query = new Query();
        if (text != null && !text.isBlank()) {
            query.addFilter(new TextFilter(text));
        }
        if (!tags.isEmpty()) {
            // fromStringList() normalizes each tag and converts yyyy-MM-dd values into DateTags.
            // Null/blank entries were already removed by the Builder, so this can't throw.
            TagList tagList = TagList.fromStringList(tags);
            List<Tag> normalizedTags = tagList.getTags();
            if (!normalizedTags.isEmpty()) {
                query.addFilter(new TagFilter(normalizedTags, TagFilter.FilterType.ALL));
            }
        }
        if (startDate != null) {
            query.addFilter(new DateFilter(startDate, DateFilterType.AFTER_INCLUSIVE));
        }
        if (endDate != null) {
            query.addFilter(new DateFilter(endDate, DateFilterType.BEFORE_INCLUSIVE));
        }
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoteSearchRequest that = (NoteSearchRequest)o;
        return limit == that.limit
            && Objects.equals(text, that.text)
            && Objects.equals(tags, that.tags)
            && Objects.equals(startDate, that.startDate)
            && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, tags, startDate, endDate, limit);
    }

    @Override
    public String toString() {
        return "NoteSearchRequest{text='" + text + "', tags=" + tags
            + ", startDate=" + startDate + ", endDate=" + endDate
            + (limit == NO_LIMIT ? "" : ", limit=" + limit) + "}";
    }

    /**
     * A builder for {@link NoteSearchRequest} instances.
     * All criteria are optional; unset criteria mean "no filtering by that criterion".
     */
    public static final class Builder {

        private String text;
        private final List<String> tags = new ArrayList<>();
        private YMDDate startDate;
        private YMDDate endDate;
        private int limit = NO_LIMIT;

        private Builder() {
        }

        /**
         * Sets the text criterion. Null or blank values clear the criterion.
         */
        public Builder withText(String text) {
            this.text = (text == null || text.isBlank()) ? null : text;
            return this;
        }

        /**
         * Replaces the tag criterion with the given tag strings.
         * Null/blank entries are skipped. Passing null clears the criterion.
         */
        public Builder withTags(List<String> tags) {
            this.tags.clear();
            if (tags != null) {
                for (String tag : tags) {
                    if (tag != null && !tag.isBlank()) {
                        this.tags.add(tag);
                    }
                }
            }
            return this;
        }

        /**
         * Replaces the tag criterion with the given tag strings.
         * Null/blank entries are skipped. Passing null clears the criterion.
         * <p>
         * Note: Arrays.asList() is used here rather than List.of(), because
         * the latter rejects null elements, and we specifically want to
         * tolerate (and skip) them.
         * </p>
         */
        public Builder withTags(String... tags) {
            return withTags(tags == null ? null : Arrays.asList(tags));
        }

        /**
         * Sets the inclusive lower date bound. Null clears the bound.
         */
        public Builder withStartDate(YMDDate date) {
            this.startDate = date;
            return this;
        }

        /**
         * Sets the inclusive lower date bound from a yyyy-MM-dd string.
         * Null or blank clears the bound.
         *
         * @throws IllegalArgumentException if the string is not a valid yyyy-MM-dd date.
         */
        public Builder withStartDate(String date) {
            this.startDate = parseStrictDate(date, "startDate");
            return this;
        }

        /**
         * Sets the inclusive upper date bound. Null clears the bound.
         */
        public Builder withEndDate(YMDDate date) {
            this.endDate = date;
            return this;
        }

        /**
         * Sets the inclusive upper date bound from a yyyy-MM-dd string.
         * Null or blank clears the bound.
         *
         * @throws IllegalArgumentException if the string is not a valid yyyy-MM-dd date.
         */
        public Builder withEndDate(String date) {
            this.endDate = parseStrictDate(date, "endDate");
            return this;
        }

        /**
         * Sets the maximum number of results to return.
         * Use {@link NoteSearchRequest#NO_LIMIT} for no upper limit.
         * Values less than 0 are rejected by {@link #build()}.
         */
        public Builder withLimit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Builds the immutable NoteSearchRequest.
         *
         * @throws IllegalArgumentException if the limit is negative, or if the start date
         *                                  is after the end date.
         */
        public NoteSearchRequest build() {
            return new NoteSearchRequest(this);
        }

        private static YMDDate parseStrictDate(String date, String fieldName) {
            if (date == null || date.isBlank()) {
                return null;
            }
            // fromJson() is strict and throws IllegalArgumentException on bad input.
            // We use it here (rather than the lenient YMDDate(String) constructor) so that
            // malformed dates from programmatic callers - such as MCP tool parameters -
            // surface as errors instead of silently becoming today's date.
            try {
                return YMDDate.fromJson(date);
            }
            catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException(fieldName + " must be a valid yyyy-MM-dd date: '" + date + "'", iae);
            }
        }
    }
}
