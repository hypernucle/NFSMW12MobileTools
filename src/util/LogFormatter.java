package util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

// https://stackoverflow.com/a/39034822
public class LogFormatter extends Formatter {

//  private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	private static final String PATTERN = "HH:mm:ss.SSS";

    @Override
    public String format(final LogRecord logRecord) {
        return String.format(
                "%1$s : %2$s -> %3$s%n",
                new SimpleDateFormat(PATTERN).format(
                        new Date(logRecord.getMillis())),
                logRecord.getLevel().getName(), formatMessage(logRecord));
    }
}
