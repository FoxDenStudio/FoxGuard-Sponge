package net.foxdenstudio.sponge.foxguard.plugin.util;

public class FoxException extends Exception {

    public FoxException() {
    }

    public FoxException(String message) {
        super(message);
    }

    public FoxException(String message, Throwable cause) {
        super(message, cause);
    }

    public FoxException(Throwable cause) {
        super(cause);
    }

    public FoxException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
