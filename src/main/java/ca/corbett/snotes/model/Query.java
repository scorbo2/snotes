package ca.corbett.snotes.model;

import ca.corbett.extras.properties.Properties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents parameters for a search query.
 * <p>
 *     TODO - I have many questions for my former self who wrote this long ago.
 *            1) why are we creating our own query class instead of using sql?
 *            2) why did this extend ConfigObject? Should this be an AbstractProperty now?
 *            3) why are we storing tags as a space-delimited string instead of a TagList directly?
 *            4) was the intention to link this to a custom FormField implementation for the UI?
 *                4a) and if so, why was that not done? Am I going to find custom UI code in here?
 *            Oh, god... this pre-dates AppProperties? This migration will be harder than I thought.
 *            The release notes say 2023-04-06, which would be swing-extras/sc-util 1.7 - not THAT old.
 *            But, old enough I guess. Much has changed between 1.7 and 2.6 in swing-extras.
 * </p>
 * <p>
 *     Okay, I just fired up Snotes 1.0 and took a look at the UI. There is indeed a custom panel
 *     for building queries, and it uses this Query class. There is no linkage via AbstractProperty
 *     or anything fancy like that - the dialog just creates and manipulates Query objects directly.
 *     Looking at this legacy code, I really think this class should be an AbstractProperty that backs
 *     onto a custom QueryFormField. This is more work than I anticipated for this 2.0 release, but I can't
 *     leave it in its current state, as it's old and crusty. So, here's the plan for 2.0:
 * </p>
 * <ul>
 *     <li>Refactor Query to be an AbstractProperty subclass, probably named QueryProperty.
 *     <li>Create a QueryFormField that can edit QueryProperty instances.
 *     <li>Refactor (or just rewrite probably) the existing Query dialog to use QueryProperty and QueryFormField.
 *     <li>I haven't looked at "Template" yet, but it will likely be the same exact problem with the same solution.
 * </ul>
 *
 * @author scorbett
 * @since Snotes 1.0, heavily rewritten for Snotes 2.0
 */
public final class Query {

    private String name;
    private String yearFilter;
    private String monthFilter;
    private String dayFilter;
    private String textFilter;
    private boolean isCaseSensitive;
    private String tagString;

    public Query() {
        this("unnamed query");
    }

    public Query(String name) {
        setDefaults(name);
    }

    public Query(Query other) {
        copyFrom(other);
    }

    public void copyFrom(Query other) {
        this.name = other.name;
        this.yearFilter = other.yearFilter;
        this.monthFilter = other.monthFilter;
        this.dayFilter = other.dayFilter;
        this.textFilter = other.textFilter;
        this.isCaseSensitive = other.isCaseSensitive;
        this.tagString = other.tagString;
    }

    public String getName() {
        return name;
    }

    public String getYearFilter() {
        return yearFilter;
    }

    public String getMonthFilter() {
        return monthFilter;
    }

    public String getDayFilter() {
        return dayFilter;
    }

    public String getTextFilter() {
        return textFilter;
    }

    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYearFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            yearFilter = "";
        }
        else if (filter.length() != 4) {
            throw new IllegalArgumentException("Invalid year filter: " + filter);
        }
        yearFilter = filter;
    }

    public void setMonthFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            monthFilter = "";
        }
        else if (filter.length() != 2) {
            throw new IllegalArgumentException("Invalid month filter: " + filter);
        }
        monthFilter = filter;
    }

    public void setDayFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            dayFilter = "";
        }
        else if (filter.length() != 2) {
            throw new IllegalArgumentException("Invalid day filter: " + filter);
        }
        dayFilter = filter;
    }

    public void setTextFilter(String filter) {
        textFilter = filter == null ? "" : filter.trim();
    }

    public void setIsCaseSensitive(boolean is) {
        isCaseSensitive = is;
    }

    public String getTagString() {
        return tagString;
    }

    public void setTagString(String t) {
        tagString = t == null ? "" : t.trim();

    }

    public void setTags(List<Tag> list) {
        tagString = "";
        boolean delimiterRequired = false;
        for (Tag tag : list) {
            if (delimiterRequired) {
                tagString += " ";
            }
            else {
                delimiterRequired = true;
            }
            tagString += tag.getTag();
        }
    }

    public List<Tag> getTags() {
        List<Tag> list = new ArrayList<>();
        if (tagString == null || tagString.trim().isEmpty()) {
            return list;
        }
        String[] tags = tagString.split(" ");
        for (String tag : tags) {
            list.add(new Tag(tag));
        }
        return list;
    }

    public void loadFromProps(Properties props, String prefix) {
        setDefaults(name);
        String pfx = prefix == null ? "" : prefix;
        name = props.getString(pfx + "name", name);
        yearFilter = props.getString(pfx + "yearFilter", yearFilter);
        monthFilter = props.getString(pfx + "monthFilter", monthFilter);
        dayFilter = props.getString(pfx + "dayFilter", dayFilter);
        textFilter = props.getString(pfx + "textFilter", textFilter);
        isCaseSensitive = props.getBoolean(pfx + "isCaseSensitive", isCaseSensitive);
        tagString = props.getString(pfx + "tagString", tagString);
    }

    public void saveToProps(Properties props, String prefix) {
        String pfx = prefix == null ? "" : prefix;
        props.setString(pfx + "name", name);
        props.setString(pfx + "yearFilter", yearFilter);
        props.setString(pfx + "monthFilter", monthFilter);
        props.setString(pfx + "dayFilter", dayFilter);
        props.setString(pfx + "textFilter", textFilter);
        props.setBoolean(pfx + "isCaseSensitive", isCaseSensitive);
        props.setString(pfx + "tagString", tagString);
    }

    private void setDefaults(String name) {
        this.name = name;
        this.yearFilter = "";
        this.monthFilter = "";
        this.dayFilter = "";
        this.textFilter = "";
        this.isCaseSensitive = false;
        this.tagString = "";
    }

}