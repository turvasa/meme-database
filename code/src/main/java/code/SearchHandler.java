package code;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class SearchHandler implements HttpHandler {

    private final Database database;


    /**
    * Handles all request for obseravtions
    *
    * @param database Database of the server
    */
    public SearchHandler(Database database) {
        this.database = database;
    }



    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try (exchange) {
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "GET" -> getRequest(exchange);

                default -> errorResponse(exchange, 405, "Unsupported search method\n");
            }
        }

        // Search items are invalid
        catch (IllegalArgumentException e) {
            errorResponse(exchange, 403, e.getMessage());
        }

        catch (Exception e) {
            errorResponse(exchange, 500, e.getMessage());
        }
    }

    


    private void getRequest(HttpExchange exchange) {

        try {

            // Content type validity chech
            Headers headers = exchange.getRequestHeaders();
            String content = getContent(exchange, headers);

            // Get the search details
            JSONObject contentJson = new JSONObject(content);
            String title = contentJson.getString("title");
            int id = contentJson.getInt("id");
            JSONArray tagsArray = contentJson.getJSONArray("tags");
            Tags tags = new Tags(database, tagsArray);

            // Get corresponding memes
            Set<String> memePaths = database.getMemePaths(title, id, tags);
            
            JSONArray memes = new JSONArray();
            for (String path: memePaths) {
                File meme = new File("../../../../memes/"+path);

                if (!meme.exists()) {
                    memes.put(path + " not found (photo is most likely deleted)");
                }

                else {
                    memes.put(path);
                }
            }

            // Send the meme paths
            byte[] memeBytes = memes.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, memeBytes.length);
        }

        catch (SQLException | IOException e) {
            errorResponse(exchange, 406, e.getMessage());
        }

        catch (Exception e) {
            errorResponse(exchange, 400, e.getMessage());
        }
    }


    /**
    * Gets the content from the header
    *
    * @param  headers HTTPS request and response header
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


    private String getContent(HttpExchange exchange, Headers headers) throws IOException {

        if (!headerContent(headers).startsWith("application/json")) {
            throw new WrongMethodTypeException("Invalid methdo type");
        }

        try (InputStream inputStream = exchange.getRequestBody()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            return reader.lines().collect(Collectors.joining("\n"));
        }
    }


    /**
    * Sends error message to the server
    *
    * @param  exchange HTTPS request hadler
    * @param  statusCode HTTPS status code of the error
    * @param  message Error message as string
    */
    private void errorResponse(HttpExchange exchange, int statusCode, String message) {
        // Transform message to bytes
        byte[] responseBytes = ("[Error] OBSERVATION - "+message).getBytes(StandardCharsets.UTF_8);
    
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
