package dev.ardapeker.fairshare.error;

/** A request that is well-formed but breaks a rule (unknown member, duplicate name...). Becomes a 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
