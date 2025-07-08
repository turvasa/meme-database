package code.handlers;

import java.io.IOException;
import java.lang.invoke.WrongMethodTypeException;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.HttpExchangeMethods;
import code.user.UserAuthenticator;

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
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - REGISTRATION");
    
        try (exchange) {
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {

                case "POST" -> postRequest(exchange, exchangeMethods);
    
                default -> exchangeMethods.errorResponse(405, ": Unsupported user method\n");
            }
        }

        // Internal server error
        catch (Exception e) {
            exchangeMethods.errorResponse(500, e.getMessage());
        }
    }



    /**
    * Handles POST request for users
    *
    * @param  exchange HTTP request hadler
    * @param  headers HTTP request and response header
    */
    @SuppressWarnings("UseSpecificCatch")
    private void postRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods) throws IOException {
        try {
            // Read user's input
            Headers headers = exchange.getRequestHeaders();
            String user = exchangeMethods.getContent(headers);

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
            exchangeMethods.errorResponse(401, e.getMessage());
        }

        // User is not new member
        catch (IllegalStateException e) {
            exchangeMethods.errorResponse(403, e.getMessage());
        }

        // Null user credential or user's JSON file is empty
        // No content type found
        catch (NullPointerException | WrongMethodTypeException | JSONException e) {
            exchangeMethods.errorResponse(405, e.getMessage());
        }
        
        // Other errors
        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(500, e.getMessage());
        }

        // Catch unexcepted errors
        catch (Exception e) {
            exchangeMethods.errorResponse(418, e.getMessage());
        }

    }

}
