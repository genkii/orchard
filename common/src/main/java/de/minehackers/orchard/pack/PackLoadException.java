package de.minehackers.orchard.pack;

/// Thrown whenever a pack - or something inside it - fails to load.
public final class PackLoadException extends RuntimeException {

    public PackLoadException(String message) {
        super(message);
    }

    public PackLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
