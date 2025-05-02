package code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UserHandler implements HttpHandler {


    private final UserAuthenticator authenticator;


    /**
    * Handles users registrations
    *
    * @param  authenticator Users authenticator for checking user's authentication
    */
    public UserHandler(UserAuthenticator authenticator) {
        this.authenticator = authenticator;
    }


    
    /**
    * Handles the method of the HTTP request (Available: POST)
    *
    * @param  exchange HTTP request hadler
    */
    @Override
    public void handle(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        String method = exchange.getRequestMethod().toUpperCase();
    
        try (exchange) {
            switch (method) {

                case "POST" -> postRequest(exchange, headers);
    
                default -> errorResponse(exchange, 405, "Unsupported user method\n");
            }
        }

        // Internal server error
        catch (Exception e) {
            errorResponse(exchange, 500, "[HANDLE]: "+e.getMessage());
        }
    }



    /**
    * Handles POST request for users
    *
    * @param  exchange HTTP request hadler
    * @param  headers HTTP request and response header
    */
    @SuppressWarnings("UseSpecificCatch")
    private void postRequest(HttpExchange exchange, Headers headers) throws IOException {
        try {
            // Read user's input
            String content = headerContent(headers);
            String user = getUser(exchange, content);

            // Check that user's JSON file is not empty
            if (user == null || user.isEmpty()) {
                throw new NullPointerException("JSON file is NULL or empty\n");
            }

            // Add the user to be member if it's a new one
            JSONObject userObject = new JSONObject(user);
            authenticator.addUser(userObject);

            // Send success message
            exchange.sendResponseHeaders(200, -1);
            System.out.println("User added successfully");
        }

        // Invalid format for user
        catch (IllegalArgumentException e) {
            errorResponse(exchange, 401, "[POST]: "+e.getMessage());
        }

        // User is not new member
        catch (IllegalStateException e) {
            errorResponse(exchange, 403, "[POST]: "+e.getMessage());
        }

        // Null user credential or user's JSON file is empty
        // No content type found
        catch (NullPointerException | WrongMethodTypeException | JSONException e) {
            errorResponse(exchange, 405, "[POST]: "+e.getMessage());
        }
        
        // Other errors
        catch (IOException | SQLException e) {
            errorResponse(exchange, 500, "[POST]: "+e.getMessage());
        }

        // Catch unexcepted errors
        catch (Exception e) {
            errorResponse(exchange, 418, "[POST]: "+e.getMessage());
        }

    }


    /**
    * Gets the content from the header
    *
    * @param  headers HTTP request and response header
    * @return Content as a string
    * @throws WrongMethodTypeException Content type is not "Content-Type"
    */
    private String headerContent(Headers headers) {
        // Set content
        if (headers.containsKey("Content-Type")) {
            return headers.getFirst("Content-Type");
        }

        // Content not found
        else {
            throw new WrongMethodTypeException("No content type in request\n");
        }
    }


    /**
    * Gets the user's credentials JSON object string
    *
    * @param  exchange HTTP request hadler
    * @param  content Content string
    * @return User's credentials string
    * @throws WrongMethodTypeException If given content type is incorrect
    */
    @SuppressWarnings("ConvertToTryWithResources")
    private String getUser(HttpExchange exchange, String content) throws IOException {
        
        // Check validity of the content type
        if (!content.startsWith("application/json")) {
            throw new WrongMethodTypeException("Incorrect content type\n");
        }

        // Get reader for JSON
        try(InputStream inputStream = exchange.getRequestBody()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        
            // Get user's info from JSON
            return reader.lines().collect(Collectors.joining("\n"));
        }

        
    }


    

    /**
    * Sends error message to the server
    *
    * @param  exchange HTTP request hadler
    * @param  statusCode HTTP status code of the error
    * @param  message Error message as string
    */
    private void errorResponse(HttpExchange exchange, int statusCode, String message) {
        // Transform message to bytes
        byte[] responseBytes = ("[Error] USER - "+message).getBytes(StandardCharsets.UTF_8);
    
        try (OutputStream outputStream = exchange.getResponseBody()) {

            // Send response
            exchange.sendResponseHeaders(statusCode, responseBytes.length);

            // Output the response
            outputStream.write(responseBytes);
            outputStream.flush();

        } catch (IOException e) {
            System.out.println("Error writing response: "+e.getMessage()+"\n");
        }
    }
}
