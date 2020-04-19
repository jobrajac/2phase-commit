import java.io.Serializable;

public class RM_State implements Serializable {
    private int transaction_id;
    private int state;
    private String data;

    RM_State(int transaction_id, String data) {
        this.transaction_id = transaction_id;
        this.state = 0;
        this.data = data;
    }

    public int getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(int transaction_id) {
        this.transaction_id = transaction_id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getData() {
        return data;
    }
}
