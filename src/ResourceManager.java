import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceManager extends Client {
    private String TM_HOST;
    private int TM_PORT;
    // How long the RM will wait for the TM to send a message after a commit_ok has been sent.
    private final int COMMIT_OK_TIMEOUT = 8; // seconds
    private HashMap<Integer, RM_State> saved_states;
    private final String stateFolderPath;
    private final String resourceFilePath;
    private java.nio.channels.FileLock lock;
    // The database. In this case a textfile
    private RandomAccessFile raf;

    public ResourceManager(int PORT, String TM_HOST, int TM_PORT, String resourceFilePath, String stateFolderPath, String logFilePath) {
        super(PORT, logFilePath);
        this.TM_HOST = TM_HOST;
        this.TM_PORT = TM_PORT;
        this.stateFolderPath = stateFolderPath;
        this.resourceFilePath = resourceFilePath;
        saved_states = readSavedStates(new File(stateFolderPath));
    }


    private RM_State getSaved_state(int id) {
        return saved_states.get(id);
    }


    private void setSaved_state(RM_State saved_state, int id) {
        saved_states.put(id, saved_state);
    }


    private FileLock getLock() {
        return lock;
    }


    private void setLock(FileLock lock) {
        this.lock = lock;
    }


    private HashMap<Integer, RM_State> readSavedStates(final File folder) {
        HashMap<Integer, RM_State> saved_states = new HashMap<>();
        try {
            for (final File fileEntry : folder.listFiles()) {
                try {
                    FileInputStream is = new FileInputStream(fileEntry);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    RM_State state = (RM_State) ois.readObject();
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


    @Override
    public void handleMessage(Message msg) {
        appendLog("Got message " + msg.getMessage().name(), msg.getTransaction_id());
        // For debugging. Ekko the value specified in debug.
        // If the value is NOREPLY don't reply.
        if(msg.isDebug()) {
            messageTypes ans = msg.getDebugAnswers().get(msg.getClient_id().name()).get(msg.message);
            if(ans != messageTypes.NO_REPLY) {
                sendMessage(TM_HOST, TM_PORT, new Message(msg.getTransaction_id(), msg.getClient_id(), ans));
            }
            return;
        }
        switch (msg.getMessage()) {
            // Save state if it does noe exist
            case START:
                if(getSaved_state(msg.getTransaction_id()) == null) {
                    System.out.println("Data: " + msg.getData());
                    RM_State newrm = new RM_State(msg.getTransaction_id(), msg.getData());
                    setSaved_state(newrm, msg.getTransaction_id());
                    setState(0, msg.getTransaction_id());
                }
                start(msg);
                break;
            case COMMIT:
                commit(msg);
                break;
            case ROLLBACK:
                rollback(msg);
                break;
            case UNDO:
                undo(msg);
        }
    }


    // Received a transaction. Trying to set locks on database, and see if file is writable.
    // If successfull, reply with START_OK, if not START_FAIL
    private void start(Message msg) {
        // Try to hold resources
        try {
            boolean writable = Files.isWritable(Paths.get(resourceFilePath));
            if(!writable) {
                throw new Exception();
            }
            raf = new RandomAccessFile(resourceFilePath, "rw");
            FileLock lock = raf.getChannel().lock();
            setLock(lock);

        }
        catch (Exception e) {
            System.out.println("ERROR reading logs");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Localized: " + e.getLocalizedMessage());
            System.err.println("Stack Trace: " + e.getStackTrace());
            // Report failed
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.START_FAIL));
            setState(2, msg.getTransaction_id());
            transactionFinished(msg.getTransaction_id());
        }
        //Report ok
        send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.START_OK));
        setState(1, msg.getTransaction_id());
    }


    // Committing transaction. Writing to file.
    private void commit(Message msg) {
        // If the state is currently START_OK proceed with commit
        if(getState(msg.getTransaction_id()) == 1) {
            // Write data to file
            try {
                raf.seek(raf.length());
                raf.write((getSaved_state(msg.getTransaction_id()).getData() + "\n").getBytes());
                System.out.println("Successfully wrote to file.");
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_OK));
                setState(4, msg.getTransaction_id());
                deleteAfterTimeout(msg.getTransaction_id());
            }
            catch (Exception e) {
                System.out.println("An error occurred writing to file.");
                e.printStackTrace();
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_FAIL));
                setState(5, msg.getTransaction_id());
                transactionFinished(msg.getTransaction_id());
            }
        }
        // If the state is COMMIT_OK the commit is already done, reply with OK.
        else if (getState(msg.getTransaction_id()) == 4) {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_OK));
            deleteAfterTimeout(msg.getTransaction_id());
        }
        // If state is anything else, reply with fail
        else {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.COMMIT_FAIL));
            setState(5, msg.getTransaction_id());
            transactionFinished(msg.getTransaction_id());
        }
    }


    // Release resources.
    private void rollback(Message msg) {
        // If state is 1 start rollback
        if (getState(msg.getTransaction_id()) == 1) {
            try {
                raf.close();
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_OK));
                setState(7, msg.getTransaction_id());
                transactionFinished(msg.getTransaction_id());
            }
            catch (Exception e) {
                e.printStackTrace();
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_FAIL));
                setState(8, msg.getTransaction_id());
                transactionFinished(msg.getTransaction_id());
            }
        }
        // If state is already rollback OK, send ok
        else if(getState(msg.getTransaction_id()) == 5) {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_OK));
            transactionFinished(msg.getTransaction_id());
        }
        // if state is anything else, reply with fail
        else {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.ROLLBACK_FAIL));
            setState(8, msg.getTransaction_id());
            transactionFinished(msg.getTransaction_id());
        }
    }


    // Undo last transaction. Delete the line of the transaction, and then release resources.
    private void undo(Message msg) {
        // If state is 4 start undo
        if (getState(msg.getTransaction_id()) == 4) {
            try {
                long length = raf.length() - 1;
                byte b;
                do {
                    length -= 1;
                    raf.seek(length);
                    b = raf.readByte();
                } while(b != 10 && length>0);
                raf.setLength(length+1);
                raf.close();
                getLock().close();
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_OK));
                setState(7, msg.getTransaction_id());
                transactionFinished(msg.getTransaction_id());
            }
            catch (Exception e) {
                e.printStackTrace();
                send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_FAIL));
                setState(11, msg.getTransaction_id());
                transactionFinished(msg.getTransaction_id());
            }
        }
        // If state is already undo OK, send ok
        if(getState(msg.getTransaction_id()) == 10) {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_OK));
            transactionFinished(msg.getTransaction_id());
        }
        // if state is anything else, reply with fail
        else {
            send(new Message(msg.getTransaction_id(), msg.getClient_id(), messageTypes.UNDO_FAIL));
            setState(11, msg.getTransaction_id());
            transactionFinished(msg.getTransaction_id());
        }
    }


    // Delete saved transaction
    private void transactionFinished(int trans_id) {
        // Delete saved transaction
        saved_states.remove(trans_id);
        try {
            File delete = new File(stateFolderPath + trans_id + ".ser");
            if(delete.delete())            {
                System.out.println("Transaction-file deleted successfully");
            }
            else            {
                System.out.println("Failed to delete transaction-file");
            }
        }
        catch (Exception e) {
            System.out.println("Could not delete transaction-file");
            e.printStackTrace();
        }
        appendLog("Transaction finished and deleted.", trans_id);
    }


    // Method that gets called after commit_ok. If no new message is received for some time, the transaction gets deleted.
    private void deleteAfterTimeout(int trans_id) {
        System.out.println("STarting timeout");
        ScheduledExecutorService scheduler
                = Executors.newSingleThreadScheduledExecutor();
        Runnable task = new Runnable() {
            public void run() {
                try {
                    // Only delete if transaction is still in state COMMIT_OK
                    if(getSaved_state(trans_id).getState() == 4) {
                        // Delete saved transaction
                        saved_states.remove(trans_id);
                        File delete = new File(stateFolderPath + trans_id + ".ser");
                        if(delete.delete())            {
                            System.out.println("Transaction-file deleted successfully");
                            appendLog("Timeout waiting for reply. Assuming transaction can be deleted.", trans_id);
                        }
                        else            {
                            System.out.println("Failed to delete transaction-file");
                        }
                    }
                }
                catch (Exception e) {
                    System.out.println("Could not delete transaction-file");
                    e.printStackTrace();
                }
            }
        };
        // How long until interrupted
        int delay = COMMIT_OK_TIMEOUT;
        scheduler.schedule(task, delay, TimeUnit.SECONDS);
        scheduler.shutdown();
    }


    private void saveState(int trans_id) {
        saveFile(getSaved_state(trans_id), stateFolderPath + trans_id + ".ser");
    }

    private void setState(int newStep, int trans_id) {
        RM_State tmp = getSaved_state(trans_id);
        tmp.setState(newStep);
        setSaved_state(tmp, trans_id);
        saveState(trans_id);
    }


    private int getState(int trans_id) {
        return getSaved_state(trans_id).getState();
    }


    private void send(Message msg) {
        appendLog("Sending message " + msg.getMessage().name(), msg.getTransaction_id());
        sendMessage(TM_HOST, TM_PORT, msg);
    }

}
