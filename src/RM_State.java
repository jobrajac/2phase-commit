import java.io.Serializable;

class RM_State implements Serializable {
    private int transaction_id;
    private int state;
    private String data;

    RM_State(int transaction_id, String data) {
        this.transaction_id = transaction_id;
        this.state = 0;
        this.data = data;
    }

    int getTransaction_id() {
        return transaction_id;
    }

    int getState() {
        return state;
    }

    void setState(int state) {
        this.state = state;
    }

    String getData() {
        return data;
    }
}
