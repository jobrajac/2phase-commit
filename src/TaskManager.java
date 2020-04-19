import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class TaskManager extends Client {
    private Participant[] resourceManagers;
    private TM_State saved_state;
    private boolean failed = false;
    private TaskManager(int PORT, Participant[] resourceManagers){
        super(PORT);
        this.resourceManagers = resourceManagers;
        // Check if there is anything saved
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public TM_State getSaved_state() {
        return saved_state;
    }

    public void setSaved_state(TM_State saved_state) {
        this.saved_state = saved_state;
    }

    @Override
    void handleMessage(Message msg) {
        try {
//            Thread.sleep(3000);
        }
        catch (Exception e) {

        }
        switch (msg.getMessage()){
            case TRANSACTION:
                transaction();
                break;
            case START_OK:
                startOk(msg);
                break;
            case START_FAIL:
                startFail(msg);
                break;
            case COMMIT_OK:
                commitOk(msg);
                break;
            case COMMIT_FAIL:
                commitFail(msg);
                break;
            case ROLLBACK_OK:
                rollbackOK(msg);
                break;
            case ROLLBACK_FAIL:
                rollbackFail(msg);
                break;
            case UNDO_OK:
                undoOk(msg);
                break;
            case UNDO_FAIL:
                undoFail(msg);
                break;
            default:
                break;

        }
    }


    private void transaction() {
        System.out.println("Transaction received.");
        if(getSaved_state() == null) {
            // TODO bytt ut 1 med faktisk id'er.
            HashMap<String, Double> states = new HashMap<>();
            states.put(Participant.TM.name(), 0.0);
            for (Participant resourceManager: resourceManagers) {
                states.put(resourceManager.name(), 0.0);
            }
            TM_State new_trans = new TM_State(1, states);
            setSaved_state(new_trans);

            for (Participant resourceManager : resourceManagers) {
                start(resourceManager, getSaved_state().getTransaction_id());
            }
        }
        else {
            // Should be added to queue, but will not be implemented in this program.
            // TODO throw error
        }
    }

    private void start(Participant who, int trans_id) {
        Message start = new Message(trans_id, who, messageTypes.START);
        send(who, start);
        setState(who, 0.1);
    }


    private void startOk(Message msg) {
        Participant who = msg.getClient_id();
        if(getSaved_state().getState(who.name()) == 0.1) {
            setState(who, 0.2);
        }
        // Sjekk om tilstand = 0.2 for alle rm
        for (Participant resourceManager1 : resourceManagers) {
            if (getSaved_state().getState(resourceManager1.name()) < 0.2) {
                System.out.println("Ikke alle har tilstand 0.2, " + resourceManager1.name());
                return;
            }
        }
        // Alle er 0.2
        System.out.println("Alle har rapportert med startOK. Går til commit.");
        setState(Participant.TM, 1);
        for (Participant resourceManager : resourceManagers) {
            commit(resourceManager, getSaved_state().getTransaction_id());
        }
    }


    private void startFail(Message msg) {
        // Update TM to state 2.0 - rollback
        setState(Participant.TM, 2.0);
        setFailed(true);
        // Update the node that failed with msg received.
        setState(msg.getClient_id(), 2.2);
        // Log error, and send rollback to others.
        // TODO log error
        for (Participant resourceManager : resourceManagers) {
            // Ikke meld fra til noden om den allerede har fått beskjed, eller også rapporterte feil.
            // Den trenger beskjed om tilstanden er under 2.2
            if (getSaved_state().getState(resourceManager.name()) < 2.2) {
                rollback(resourceManager, msg.getTransaction_id());
            }
        }
        // Sjekk om alle har tilstand 2.2, isåfall -> ferdig
        for(Participant resourceManager: resourceManagers) {
            if(getSaved_state().getState(resourceManager.name()) != 2.2) {
                return;
            }
        }
        // Alle har rullet tilbake eller rapportert feil på start.
        setState(Participant.TM, 4);
        transactionComplete(msg);
    }


    private void commit(Participant who, int trans_id){
        // Send commit.
        Message commit = new Message(trans_id, who, messageTypes.COMMIT);
        send(who, commit);
        setState(who, 1.1);
    }


    private void commitOk(Message msg) {
        // Hvis transaksjon har feilet, gjør ingenting
        if(!isFailed()) {
            Participant who = msg.getClient_id();
            if(getSaved_state().getState(who.name()) == 1.1) {
                setState(who, 1.2);
            }
            // Sjekk om tilstand = 1.2 for alle rm
            for (Participant resourceManager : resourceManagers) {
                if (getSaved_state().getState(resourceManager.name()) != 1.2) {
                    return;
                }
            }
            // Alle er 1.2
            setState(Participant.TM, 4);
            transactionComplete(msg);
        }
    }


    private void commitFail(Message msg) {
        // Update TM to state 3.0 - undo
        // TODO log error
        setFailed(true);
        setState(Participant.TM, 3.0);
        // Update the node that failed.
        setState(msg.getClient_id(), 3.2);
        // Log error, and send undo to others.

        for (Participant resourceManager : resourceManagers) {
            // Ikke meld fra til noden om den allerede har fått beskjed, eller også rapporterte feil.
            // Den trenger beskjed om tilstanden er under 3.2
            if (getSaved_state().getState(resourceManager.name()) < 3.2) {
                undo(resourceManager, msg.getTransaction_id());
            }
        }
        // Sjekk om alle har tilstand 3.2, isåfall -> ferdig
        for(Participant resourceManager: resourceManagers) {
            if(getSaved_state().getState(resourceManager.name()) != 3.2) {
                return;
            }
        }
        // Alle har undoet eller rapportert feil på commit.
        transactionComplete(msg);
    }


    private void rollback(Participant who, int trans_id) {
        // Send rollback.
        Message rollback = new Message(trans_id, who, messageTypes.ROLLBACK);
        send(who, rollback);
        setState(who, 2.1);
    }


    private void rollbackOK(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 2.1) {
            setState(who, 2.2);
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for (Participant resourceManager : resourceManagers) {
            if (getSaved_state().getState(resourceManager.name()) != 2.2) {
                return;
            }
        }
        // Alle er 2.2
        setState(Participant.TM, 4);
        // TODO loggfør rollback.
        transactionComplete(msg);
    }


    private void rollbackFail(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 2.1) {
            setState(who, 2.2);
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 2.2) {
                return;
            }
        }
        // Alle er 2.2
        setState(Participant.TM, 4);
        // TODO loggfør rollback FAIL!.
        transactionComplete(msg);
    }


    private void undo(Participant who, int trans_id){
        // Send undo.
        Message undo = new Message(trans_id, who, messageTypes.UNDO);
        send(who, undo);
        setState(who, 3.1);
    }


    void undoOk(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 3.1) {
            setState(who, 3.2);
        }
        // Sjekk om tilstand = 3.2 for alle rm
        for (Participant resourceManager : resourceManagers) {
            if (getSaved_state().getState(resourceManager.name()) != 3.2) {
                return;
            }
        }

        // Alle er 3.2
        setState(Participant.TM, 4);
        // TODO loggfør undo.
        transactionComplete(msg);
    }


    void undoFail(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 3.1) {
            setState(who, 3.2);
        }
        // Sjekk om tilstand = 3.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 3.2) {
                return;
            }
        }

        // Alle er 3.2
        setState(Participant.TM, 4);
        // TODO loggfør rollback FAIL!.
        transactionComplete(msg);
    }


    private void transactionComplete(Message msg) {
        if(isFailed()) {
            // Rapporter
            System.out.println("Transaction failed");
        }
        else {
            // Rapporter
            System.out.println("Transaction complete");
        }
    }


    private void saveState() {
        saveFile(getSaved_state(), "saved_states/tm/" + getSaved_state().getTransaction_id() + ".ser");
    }


    private void setState(Participant who, double newStep) {
        TM_State tmp = getSaved_state();
        tmp.setState(who.name(), newStep);
        setSaved_state(tmp);
        saveState();
    }


    private void send(Participant who, Message message) {
        // Try catch this
        sendMessage(who.getHost(), who.getPort(), message);
    }

    public static void main(String[] args) {
        Participant[] rm = new Participant[]{Participant.A, Participant.B};
        Thread tm = new Thread(new TaskManager(Participant.TM.getPort(), rm));
        tm.start();
        Thread a = new Thread(new ResourceManager(Participant.A.getPort(), Participant.TM.getHost(), Participant.TM.getPort()));
        a.start();
        Thread b = new Thread(new ResourceManager(Participant.B.getPort(), Participant.TM.getHost(), Participant.TM.getPort()));
        b.start();
        try {
            // Create the socket
            Socket clientSocket = new Socket(Participant.TM.getHost(), Participant.TM.getPort());
            // Create the input & output streams to the server
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            Message send = new Message(1, Participant.OUTSIDE, messageTypes.TRANSACTION);
            outputStream.writeObject(send);
            outputStream.flush();
            outputStream.close();
            clientSocket.close();

        } catch (Exception e) {
            System.err.println("Client Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
        }
    }
}
