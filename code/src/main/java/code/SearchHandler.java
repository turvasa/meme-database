package code;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore.Entry;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class SearchHandler implements HttpHandler {

    private final Database database;
    private Set<Tag> allTags;
    private SortedMap<Meme, Integer> memes;


    /**
    * Handles all request for obseravtions
    *
    * @param database Database of the server
    */
    public SearchHandler(Database database, Set<Tag> allTags, SortedMap<Meme, Integer> memes) {
        this.database = database;
        this.allTags = allTags;
        this.memes = memes;
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
            String title = contentJson.optString("title", null);
            int id = contentJson.optInt("id", -1);
            JSONArray tagsArray = contentJson.optJSONArray("tags", null);

            // Corresponding memes array
            JSONArray memePaths = filterMemes(title, id, tagsArray);

            // Send the meme paths
            byte[] memeBytes = memePaths.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, memeBytes.length);
        }

        catch (IOException e) {
            errorResponse(exchange, 406, e.getMessage());
        }

        catch (Exception e) {
            errorResponse(exchange, 400, e.getMessage());
        }
    }


    private String getContent(HttpExchange exchange, Headers headers) throws IOException {

        if (!headerContent(headers).startsWith("application/json")) {
            throw new WrongMethodTypeException("Invalid method type");
        }

        try (InputStream inputStream = exchange.getRequestBody()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            return reader.lines().collect(Collectors.joining("\n"));
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



    private JSONArray filterMemes(String title, int id, JSONArray tags) throws SQLException {
        
        boolean isFiltered = false;
        Map<Meme, Integer> filtererMemes = new HashMap<>();
        
        // Filter title
        if (title != null) {
            filterTitle(filtererMemes, title);
            isFiltered = true;
        }

        // Filter ID
        if (id != -1) {
            filterId(filtererMemes, id, isFiltered);
            isFiltered = true;
        }

        // Filter with tags
        if (tags != null) {
            filterTags(filtererMemes, tags, isFiltered);
            isFiltered = true;
        }

        // Return the found memes as json array
        if (isFiltered) {
            return filteredMemeArray(filtererMemes);
        } else {
            return filteredMemeArray(memes);
        }
    }


    private void filterTitle(Map<Meme, Integer> currentMemes, String title) {

        Meme meme = new Meme(title, new JSONArray());

        if (memes.containsKey(meme)) {
            currentMemes.put(meme, memes.get(meme));
        }
    }


    private void filterId(Map<Meme, Integer> currentMemes, int id, boolean isFiltered) throws SQLException {

        // Previous filters have filtered out everything
        if (currentMemes.isEmpty()) {
            return;
        }

        // This is first filtering
        if (!isFiltered) {
            currentMemes = database.getMemeById(id);
        }

        else {
            // Memes are already filtered by title, so there is only 1 node in the map
            if (!currentMemes.containsValue(id)) {
                currentMemes.clear();
            }
        }
    }


    private void filterTags(Map<Meme, Integer> currentMemes, JSONArray tagsJson, boolean isFiltered) {

        // Previous filters have filtered out everything
        if (currentMemes.isEmpty()) {
            return;
        }

        // Convert the tags json to set
        Set<Tag> tags = new HashSet<>();
        for (int i = 0; i < tagsJson.length(); i++) {
            JSONObject tag = tagsJson.getJSONObject(i);
            String title = tag.optString("title", "Title not provided");
            int count = tag.optInt("count", 0);
            tags.add(new Tag(title, count));
        }

        // This is first filtering
        if (!isFiltered) {
            for (Meme meme: memes.keySet()) {

                // Add all corresponding memes
                if (meme.containsTags(tags)) {
                    currentMemes.put(meme, 0);
                }
            }
        }

        else {
            for (Meme meme: currentMemes.keySet()) {

                // Remove all non corresponding memes
                if (!meme.containsTags(tags)) {
                    currentMemes.remove(meme);
                }
            }
        }
    }


    private JSONArray filteredMemeArray(Map<Meme, Integer> filterMemes) {
        
        JSONArray filteredMemeArray = new JSONArray();

        for (Meme meme: filterMemes.keySet()) {
            filteredMemeArray.put(getFullPath(meme.getTitle()));
        }

        return filteredMemeArray;
    }


    private String getFullPath(String title) {
        return "../../../../memes/"+title;
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
