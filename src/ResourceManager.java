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
        // For debugging. Ekko the value specified in debug.
        // If the value is NOREPLY don't reply.
        if(msg.getDebug() != null) {
            if(msg.getDebug() != messageTypes.NO_REPLY){
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), msg.getDebug()));
            }
        }
        switch (msg.getMessage()) {
            case START:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.START_OK));
                break;
            case COMMIT:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_OK));
                break;
            case ROLLBACK:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_OK));
                break;
            case UNDO:
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_OK));
        }
    }
}
