package code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class HttpExchangeMethods {


    private final HttpExchange exchange;
    private final String errorMessage;


    public HttpExchangeMethods(HttpExchange exchange, String erroMessage) {
        this.exchange = exchange;
        this.errorMessage = erroMessage;
    }



    /**
     * Gets the content of the request while cheking its validity
     * 
     * @param  exchange HTTPS request handler
     * @param  headers HTTPS request and response headers
     * @return Request content string
     * @throws IOException
     * @throws WrongMethodTypeException Request's method type is invalid
     */
    public String getContent(Headers headers) throws IOException {

        // Check content type validity
        if (!headerContent(headers).startsWith("application/json")) {
            throw new WrongMethodTypeException("Invalid method type. Must be \"application/json\"");
        }

        // Get reader for JSON file
        try (InputStream inputStream = exchange.getRequestBody()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            // Get the JSON file
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }




    /**
    * Gets the content from the header
    *
    * @param  headers HTTPS request and response headers
    * @return Content as a string
    * @throws WrongMethodTypeException Content type is not "Content-Type"
    */
    private String headerContent(Headers headers) {
        // Set content type
        if (headers.containsKey("Content-Type")) {
            return headers.getFirst("Content-Type");
        }

        // Content type is incorrect
        else {
            throw new WrongMethodTypeException("Incorrect content type in request\n");
        }
    }



    /**
    * Gets username from authentication query
    *
    * @param  exchange HTTPS request hadler
    * @return User's username string
    * @return User's username
    * @throws SecurityException If given user is not registered 
    */
    public String checkUserValidity() {
        if (exchange.getPrincipal() == null) {
            throw new SecurityException("User isn't authenticated\n");
        }

        return exchange.getPrincipal().getUsername();
    }





    /**
    * Sends error message to the server
    *
    * @param  exchange HTTPS request hadler
    * @param  statusCode HTTPS status code of the error
    * @param  message Error message as string
    */
    public void errorResponse(int statusCode, String message) {
        // Transform message to bytes
        byte[] responseBytes = (errorMessage + message).getBytes(StandardCharsets.UTF_8);
    
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
