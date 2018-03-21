package au.com.smarttrace.beacon.net.exception;

/**
 * Created by beou on 3/21/18.
 */

public class ParsingException extends RuntimeException {
    public ParsingException(String message) {
        super("[ParsingException]" + message);
    }
}
