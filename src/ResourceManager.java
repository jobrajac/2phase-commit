public class ResourceManager extends Client {
    String TM_HOST;
    int TM_PORT;
    public ResourceManager(int PORT, String TM_HOST, int TM_PORT) {
        super(PORT);
        this.TM_HOST = TM_HOST;
        this.TM_PORT = TM_PORT;
    }
    @Override
    public void handleMessage(Message msg) {
        switch (msg.getMessage()) {
            case START:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.transaction_id, msg.client_id, messageTypes.START_OK));
                break;
            case COMMIT:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.transaction_id, msg.client_id, messageTypes.COMMIT_OK));
                break;
            case ROLLBACK:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.transaction_id, msg.client_id, messageTypes.ROLLBACK_OK));
                break;
            case UNDO:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.transaction_id, msg.client_id, messageTypes.UNDO_OK));
        }
    }
}
