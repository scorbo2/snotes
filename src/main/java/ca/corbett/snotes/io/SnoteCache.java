package ca.corbett.snotes.io;

import ca.corbett.snotes.model.DateTag;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.Snote;
import ca.corbett.snotes.model.Tag;
import ca.corbett.snotes.model.Template;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * This class represents an in-memory cache of all Snotes and Tags that were found in
 * a given directory scan. This class provides methods for querying the
 * Snotes contained here.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public class SnoteCache {

    private static final Logger logger = Logger.getLogger(SnoteCache.class.getName());

    private final Map<File, Snote> snotes = new HashMap<>();
    private final SortedSet<Tag> nonDateTags = new TreeSet<>();
    private final SortedSet<DateTag> dateTags = new TreeSet<>();

    SnoteCache() {

    }

    /**
     * Removes all data from the cache, including data not yet saved.
     */
    public void clear() {
        snotes.clear();
        nonDateTags.clear();
        dateTags.clear();
        logger.info("Snote cache cleared.");
    }

    /**
     * Adds the given Snote to the cache. If the sourceFile property of the given
     * Snote is not set, this call does nothing. This is because Snotes are cached
     * based on their source file.
     */
    public void add(Snote snote) {
        if (snote == null) {
            logger.warning("Attempt to add null Snote to cache - ignored.");
            return;
        }
        if (snote.getSourceFile() == null) {
            logger.warning("Attempt to add Snote with no source file to cache - ignored.");
            return;
        }
        if (snotes.get(snote.getSourceFile()) != null) {
            logger.warning("Snote cache already contains an entry for file: " +
                           snote.getSourceFile().getAbsolutePath() +
                           " - overwriting existing entry.");
        }

        snotes.put(snote.getSourceFile(), snote);

        // Interrogate it for its tags and dates and add those to our tag/date lists:
        for (Tag tag : snote.getTags()) {
            addTag(tag);
        }

        // TODO do we create it on disk at this point or wait for an explicit save?
    }

    /**
     * Adds the given Tag to the cache.
     */
    protected void addTag(Tag tag) {
        if (tag instanceof DateTag) {
            addDateTag((DateTag)tag);
        }
        else {
            nonDateTags.add(tag);
        }
    }

    /**
     * Adds the given DateTag to the cache.
     */
    public void addDateTag(DateTag tag) {
        dateTags.add(tag);
    }

    /**
     * Returns the size of the cache.
     *
     * @return How many Snotes currently in cache.
     */
    public int size() {
        return snotes.size();
    }

    /**
     * Equivalent to calling getDateTagCount() + getNonDateTagCount()
     *
     * @return A count of ALL tags here, both dated and non-dated.
     */
    public int getTagCount() {
        return nonDateTags.size() + dateTags.size();
    }

    /**
     * Returns the count of date tags in the cache.
     */
    public int getDateTagCount() {
        return dateTags.size();
    }

    /**
     * Returns the count of non-date tags in the cache.
     */
    public int getNonDateTagCount() {
        return nonDateTags.size();
    }

    /**
     * Returns a sorted list of all unique DateTags in the cache.
     */
    public List<DateTag> getUniqueDates() {
        return dateTags.stream().sorted().toList();
    }

    /**
     * Returns a sorted list of all unique years represented by DateTags in the cache.
     */
    public List<String> getYears() {
        return dateTags.stream().map(dateTag -> dateTag.getDate().getYearStr()).distinct().sorted().toList();
    }

    /**
     * Returns a sorted list of all non-date tags in the cache.
     */
    public List<Tag> getNonDateTags() {
        return nonDateTags.stream().sorted().toList();
    }

    /**
     * Returns a sorted list of all tags here - dates will be first, followed by non-date tags.
     */
    public List<Tag> getAllTags() {
        List<Tag> list = new ArrayList();
        list.addAll(getUniqueDates());
        list.addAll(getNonDateTags());
        return list;
    }

    /**
     * Removes the given entry from this cache and deletes its associated file.
     */
    public void remove(File srcFile) {
        Snote removed = snotes.remove(srcFile);
        if (removed != null) {
            logger.info("Removed Snote for file: " + srcFile.getAbsolutePath() + " from cache.");
            if (! removed.getSourceFile().delete()) {
                logger.warning("Failed to delete Snote source file: " + srcFile.getAbsolutePath());
            }
        }
        else {
            logger.warning("Attempt to remove Snote for file: " + srcFile.getAbsolutePath() +
                           " failed - no such entry in cache.");
        }

        // TODO we may want to clean up tags that are no longer used here...
        //      for each tag in the Snotes we just removed, find out if there are no other references
        //      to it, and remove it from our tag lists if so.
    }

    /**
     * Returns a sorted copy of the list of Snotes held in cache. The list is sorted
     * by the absolute path of each entry.
     */
    public List<Snote> getAll() {
        return snotes.values().stream()
                     .sorted(Comparator.comparing(snote -> snote.getSourceFile().getAbsolutePath()))
                     .toList();
    }

    /**
     * Executes the given Query and returns all entries found.
     * You can limit how many rows are returned with the rowCount option.
     * The last N entries in the found list will be returned. Note that the size of the return
     * list may be smaller than N if too few entries are found to return the requested
     * row count. The list may be empty if nothing was found.
     *
     * @param query The Query to execute.
     * @return A filtered list of snotes, which may be empty if nothing was found.
     */
    public List<Snote> executeQuery(Query query) {
        return executeQuery(query, Template.RowCountOption.ALL);
    }

    /**
     * Executes the given Query and returns the specified number of entries from the end of
     * whatever is found. You can limit how many rows are returned with the rowCount option.
     * The last N entries in the found list will be returned. Note that the size of the return
     * list may be smaller than N if too few entries are found to return the requested
     * row count. The list may be empty if nothing was found.
     *
     * @param query The Query to execute.
     * @param rowCount How many rows to return from whatever is found.
     * @return A list of snotes, which may be empty if nothing was found.
     */
    public List<Snote> executeQuery(Query query, Template.RowCountOption rowCount) {
        // Start with everything so we can filter it down:
        List<Snote> searchResults = getAll();
        List<Snote> undatedMatches = new ArrayList<>();

        if (!query.getTagString().isEmpty()) {
            for (Tag tag : query.getTags()) {
                searchResults = filterSnotesByTag(searchResults, tag.getTag());
            }

            // Save all the undated snotes that match... we'll add them back after date filtering.
            for (Snote snote : searchResults) {
                if (!snote.hasDate()) {
                    undatedMatches.add(snote);
                }
            }
        }

        // Note: date filtering will screen out ALL undated snotes, even ones that match our tag list above.
        searchResults = filterSnotesByDate(searchResults, query.getYearFilter(), query.getMonthFilter(), query.getDayFilter());

        // So, add back any undated Snotes that match our tag list, if we found any:
        searchResults.addAll(undatedMatches);

        if (!query.getTextFilter().isEmpty()) {
            searchResults = filterSnotesByText(searchResults, query.getTextFilter(), query.isCaseSensitive());
        }

        if (rowCount == Template.RowCountOption.ALL) {
            return searchResults;
        }

        int rows = 0;
        switch (rowCount) {
            case MOST_RECENT1:
                rows = 1;
                break;
            case MOST_RECENT3:
                rows = 3;
                break;
            case MOST_RECENT5:
                rows = 5;
                break;
            case MOST_RECENT10:
                rows = 10;
                break;
        }

        List<Snote> finalList = new ArrayList<>();
        for (int i = searchResults.size() - rows; i < searchResults.size(); i++) {
            if (i < 0) {
                continue;
            }
            finalList.add(searchResults.get(i));
        }
        return finalList;
    }

    /**
     * Returns the Snote that matches the given source file, if one exists.
     *
     * @param srcFile the source file in question
     * @return A Snote matching that source file, or null if not found.
     */
    public Snote findSnoteBySourceFile(File srcFile) {
        return snotes.get(srcFile);
    }

    /**
     * Returns a list of all Snotes that have the given tag value.
     *
     * @param tag The tag to search for (all tag values are case-insensitive).
     * @return A List of Snotes with that tag. May be empty.
     */
    public List<Snote> findSnotesByTag(String tag) {
        return filterSnotesByTag(getAll(), tag);
    }

    /**
     * Filters the given list of Snotes and returns only those in the list that
     * match the given tag value. All tag values are case-insensitive.
     *
     * @param list The list of Snotes to filter.
     * @param tag The tag value to search for.
     * @return A list of Snotes from the input list that match the input tag. May be empty.
     */
    public static List<Snote> filterSnotesByTag(List<Snote> list, String tag) {
        List<Snote> result = new ArrayList<>();
        for (Snote snote : list) {
            if (snote.hasTag(tag)) {
                result.add(snote);
            }
        }
        return result;
    }

    /**
     * Returns a list of all Snotes that match the given date parameters.
     * Any of the given date parameters can be null or empty, which is interpreted
     * as meaning "any" for that value. For example, to find all Snotes
     * from the year 2020: findSnotesByDate("2020",null,null);
     * <p>
     * Note that undated Snotes are never returned from this method! If a Snote
     * does not have a date tag, it is not considered by this search.
     * </p>
     *
     * @param yearStr The year to search for. Can be empty or null.
     * @param monthStr The month to search for. Can be empty or null.
     * @param dayStr The day to search for. Can be empty or null.
     * @return A list of Snotes matching the given date parameters.
     */
    public List<Snote> findSnotesByDate(String yearStr, String monthStr, String dayStr) {
        return filterSnotesByDate(getAll(), yearStr, monthStr, dayStr);
    }

    /**
     * Filters the given list of Snotes and returns only those that match the given date
     * parameters. Any of the given date parameters can be null or empty, which is interpreted
     * as meaning "any" for that value. For example, to find all Snotes
     * from the year 2020: findSnotesByDate("2020",null,null);
     * <p>
     * Note that undated Snotes are never returned from this method! If a Snote
     * does not have a date tag, it is not considered by this search.
     * </p>
     *
     * @param list The list to filter.
     * @param yearStr The year to search for. Can be empty or null.
     * @param monthStr The month to search for. Can be empty or null.
     * @param dayStr The day to search for. Can be empty or null.
     * @return A list of Snotes from the input list that match the given date parameters.
     */
    public static List<Snote> filterSnotesByDate(List<Snote> list, String yearStr, String monthStr, String dayStr) {
        List<Snote> result = new ArrayList<>();

        // Figure out what filter we got:
        boolean hasYearFilter = (yearStr != null && !yearStr.trim().isEmpty());
        boolean hasMonthFilter = (monthStr != null && !monthStr.trim().isEmpty());
        boolean hasDayFilter = (dayStr != null && !dayStr.trim().isEmpty());

        // Let's special case the stupid scenario where no filter was given:
        boolean isFilterStupid = !hasYearFilter && !hasMonthFilter && !hasDayFilter;

        for (Snote snote : list) {
            if (!snote.hasDate()) {
                continue;
            }
            if (isFilterStupid) {
                result.add(snote);
                continue;
            }

            boolean looksGood = true;
            if (hasYearFilter) {
                if (!yearStr.equals(snote.getDate().getYearStr())) {
                    looksGood = false;
                }
            }
            if (hasMonthFilter) {
                if (!monthStr.equals(snote.getDate().getMonthStr())) {
                    looksGood = false;
                }
            }
            if (hasDayFilter) {
                if (!dayStr.equals(snote.getDate().getDayStr())) {
                    looksGood = false;
                }
            }
            if (looksGood) {
                result.add(snote);
            }
        }
        return result;
    }

    /**
     * Returns a list of all Snotes that contain the given text.
     * If the given text is null or empty, all Snotes are returned.
     *
     * @param text The text to search for.
     * @param matchCase Whether to take case into consideration when searching.
     * @return A List of Snotes that contain the given text. May be empty.
     */
    public List<Snote> findSnotesByText(String text, boolean matchCase) {
        return filterSnotesByText(getAll(), text, matchCase);
    }

    /**
     * Filters the given list of Snotes and returns only those that contain the given text.
     * If the given text is null or empty, all Snotes are returned.
     *
     * @param list The list to filter.
     * @param text The text to search for.
     * @param matchCase Whether to take case into consideration when searching.
     * @return A List of Snotes that contain the given text. May be empty.
     */
    public static List<Snote> filterSnotesByText(List<Snote> list, String text, boolean matchCase) {
        List<Snote> result = new ArrayList<>();
        boolean isNoTextGiven = (text == null || text.trim().isEmpty());
        for (Snote snote : list) {
            if (isNoTextGiven) {
                result.add(snote);
            }
            else {
                if (matchCase) {
                    if (snote.getText().contains(text)) {
                        result.add(snote);
                    }
                }
                else {
                    if (snote.getText().toLowerCase().contains(text.toLowerCase())) {
                        result.add(snote);
                    }
                }
            }
        }
        return result;
    }
}