import java.io.Serializable;

public class Message implements Serializable {
    int transaction_id;
    Participant client_id;
    public messageTypes message;
    public Message(int trans_id, Participant client_id, messageTypes message) {
        this.transaction_id = trans_id;
        this.client_id = client_id;
        this.message = message;
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
