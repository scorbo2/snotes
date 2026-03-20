package ca.corbett.snotes.model;

import ca.corbett.snotes.model.filter.Filter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Query is a collection of zero or more Filter instances that can be applied
 * to a list of Notes to produce a filtered list.
 * <p>
 * Note that all Filters in the chain are "and"ed together. There
 * is currently no way to "or" filters together, or to have more complex logic like
 * "filter A and (filter B or filter C)".
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 */
public class Query {

    public static final String DEFAULT_NAME = "Unnamed Query";
    public static final int NAME_LENGTH_LIMIT = 25;

    private String name;
    private final List<Filter> filters;

    /**
     * Creates an empty, unnamed Query with no filters.
     * In this configuration, the Query will always return a completely unfiltered list of Notes when applied.
     * Use addFilter() to add filters to this Query and make it more selective.
     */
    public Query() {
        this.filters = new ArrayList<>();
        name = DEFAULT_NAME;
    }

    /**
     * Returns the name of this Query, or DEFAULT_NAME if no name has been set.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets an optional name for this Query. It should be short and user-presentable.
     * If longer than NAME_LENGTH_LIMIT, the name will be truncated to that length.
     * If null or blank, the name will be set to DEFAULT_NAME.
     */
    public Query setName(String name) {
        if (name == null || name.isBlank()) {
            name = DEFAULT_NAME;
        }
        if (name.length() > NAME_LENGTH_LIMIT) {
            name = name.substring(0, NAME_LENGTH_LIMIT);
        }
        this.name = name;
        return this;
    }

    /**
     * Returns true if there are no Filters in this Query.
     */
    public boolean isEmpty() {
        return filters.isEmpty();
    }

    /**
     * Returns a count of Filters in this Query.
     */
    public int size() {
        return filters.size();
    }

    /**
     * Removes all Filters from this Query, and returns it to an empty state.
     * An empty Query will return a completely unfiltered list of Notes when applied.
     */
    public void clear() {
        filters.clear();
    }

    /**
     * Adds a Filter to this Query. Filters are applied in the order they are added.
     * Note that there is no code here to check that the given Filters make sense
     * and don't conflict with one another. For example, you could add a DateFilter
     * of "before 2010" and another DateFilter of "after 2015" to the same Query.
     * This is technically valid, but will filter out all Notes.
     */
    public Query addFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Cannot add a null Filter to a Query.");
        }
        filters.add(filter);
        return this;
    }

    /**
     * Removes the given Filter from this Query, if it is present.
     */
    public Query removeFilter(Filter filter) {
        filters.remove(filter);
        return this;
    }

    /**
     * Returns a copy of the list of Filters in this Query.
     */
    public List<Filter> getFilters() {
        return new ArrayList<>(filters); // Return a copy to prevent external modification
    }

    /**
     * Applies our chain of filters to the provided list of Notes, returning a new list that only contains
     * Notes that were not filtered out by any of the filters in this Query.
     * The resulting list may be empty if the filters are too strict, but it will never be null.
     *
     * @param notes The list of Notes to filter. This list is not modified by this method.
     * @return A new list of Notes that passed through all the filters in this Query. May be empty, but never null.
     */
    public List<Note> filter(List<Note> notes) {
        if (notes == null) {
            return new ArrayList<>();
        }
        List<Note> filteredNotes = new ArrayList<>();
        for (Note note : notes) {
            boolean isFiltered = false;
            for (Filter filter : filters) {
                if (filter.isFiltered(note)) {
                    isFiltered = true;
                    break; // No need to check other filters if one already filters this note
                }
            }
            if (!isFiltered) {
                filteredNotes.add(note);
            }
        }
        return filteredNotes;
    }

    /**
     * Attempts to persist this Query and all of its Filters to the given file.
     * The save format is pretty-printed JSON.
     *
     * @param targetFile Any writable file. If the file already exists, it will be overwritten.
     * @throws IOException If the save fails.
     */
    public void save(File targetFile) throws IOException {
        if (targetFile == null) {
            throw new IllegalArgumentException("targetFile cannot be null");
        }
        if (targetFile.isDirectory() || (targetFile.exists() && !targetFile.canWrite())) {
            throw new IOException("Target file is not a writable file: " + targetFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("name", name);

        ArrayNode filtersArray = mapper.createArrayNode();
        for (Filter filter : filters) {
            filtersArray.add(mapper.valueToTree(filter));
        }
        rootNode.set("filters", filtersArray);

        mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, rootNode);
    }

    /**
     * Attempts to load a Query and its Filters from the given file,
     * which should be in the same format as produced by save().
     *
     * @param sourceFile Any query file that was generated via the save() method in this class.
     * @return A populated Query instance.
     * @throws IOException If the load fails.
     */
    public static Query load(File sourceFile) throws IOException {
        if (sourceFile == null) {
            throw new IllegalArgumentException("sourceFile cannot be null");
        }
        if (!sourceFile.exists() || sourceFile.isDirectory() || !sourceFile.canRead()) {
            throw new IOException("Source file is not a readable file: " + sourceFile.getAbsolutePath());
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(sourceFile);
        if (rootNode == null) {
            // This can happen with empty or blank files, and possibly other wonky scenarios as well:
            throw new IOException("Failed to parse Query from file: " + sourceFile.getAbsolutePath());
        }

        Query query = new Query(); // Gets DEFAULT_NAME and an empty filter list by default

        // Set query name if present in the JSON:
        JsonNode nameNode = rootNode.get("name");
        if (nameNode != null && !nameNode.isNull() && nameNode.isTextual()) {
            String nameText = nameNode.asText();
            if (nameText != null && !nameText.trim().isEmpty()) {
                query.setName(nameText);
            }
        }

        // Load up filters if any are here:
        JsonNode filtersNode = rootNode.get("filters");
        if (filtersNode != null && filtersNode.isArray()) {
            for (JsonNode filterNode : filtersNode) {
                Filter filter = mapper.treeToValue(filterNode, Filter.class);
                query.addFilter(filter);
            }
        }

        return query;
    }
}
