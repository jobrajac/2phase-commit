import java.io.Serializable;

public class Message implements Serializable {
    public int transaction_id;
    public String client_id;
    public messageTypes message;
    public Message(int trans_id, String client_id, messageTypes message) {
        this.transaction_id = trans_id;
        this.client_id = client_id;
        this.message = message;
    }

    public int getTransaction_id() {
        return transaction_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public messageTypes getMessage() {
        return message;
    }
}
