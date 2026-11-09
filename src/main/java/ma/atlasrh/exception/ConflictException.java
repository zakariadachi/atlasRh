package ma.atlasrh.exception;

// Exception en cas de conflit métier (code HTTP 409)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
