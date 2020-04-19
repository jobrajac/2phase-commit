import java.io.Serializable;
import java.util.Map;

public class TM_State implements Serializable {
    private int transaction_id;
    private Map<String, Double> states;
    TM_State(int transaction_id, Map<String, Double> rmSteps) {
        this.transaction_id = transaction_id;
        this.states = rmSteps;
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
}
