package ma.atlasrh.exception;

/**
 * Exception lancée lorsqu'une ressource demandée n'est pas trouvée.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " avec l'id " + id + " introuvable");
    }
}
