package ca.corbett.snotes.model;

import ca.corbett.snotes.io.QueryCache;
import ca.corbett.snotes.io.SnoteCache;
import ca.corbett.snotes.io.TemplateCache;

/**
 * A SnoteCollection represents a filesystem grouping of Snotes, Templates, and Queries.
 * Use the Scanner class to load a SnoteCollection from disk.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class SnoteCollection {

    private final SnoteCache snoteCache;
    private final TemplateCache templateCache;
    private final QueryCache queryCache;

    public SnoteCollection(SnoteCache snoteCache,
                          TemplateCache templateCache,
                          QueryCache queryCache) {
        this.snoteCache = snoteCache;
        this.templateCache = templateCache;
        this.queryCache = queryCache;
    }

    /**
     * Provides access to the SnoteCache for this Collection.
     */
    public SnoteCache getSnoteCache() {
        return snoteCache;
    }

    /**
     * Provides access to the TemplateCache for this Collection.
     */
    public TemplateCache getTemplateCache() {
        return templateCache;
    }

    /**
     * Provides access to the QueryCache for this Collection.
     */
    public QueryCache getQueryCache() {
        return queryCache;
    }
}
