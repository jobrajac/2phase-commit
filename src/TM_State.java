import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Class that the TaskManager uses to track the state of a transaction
class TM_State implements Serializable {
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


    String getData() {
        return data;
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


    boolean isDebug() {
        return debug;
    }


    void setDebug(boolean debug) {
        this.debug = debug;
    }


    HashMap<String, HashMap<messageTypes, messageTypes>> getDebugAnswers() {
        return debugAnswers;
    }


    void setDebugAnswers(HashMap<String, HashMap<messageTypes, messageTypes>> debugAnswers) {
        this.debugAnswers = debugAnswers;
    }

}
