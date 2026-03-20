package ca.corbett.snotes.model;

/**
 * DateTag is a special case of Tag, where the Tag value is always represented
 * in YMDDate format. If you supply some value that does not conform to the
 * yyyy-MM-dd format, then a default value of today's date will be used.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 1.0
 */
public final class DateTag extends Tag {

    final YMDDate dateTag;

    /**
     * You can create a DateTag by providing a String in yyyy-mm-dd format.
     * A String in any other format will be ignored, and today's date will
     * be used instead.
     *
     * @param val A yyyy-MM-dd format string. If invalid, today's date is used.
     */
    public DateTag(String val) {
        this(new YMDDate(val));
    }

    /**
     * You can create a DateTag by providing a YMDDate object. If the
     * object is null, it will be ignored and today's date will be used.
     */
    public DateTag(YMDDate date) {
        super(date == null ? new YMDDate().toString() : date.toString());
        dateTag = new YMDDate(this.tag);
    }

    /**
     * You can create a DateTag based on some other DateTag.
     */
    public DateTag(DateTag other) {
        this(other == null ? new YMDDate() : other.getDate());
    }

    /**
     * Returns the YMDDate object that this tag represents.
     */
    public YMDDate getDate() {
        return dateTag; // it's immutable, so we can just return the reference
    }

    @Override
    public int compareTo(Tag other) {
        if (other instanceof DateTag otherDateTag) {
            return dateTag.compareTo(otherDateTag.dateTag);
        }
        else {
            return super.compareTo(other);
        }
    }
}