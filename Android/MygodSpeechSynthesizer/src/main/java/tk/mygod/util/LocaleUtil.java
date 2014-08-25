package tk.mygod.util;

import android.text.TextUtils;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author   Mygod
 */
public final class LocaleUtil {
    private static final Pattern localeMatcher = Pattern.compile("^([^_]*)(_([^_]*)(_#(.*))?)?$");

    private LocaleUtil() {
        throw new AssertionError();
    }

    public static Locale parseLocale(String value) {
        Matcher matcher = localeMatcher.matcher(value.replace('-', '_'));
        return matcher.find()
                ? TextUtils.isEmpty(matcher.group(5))
                    ? TextUtils.isEmpty(matcher.group(3))
                        ? TextUtils.isEmpty(matcher.group(1)) ? null : new Locale(matcher.group(1))
                        : new Locale(matcher.group(1), matcher.group(3))
                    : new Locale(matcher.group(1), matcher.group(3), matcher.group(5))
                : null;
    }

    public static class DisplayNameComparator implements Comparator<Locale> {
        @Override
        public int compare(Locale lhs, Locale rhs) {
            return lhs.getDisplayName().compareTo(rhs.getDisplayName());
        }
    }
}
