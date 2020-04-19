import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TM_State implements Serializable {
    private int transaction_id;
    private Map<String, Double> states;
    private String data;

    private boolean debug = false;
    private HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers = null;

    TM_State(int transaction_id, Map<String, Double> states, String data) {
        this.transaction_id = transaction_id;
        this.states = states;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    double getState(String name) {
        return states.get(name);
    }
    void setState(String name, double step) {
        states.put(name, step);
    }

    int getTransaction_id() {
        return transaction_id;
    }


    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    HashMap<String, HashMap<messageTypes, messageTypes>> getDebugAnswers() {
        return debugAnswers;
    }

    void setDebugAnswers(HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers) {
        this.debugAnswers = debugAnswers;
    }

}
