package ma.atlasrh.exception;

/**
 * Exception lancée lors d'un conflit métier (unicité, contrat en cours, etc.).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
