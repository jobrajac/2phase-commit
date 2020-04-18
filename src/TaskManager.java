import java.awt.*;

public class TaskManager extends Client {
    String A_HOST;
    int A_PORT;
    String B_HOST;
    int B_PORT;

    public TaskManager(int PORT, String A_HOST, int A_PORT, String B_HOST, int B_PORT){
        super(PORT);
        this.A_HOST = A_HOST;
        this.A_PORT = A_PORT;
        this.B_HOST = B_HOST;
        this.B_PORT = B_PORT;
        // Check if there is anything saved
    }
    @Override
    void handleMessage(Message msg) {
        switch (msg.getMessage()){
            case TRANSACTION:
                // Incoming transaction that should be distributed.
                startTransaction();
                break;
            default:
                break;

        }
    }
    private void startTransaction() {
        Message startA = new Message(1, "A", messageTypes.START);
        sendMessage(A_HOST, A_PORT, startA);
        Message startB = new Message(1, "B", messageTypes.START);
        sendMessage(B_HOST, B_PORT, startB);
    }
    public static void main(String[] args) {
        TaskManager tm = new TaskManager(9000, "localhost", 9001, "localhost", 9002);
    }
}
