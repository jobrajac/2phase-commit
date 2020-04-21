import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

// Class that handles the distribution of a transaction to different resourcemanagers.
class TaskManager extends Client {
    private Participant[] resourceManagers;
    private HashMap<Integer, TM_State> saved_states;
    private boolean failed = false;
    private final String stateFolderPath = "saved_states/tm/";

    TaskManager(int PORT, Participant[] resourceManagers){
        super(PORT, "logs/tm/");
        this.resourceManagers = resourceManagers;
        // Check if there is anything saved.
        this.saved_states = readSavedStates(new File(stateFolderPath));
        if(this.saved_states != null) {
            resumeSavedTransactions();
        }
    }

    public boolean isFailed() {
        return failed;
    }


    public void setFailed(boolean failed) {
        this.failed = failed;
    }


    private TM_State getSaved_state(int id) {
        return saved_states.get(id);
    }


    private void setSaved_state(TM_State saved_state, int id) {
        saved_states.put(id, saved_state);
    }


    @Override
    void handleMessage(Message msg) {
            appendLog("Received message " + msg.getMessage() + " from " + msg.getClient_id(), msg.getTransaction_id());
            switch (msg.getMessage()) {
                case TRANSACTION:
                    transaction(msg);
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


    private HashMap<Integer, TM_State> readSavedStates(final File folder) {
        HashMap<Integer, TM_State> saved_states = new HashMap<>();
        try {
            for (final File fileEntry : folder.listFiles()) {
                try {
                    FileInputStream is = new FileInputStream(fileEntry);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    TM_State state = (TM_State) ois.readObject();
                    saved_states.put(state.getTransaction_id(), state);
                    ois.close();
                    is.close();
                }
                catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }

            }
        }
        catch (NullPointerException e) {
            return null;
        }
        return saved_states;
    }


    private void resumeSavedTransactions(){
        for (HashMap.Entry<Integer, TM_State> entry : saved_states.entrySet()) {
            TM_State transaction = entry.getValue();
            double tm_value = transaction.getState(Participant.TM.name());
            int id = entry.getKey();

            // TM has not received START_OK / START FAIL from all
            if(tm_value == 0) {
                // If there is no data in the transaction, jump to end.
                if(transaction.getData() == null) {
                    appendLog("No data in transaction." , transaction.getTransaction_id());
                    transactionComplete(transaction.getTransaction_id());
                    return;
                }
                // Check if all rm's is in state 0.2, if so we can jump to sending commit.
                boolean allReceived = true;
                for (Participant resourceManager: resourceManagers) {
                    if(transaction.getState(resourceManager.name()) != 0.2) {
                        allReceived = false;
                    }
                }
                if (allReceived) {
                    // Alle er 0.2
                    System.out.println("Alle har rapportert med startOK. Går til commit.");
                    setState(Participant.TM, 1, transaction.getTransaction_id());
                    for (Participant resourceManager : resourceManagers) {
                        commit(resourceManager, getSaved_state(transaction.getTransaction_id()).getTransaction_id());
                    }
                    return;
                }
                // Everyone is not in the final state, send out necessary messages.
                for (Participant resourceManager: resourceManagers) {
                    double rm_state = transaction.getState(resourceManager.name());
                    // START has not been sent to RM or START_OK/FAILED has not been received
                    if(rm_state < 0.2) {
                        Message msg = new Message(transaction.getTransaction_id(), resourceManager, messageTypes.START);
                        msg.setData(transaction.getData());
                        send(resourceManager, msg);
                    }
                }
            }
            // TM has not received COMMIT_OK / COMMIT_FAIL from all
            else if(tm_value == 1) {
                // Check if all rm's is in state 1.2, if so we're finished.
                boolean allReceived = true;
                for (Participant resourceManager: resourceManagers) {
                    if(transaction.getState(resourceManager.name()) != 1.2) {
                        allReceived = false;
                    }
                }
                if (allReceived) {
                    // All is 1.2
                    System.out.println("All reported with COMMIT_OK.");
                    setState(Participant.TM, 4, transaction.getTransaction_id());
                    transactionComplete(transaction.getTransaction_id());
                    return;
                }
                // Everyone is not in the final state, send out necessary messages.
                for (Participant resourceManager: resourceManagers) {
                    double rm_state = transaction.getState(resourceManager.name());
                    // COMMIT has not been sent to RM or COMMIT_OK/FAILED has not been received
                    if(rm_state < 1.1) {
                        Message msg = new Message(transaction.getTransaction_id(), resourceManager, messageTypes.COMMIT);
                        send(resourceManager, msg);
                    }
                }
            }
            // TM has not received ROLLBACK / ROLLBACK FAIL from all
            else if(tm_value == 2) {
                // Check if all rm's is in state 2.2, if so we're finished.
                boolean allReceived = true;
                for (Participant resourceManager: resourceManagers) {
                    if(transaction.getState(resourceManager.name()) != 2.2) {
                        allReceived = false;
                    }
                }
                if (allReceived) {
                    // All is 2.2
                    System.out.println("All reported with ROLLBACK_OK.");
                    setState(Participant.TM, 4, transaction.getTransaction_id());
                    transactionComplete(transaction.getTransaction_id());
                    return;
                }
                // Everyone is not in the final state, send out necessary messages.
                for (Participant resourceManager: resourceManagers) {
                    double rm_state = transaction.getState(resourceManager.name());
                    // ROLLBACK has not been sent to RM or ROLLBACK_OK/FAILED has not been received
                    if(rm_state < 2.2) {
                        Message msg = new Message(transaction.getTransaction_id(), resourceManager, messageTypes.ROLLBACK);
                        send(resourceManager, msg);
                    }
                }
            }
            // TM has not received UNDO_OK / UNDO_FAIL from all
            else if(tm_value == 3) {
                // Check if all rm's is in state 3.2, if so we're finished.
                boolean allReceived = true;
                for (Participant resourceManager: resourceManagers) {
                    if(transaction.getState(resourceManager.name()) != 3.2) {
                        allReceived = false;
                    }
                }
                if (allReceived) {
                    // All is 3.2
                    System.out.println("All reported with UNDO_OK.");
                    setState(Participant.TM, 4, transaction.getTransaction_id());
                    transactionComplete(transaction.getTransaction_id());
                    return;
                }
                // Everyone is not in the final state, send out necessary messages.
                for (Participant resourceManager: resourceManagers) {
                    double rm_state = transaction.getState(resourceManager.name());
                    // ROLLBACK has not been sent to RM or ROLLBACK_OK/FAILED has not been received
                    if(rm_state < 3.2) {
                        Message msg = new Message(transaction.getTransaction_id(), resourceManager, messageTypes.UNDO);
                        send(resourceManager, msg);
                    }
                }
            }
            // Transaction was finished, but not deleted
            else if(tm_value == 4) {
                transactionComplete(id);
            }
        }
    }


    private void transaction(Message msg) {
        System.out.println("Transaction received.");
        HashMap<String, Double> states = new HashMap<>();
        states.put(Participant.TM.name(), 0.0);
        for (Participant resourceManager: resourceManagers) {
            states.put(resourceManager.name(), 0.0);
        }
        // If there is no data in the transaction, jump to last step
        if(msg.getData() ==null) {
            appendLog("No data in transaction.", msg.getTransaction_id());
            transactionComplete(msg.getTransaction_id());
            return;
        }
        TM_State new_trans = new TM_State(msg.getTransaction_id(), states, msg.getData());
        // Debugging
        if (msg.isDebug()) {
            new_trans.setDebug(true);
            new_trans.setDebugAnswers(msg.getDebugAnswers());
        }
        // Save state in hashmap with key = id.
        setSaved_state(new_trans, msg.getTransaction_id());

        // Notify all rm's of incoming transaction
        for (Participant resourceManager : resourceManagers) {
            start(resourceManager, getSaved_state(msg.getTransaction_id()).getTransaction_id());
        }

    }


    private void start(Participant who, int trans_id) {
        Message start = new Message(trans_id, who, messageTypes.START);
        start.setData(getSaved_state(trans_id).getData());
        boolean sent = send(who, start);
        setState(who, 0.1, trans_id);
        if(!sent) {
            appendLog("Could not send message START to " + who.name(), trans_id);
            startFail(new Message(trans_id, who, messageTypes.START_FAIL));
        }
    }


    private void startOk(Message msg) {
        Participant who = msg.getClient_id();
        if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 0.1) {
            setState(who, 0.2, msg.getTransaction_id());
        }
        // Sjekk om tilstand = 0.2 for alle rm
        for (Participant resourceManager1 : resourceManagers) {
            if (getSaved_state(msg.getTransaction_id()).getState(resourceManager1.name()) != 0.2) {
                System.out.println("Ikke alle har tilstand 0.2, " + resourceManager1.name());
                return;
            }
        }
        // Alle er 0.2
        System.out.println("Alle har rapportert med startOK. Går til commit.");
        setState(Participant.TM, 1, msg.getTransaction_id());
        for (Participant resourceManager : resourceManagers) {
            commit(resourceManager, getSaved_state(msg.getTransaction_id()).getTransaction_id());
        }
    }


    private void startFail(Message msg) {
        // Update TM to state 2.0 - rollback
        setState(Participant.TM, 2.0, msg.getTransaction_id());
        setFailed(true);
        // Update the node that failed with msg received.
        setState(msg.getClient_id(), 2.2, msg.getTransaction_id());
        // Log error, and send rollback to others.
        for (Participant resourceManager : resourceManagers) {
            // Ikke meld fra til noden om den allerede har fått beskjed, eller også rapporterte feil.
            // Den trenger beskjed om tilstanden er under 2.2
            if (getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) < 2.2) {
                rollback(resourceManager, msg.getTransaction_id());
            }
        }
        // Sjekk om alle har tilstand 2.2, isåfall -> ferdig
        for(Participant resourceManager: resourceManagers) {
            if(getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) != 2.2) {
                return;
            }
        }
        // Alle har rullet tilbake eller rapportert feil på start.
        setState(Participant.TM, 4, msg.getTransaction_id());
        transactionComplete(msg.getTransaction_id());
    }


    private void commit(Participant who, int trans_id){
        // Send commit.
        Message commit = new Message(trans_id, who, messageTypes.COMMIT);
        boolean sent = send(who, commit);
        setState(who, 1.1, trans_id);
        if(!sent) {
            appendLog("Could not send message COMMIT to " + who.name(), trans_id);
            startFail(new Message(trans_id, who, messageTypes.COMMIT_FAIL));
        }
    }


    private void commitOk(Message msg) {
        // Hvis transaksjon har feilet, gjør ingenting
        if(!isFailed()) {
            Participant who = msg.getClient_id();
            if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 1.1) {
                setState(who, 1.2, msg.getTransaction_id());
            }
            // Sjekk om tilstand = 1.2 for alle rm
            for (Participant resourceManager : resourceManagers) {
                if (getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) != 1.2) {
                    return;
                }
            }
            // Alle er 1.2
            setState(Participant.TM, 4, msg.getTransaction_id());
            transactionComplete(msg.getTransaction_id());
        }
    }


    private void commitFail(Message msg) {
        // Update TM to state 3.0 - undo
        setFailed(true);
        setState(Participant.TM, 3.0, msg.getTransaction_id());
        // Update the node that failed.
        setState(msg.getClient_id(), 3.2, msg.getTransaction_id());
        // Log error, and send undo to others.

        for (Participant resourceManager : resourceManagers) {
            // Ikke meld fra til noden om den allerede har fått beskjed, eller også rapporterte feil.
            // Den trenger beskjed om tilstanden er under 3.2
            if (getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) < 3.2) {
                undo(resourceManager, msg.getTransaction_id());
            }
        }
        // Sjekk om alle har tilstand 3.2, isåfall -> ferdig
        for(Participant resourceManager: resourceManagers) {
            if(getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) != 3.2) {
                return;
            }
        }
        // Alle har undoet eller rapportert feil på commit.
        transactionComplete(msg.getTransaction_id());
    }


    private void rollback(Participant who, int trans_id) {
        // Send rollback.
        Message rollback = new Message(trans_id, who, messageTypes.ROLLBACK);
        boolean sent = send(who, rollback);
        setState(who, 2.1, trans_id);
        if(!sent) {
            appendLog("Could not send message ROLLBACK to " + who.name(), trans_id);
            startFail(new Message(trans_id, who, messageTypes.ROLLBACK_FAIL));
        }
    }


    private void rollbackOK(Message msg) {
        Participant who = msg.getClient_id();
        if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 2.1) {
            setState(who, 2.2, msg.getTransaction_id());
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for (Participant resourceManager : resourceManagers) {
            if (getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) != 2.2) {
                return;
            }
        }
        // Alle er 2.2
        setState(Participant.TM, 4, msg.getTransaction_id());
        transactionComplete(msg.getTransaction_id());
    }


    private void rollbackFail(Message msg) {
        Participant who = msg.getClient_id();
        if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 2.1) {
            setState(who, 2.2, msg.getTransaction_id());
        }
        // Sjekk om tilstand = 2.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state(msg.getTransaction_id()).getState(resourceManagers[i].name()) != 2.2) {
                return;
            }
        }
        // Alle er 2.2
        setState(Participant.TM, 4, msg.getTransaction_id());
        transactionComplete(msg.getTransaction_id());
    }


    private void undo(Participant who, int trans_id){
        // Send undo.
        Message undo = new Message(trans_id, who, messageTypes.UNDO);
        boolean sent = send(who, undo);
        setState(who, 3.1, trans_id);
        if(!sent) {
            appendLog("Could not send message UNDO to " + who.name(), trans_id);
            startFail(new Message(trans_id, who, messageTypes.UNDO_FAIL));
        }
    }


    private void undoOk(Message msg) {
        Participant who = msg.getClient_id();
        if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 3.1) {
            setState(who, 3.2, msg.getTransaction_id());
        }
        // Sjekk om tilstand = 3.2 for alle rm
        for (Participant resourceManager : resourceManagers) {
            if (getSaved_state(msg.getTransaction_id()).getState(resourceManager.name()) != 3.2) {
                return;
            }
        }
        // Alle er 3.2
        setState(Participant.TM, 4, msg.getTransaction_id());
        transactionComplete(msg.getTransaction_id());
    }


    private void undoFail(Message msg) {

        Participant who = msg.getClient_id();
        if(getSaved_state(msg.getTransaction_id()).getState(who.name()) == 3.1) {
            setState(who, 3.2, msg.getTransaction_id());
        }
        // Sjekk om tilstand = 3.2 for alle rm
        for(int i=0;i<resourceManagers.length;i++) {
            if(getSaved_state(msg.getTransaction_id()).getState(resourceManagers[i].name()) != 3.2) {
                return;
            }
        }

        // Alle er 3.2
        setState(Participant.TM, 4, msg.getTransaction_id());
        transactionComplete(msg.getTransaction_id());
    }


    private void transactionComplete(int trans_id) {
        System.out.println("\n");
        if(isFailed()) {
            System.out.println("Transaction with id "+trans_id+" failed.");
        }
        else {
            System.out.println("Transaction with id "+trans_id+" completed without any errors.");
        }


        // Delete saved transaction
        saved_states.remove(trans_id);
        try {
            File delete = new File(stateFolderPath + trans_id + ".ser");

            if(delete.delete())            {
                System.out.println("Transaction-file deleted successfully");
                appendLog("Transaction-file deleted.", trans_id);
            }
            else            {
                System.out.println("Failed to delete transaction-file");
            }
        }
        catch (Exception e) {
            System.out.println("ERROR reading logs");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
        }

        System.out.println("Printing logs:");
        System.out.println(readLog(trans_id));
    }


    private void saveState(int trans_id) {
        saveFile(getSaved_state(trans_id), stateFolderPath + trans_id + ".ser");
    }


    private void setState(Participant who, double newStep, int trans_id) {
        TM_State tmp = getSaved_state(trans_id);
        tmp.setState(who.name(), newStep);
        setSaved_state(tmp, trans_id);
        saveState(trans_id);
    }


    private String readLog(int trans_id)    {
        String fileName = logsFolderPath + trans_id + ".txt";
        try {
            return new String(Files.readAllBytes(Paths.get(fileName)));
        }
        catch (Exception e) {
            System.out.println("ERROR reading logs");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
        }
        return null;
    }


    private boolean send(Participant who, Message message) {
        appendLog("Sending " + message.getMessage() + " to " + message.getClient_id().name(), message.getTransaction_id());
        // Try catch this
        // Debugging
        if(getSaved_state(message.getTransaction_id()).isDebug()) {
            message.setDebug(true);
            message.setDebugAnswers(getSaved_state(message.getTransaction_id()).getDebugAnswers());
        }
        return sendMessage(who.getHost(), who.getPort(), message);
    }

}
