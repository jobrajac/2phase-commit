
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
        switch (msg.getMessage()){
            case TRANSACTION:
                transaction();
                break;
            case START_OK:
                startOk(msg);
                break;
            case START_FAIL:
                startFail(msg);
            default:
                break;

        }
    }
    private void transaction() {
        if(getSaved_state() == null) {
            setState(Participant.TM, 1.0);
            // Incoming transaction that should be distributed.
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
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 0.2) {
                break;
            }
            // Alle er 0.2
            setState(Participant.TM, 1);
            for (Participant resourceManager : resourceManagers) {
                commit(resourceManager, getSaved_state().getTransaction_id());
            }
        }
    }
    void startFail(Message msg) {
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
                break;
            }
            // Alle har rullet tilbake eller rapportert feil på start.
            transactionComplete(msg);
        }
    }
    void commit(Participant who, int trans_id){
        // Send commit.
        Message commit = new Message(trans_id, who, messageTypes.COMMIT);
        send(who, commit);
        setState(who, 1.1);
    }
    void commitOk(Message msg) {
        // Hvis transaksjon har feilet, gjør ingenting
        if(!isFailed()) {
            Participant who = msg.getClient_id();
            if(getSaved_state().getState(who.name()) == 1.1) {
                setState(who, 1.2);
            }
            // Sjekk om tilstand = 1.2 for alle rm
            for(int i=0;i<resourceManagers.length;i++) {
                if(getSaved_state().getState(resourceManagers[i].name()) != 1.2) {
                    break;
                }
                // Alle er 1.2
                setState(Participant.TM, 4);
                transactionComplete(msg);
            }
        }
    }
    void commitFail(Message msg) {
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
                break;
            }
            // Alle har undoet eller rapportert feil på commit.
            transactionComplete(msg);
        }
    }
    void rollback(Participant who, int trans_id) {
        // Send rollback.
        Message rollback = new Message(trans_id, who, messageTypes.ROLLBACK);
        send(who, rollback);
        setState(who, 2.1);
    }
    void rollbackOK(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 2.1) {
            setState(who, 2.2);
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 2.2) {
                break;
            }
            // Alle er 2.2
            setState(Participant.TM, 4);
            // TODO loggfør rollback.
            transactionComplete(msg);
        }
    }
    void rollbackFail(Message msg) {
        Participant who = msg.getClient_id();
        // TODO trenger jeg denne sjekken her?
        if(getSaved_state().getState(who.name()) == 2.1) {
            setState(who, 2.2);
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 2.2) {
                break;
            }
            // Alle er 2.2
            setState(Participant.TM, 4);
            // TODO loggfør rollback FAIL!.
            transactionComplete(msg);
        }
    }
    void undo(Participant who, int trans_id){
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
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state().getState(resourceManagers[i].name()) != 3.2) {
                break;
            }
            // Alle er 3.2
            setState(Participant.TM, 4);
            // TODO loggfør undo.
            transactionComplete(msg);
        }
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
                break;
            }
            // Alle er 2.2
            setState(Participant.TM, 4);
            // TODO loggfør rollback FAIL!.
            transactionComplete(msg);
        }
    }
    void transactionComplete(Message msg) {
        setState(Participant.TM, 4.0);
        System.out.println("Transaction failed");
        if(isFailed()) {
            // Rapporter
        }
        else {
            // Rapporter
        }
    }
    private void saveState() {
        saveFile(getSaved_state(), "saved_states/tm/" + getSaved_state().getTransaction_id() + ".ser");
    }
    private void setState(Participant who, double newStep) {
        System.out.println("Oppdaterer tilstand for " + who.name());
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
        Participant[] all = new Participant[]{Participant.TM, Participant.A, Participant.B};
        Thread tm = new Thread(new TaskManager(Participant.TM.getPort(), all));
        tm.start();
        Thread a = new Thread(new ResourceManager(Participant.A.getPort(), Participant.TM.getHost(), Participant.TM.getPort()));
        a.start();
        Thread b = new Thread(new ResourceManager(Participant.B.getPort(), Participant.TM.getHost(), Participant.TM.getPort()));
        b.start();
    }
}
