package tk.mygod.util;

import java.io.*;
import java.util.Scanner;

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
        Scanner scanner = new Scanner(stream).useDelimiter("\\a");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
