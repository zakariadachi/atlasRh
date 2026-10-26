package ma.atlasrh.exception;

/**
 * Exception lancée lorsqu'une règle métier est violée (ex: contrat déjà terminé).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
