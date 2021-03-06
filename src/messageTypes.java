// Enum to keep track of all acceptable messages
public enum messageTypes {
    TRANSACTION,
    START,
    START_OK,
    START_FAIL,
    COMMIT,
    COMMIT_OK,
    COMMIT_FAIL,
    ROLLBACK,
    ROLLBACK_OK,
    ROLLBACK_FAIL,
    UNDO,
    UNDO_OK,
    UNDO_FAIL,
    // For debugging:
    NO_REPLY,
}
