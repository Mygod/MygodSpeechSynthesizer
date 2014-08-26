package tk.mygod.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Mygod
 */
public final class FileUtils {
    private FileUtils() {
        throw new AssertionError();
    }

    public static String getTempFileName() {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    }
}
