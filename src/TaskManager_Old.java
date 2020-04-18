import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class TaskManager_Old {
//    private ArrayList<ResourceManager> resourceManagers = new ArrayList();
//
//    public TaskManager_Old() {
//        // Check if there is any stored request locally
//        // If yes, jump to step
//        // If not, wait for incoming requests
//    }
//    private void addResourceManager() {
//        // Add RM to arraylist
//    }
//    private void handleRequest(Request request) {
//        // Step 1
//        // Record request and status in case of failure
//        boolean saved = saveRequest(request);
//        if (!saved) {
//            // Throw error
//        }
//        // Step 2
//        // Send request to all rm's
//
//        // Step 3
//        // Voting round
//
//        // Step 4
//        // Committing or aborting
//
//        // Step 5
//        // If committing - Send commit request
//
//        // Step 6
//        // Wait for all rm's to report back that they have committed
//
//        // Step 7
//        // Wait for all rm's to report back that they have deleted the request
//        // Do I need this. Can i just have a flag in the request?
//
//        // Step 8
//        // Delete local information about the request
//    }
//    private boolean saveRequest(Request request) {
//        // Save request to file.
//    }
    private static void handleRequest(HttpExchange exchange) throws IOException {
        System.out.println("Got request");
        String response = "Hi there!";
        exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8500), 0);
        HttpContext context = server.createContext("/");
        context.setHandler(TaskManager_Old::handleRequest);
        server.start();
    }
}