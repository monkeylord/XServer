package monkeylord.XServer.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Xlog {
    PrintWriter xlog;
    int count = 0;

    public Xlog(String logfile) throws IOException {
        xlog = new PrintWriter(new FileWriter(logfile, false));
    }

    public void log(String line) {
        xlog.println(line);
        if (count++ > 20) xlog.flush();
    }

    public void log(String line, boolean flush) {
        xlog.println(line);
        if (flush) xlog.flush();
    }
}