public class TaskManager {
    private ArrayList<ResourceManager> resourceManagers = new ArrayList();

    public TaskManager() {
        // Check if there is any stored request locally
        // If yes, jump to step
        // If not, wait for incoming requests
    }

    private void addResourceManager() {
        // Add RM to arraylist
    }

    private void handleRequest(Request request) {
        // Step 1
        // Record request and status in case of failure
        boolean saved = saveRequest(request);
        if (!saved) {
            // Throw error
        }
        // Step 2
        // Send request to all rm's

        // Step 3
        // Voting round

        // Step 4
        // Committing or aborting

        // Step 5
        // If committing - Send commit request

        // Step 6
        // Wait for all rm's to report back that they have committed

        // Step 7
        // Wait for all rm's to report back that they have deleted the request
        // Do I need this. Can i just have a flag in the request?

        // Step 8
        // Delete local information about the request
    }
    private boolean saveRequest(Request request) {
        // Save request to file.
    }
}