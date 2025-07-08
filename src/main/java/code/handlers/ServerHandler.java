package code.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.HttpExchangeMethods;


public class ServerHandler implements HttpHandler {


    private static final String SERVER_MESSAGE = 
        """
            Hello, Welcome to the Meme Database!
               
            The Instructions for the database usage can be found from \"/help\"
        """
    ;


    /**
    * Handles the method of the HTTP request (Available: POST, GET, DELETE)
    *
    * @param  exchange HTTP request hadler
    */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - MAINPAGE");

        try (exchange) {
            String method = exchange.getRequestMethod().toUpperCase();
            
            switch (method) {
                
                // Handle POST requests here (users send this for sending messages)
                case "GET" -> getRequest(exchange);
                
                // Hande unsupported methods
                default -> exchangeMethods.errorResponse(407, "Unsupported method");
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
