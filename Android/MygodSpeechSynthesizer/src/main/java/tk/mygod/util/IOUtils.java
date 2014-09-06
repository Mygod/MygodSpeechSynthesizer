package tk.mygod.util;

import java.io.*;

/**
 * @author Mygod
 */
public class IOUtils {
    private static final int BUF_SIZE = 0x1000; // 4K

    public static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static String readAllText(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(inputStreamReader = new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            return builder.toString();
        } finally {
            if (inputStreamReader != null) try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
