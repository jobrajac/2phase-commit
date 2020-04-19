import java.io.Serializable;
import java.util.HashMap;

public class Message implements Serializable {
    private int transaction_id;
    private Participant client_id;
    public messageTypes message;
    public String data = null;
    private boolean debug = false;
    private HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = null;

    public Message(int trans_id, Participant client_id, messageTypes message) {
        this.transaction_id = trans_id;
        this.client_id = client_id;
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public HashMap<String, HashMap<messageTypes, messageTypes>> getDebugAnswers() {
        return debugAnswers;
    }

    public void setDebugAnswers(HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers) {
        this.debugAnswers = debugAnswers;
    }

    int getTransaction_id() {
        return transaction_id;
    }

    Participant getClient_id() {
        return client_id;
    }

    public messageTypes getMessage() {
        return message;
    }
}
