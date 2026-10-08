package dev.ardapeker.fairshare.error;

/** Turned into a 404 response by {@link ApiExceptionHandler}. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
