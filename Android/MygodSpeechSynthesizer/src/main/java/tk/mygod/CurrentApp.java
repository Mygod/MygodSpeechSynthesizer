package tk.mygod;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipFile;

/**
 * @author Mygod
 */
public final class CurrentApp {
    private CurrentApp() {
        throw new AssertionError();
    }

    /**
     * Get version name for the current package.
     * @param context Current context.
     * @return Version name if succeeded, null otherwise.
     */
    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date getBuildTime(Context context) {
        ZipFile file = null;
        try {
            file = new ZipFile(context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir);
            return new Date(file.getEntry("META-INF/MANIFEST.MF").getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return new Date();
        } finally {
            if (file != null) try {
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
