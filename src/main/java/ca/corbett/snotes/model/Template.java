package ca.corbett.snotes.model;

import ca.corbett.extras.properties.Properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a template that can be used as a starting point for quickly creating a new
 * Snote, with certain parameters already filled out.
 *
 * @author scorbett
 * @since Snotes 1.0, heavily rewritten for Snotes 2.0
 */
public final class Template {

    public enum DateOption {
        NONE("No date"),
        TODAY("Today"),
        YESTERDAY("Yesterday"),
        CUSTOM("Custom...");

        private final String label;

        DateOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static DateOption fromLabel(String label) {
            for (DateOption option : DateOption.values()) {
                if (option.label.equals(label)) {
                    return option;
                }
            }
            return null;
        }
    }

    public enum RowCountOption {
        ALL("All"),
        MOST_RECENT1("1 most recent"),
        MOST_RECENT3("3 most recent"),
        MOST_RECENT5("5 most recent"),
        MOST_RECENT10("10 most recent");

        private final String label;

        private RowCountOption(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static RowCountOption fromLabel(String label) {
            for (RowCountOption option : RowCountOption.values()) {
                if (option.label.equals(label)) {
                    return option;
                }
            }
            return null;
        }
    }

    private String name;
    private DateOption dateOption;
    private YMDDate customDate;
    private final List<Tag> tagList = new ArrayList<>();
    private Query contextQuery;
    private RowCountOption contextRowCount;
    private boolean useDefaultSaveLocation;
    private String customSavePath;
    private String customSaveFilename;

    public Template() {
        this("unnamed template");
    }

    public Template(String name) {
        setDefaults(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateOption getDateOption() {
        return dateOption;
    }

    public void setDateOption(DateOption option) {
        dateOption = option;
    }

    public YMDDate getCustomDate() {
        return customDate;
    }

    public void setCustomDate(YMDDate custom) {
        customDate = custom;
    }

    public boolean hasCustomDate() {
        return customDate != null;
    }

    public RowCountOption getContextRowCount() {
        return contextRowCount;
    }

    public void setContextRowCount(RowCountOption option) {
        contextRowCount = option;
    }

    public void addTag(Tag tag) {
        if (!tagList.contains(tag)) {
            tagList.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        tagList.remove(tag);
    }

    public List<Tag> getTags() {
        List<Tag> copy = new ArrayList<>();
        copy.addAll(tagList);
        return copy;
    }

    public void setTagsAsSpaceSeparatedString(String tagString) {
        tagList.clear();
        if (tagString != null && !tagString.isEmpty()) {
            String[] tags = tagString.split(" ");
            for (String t : tags) {
                addTag(new Tag(t));
            }
        }
    }

    public String getTagsAsSpaceSeparatedString() {
        String tagString = "";
        if (!tagList.isEmpty()) {
            for (Tag tag : tagList) {
                tagString += tag.getTag() + " ";
            }
        }
        return tagString.trim();
    }

    public boolean hasContextQuery() {
        return contextQuery != null;
    }

    public Query getContextQuery() {
        return contextQuery;
    }

    public void setContextQuery(Query query) {
        contextQuery = query;
    }

    public boolean useDefaultSaveLocation() {
        return useDefaultSaveLocation;
    }

    public void setUseDefaultSaveLocation(boolean val) {
        useDefaultSaveLocation = val;
    }

    public String getCustomSavePath() {
        return customSavePath;
    }

    public String getCustomSaveFilename() {
        return customSaveFilename;
    }

    public File getCustomSavePathAsFile() {
        File staticDir = AppPreferences.getDataDirectory();
        if (customSavePath == null || customSavePath.isEmpty()) {
            return staticDir;
        }
        return new File(staticDir, customSavePath);
    }

    public void setCustomSavePath(String path) {
        customSavePath = path == null ? "" : path.trim();
    }

    public void setCustomSaveFilename(String name) {
        customSaveFilename = name == null ? "" : name.trim();
    }

    public File getCustomSaveFile() {
        if (customSaveFilename == null || customSaveFilename.isEmpty()) {
            return null;
        }
        File basePath = AppPreferences.getDataDirectory();
        if (customSavePath != null && !customSavePath.isEmpty()) {
            basePath = new File(basePath, customSavePath);
        }
        return new File(basePath, customSaveFilename);
    }

    public boolean hasCustomSavePath() {
        return customSavePath != null && !customSavePath.trim().isEmpty();
    }

    public boolean hasCustomSaveFilename() {
        return customSaveFilename != null && !customSaveFilename.trim().isEmpty();
    }

    public void loadFromProps(Properties props, String prefix) {
        setDefaults(name);
        String pfx = (prefix == null) ? "" : prefix;
        name = props.getString(pfx + "name", name);
        dateOption = DateOption.valueOf(props.getString(pfx + "dateOption", "NONE"));
        String customDateStr = props.getString(pfx + "customDate", "");
        if (!customDateStr.isEmpty() && YMDDate.isValidYMD(customDateStr)) {
            customDate = new YMDDate(customDateStr);
        }
        customDate = new YMDDate(props.getString(pfx + "custoMDate", ""));
        setTagsAsSpaceSeparatedString(props.getString(pfx + "tags", ""));
        boolean hasContextQuery = props.getBoolean(pfx + "hasContextQuery", false);
        if (hasContextQuery) {
            contextQuery = new Query();
            contextQuery.loadFromProps(props, pfx + "contextQuery.");
            contextRowCount = RowCountOption.valueOf(props.getString(pfx + "contextRowCount", "ALL"));
        }
        useDefaultSaveLocation = props.getBoolean(pfx + "useDefaultSaveLocation", useDefaultSaveLocation);
        customSavePath = props.getString(pfx + "customSavePath", "");
        customSaveFilename = props.getString(pfx + "customSaveFilename", "");
    }

    public void saveToProps(Properties props, String prefix) {
        String pfx = (prefix == null) ? "" : prefix;
        props.setString(pfx + "name", name);
        props.setString(pfx + "dateOption", dateOption.name());
        props.setString(pfx + "customDate", customDate == null ? "" : customDate.toString());
        props.setString(pfx + "tags", getTagsAsSpaceSeparatedString());
        props.setBoolean(pfx + "hasContextQuery", hasContextQuery());
        if (hasContextQuery()) {
            contextQuery.saveToProps(props, pfx + "contextQuery.");
            props.setString(pfx + "contextRowCount", contextRowCount.name());
        }
        props.setBoolean(pfx + "useDefaultSaveLocation", useDefaultSaveLocation);
        props.setString(pfx + "customSavePath", customSavePath);
        props.setString(pfx + "customSaveFilename", customSaveFilename);
    }

    private void setDefaults(String name) {
        this.name = name;
        dateOption = DateOption.NONE;
        customDate = null;
        tagList.clear();
        contextQuery = null;
        contextRowCount = RowCountOption.ALL;
        useDefaultSaveLocation = true;
        customSavePath = "";
        customSaveFilename = "";
    }

}