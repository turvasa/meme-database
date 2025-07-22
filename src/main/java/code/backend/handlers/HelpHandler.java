package code.backend.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.backend.HttpExchangeMethods;


public class HelpHandler implements HttpHandler {
    
    
    private static final String ERROR_MESSAGE = "[ERROR] - HELP";
    private static final String SERVER_MESSAGE = 
        """
        Welcome to the Meme Database Instructions
            1. Meme must be .png file
            2. Meme must be given with tags and title while adding
            3. Title white spaces will be replaced by '_'
        """
    ;



    /**
    * Handles the method of the HTTP request (Available: POST, GET, DELETE)
    *
    * @param  exchange HTTP request hadler
    */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - HELP");

        try (exchange) {
            String method = exchange.getRequestMethod().toUpperCase();
            
            switch (method) {
                
                // Handle POST requests here (users send this for sending messages)
                case "GET" -> getRequest(exchange);
                
                // Hande unsupported methods
                default -> exchangeMethods.errorResponse(407, ERROR_MESSAGE+": Unsupported method");
            }
        }

		catch (Exception e) {
			exchangeMethods.errorResponse(400, e.getMessage());
		}
	}


	/**
    * Handles the POST method
    *
    * @param  exchange HTTP request hadler
    */
	private void getRequest(HttpExchange exchange) throws IOException {
        byte[] bytes = SERVER_MESSAGE.getBytes(StandardCharsets.UTF_8);

		// Send response to the server
		exchange.sendResponseHeaders(200, bytes.length);
	}
  
}
