package code.backend.handlers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.backend.Database;
import code.backend.HttpExchangeMethods;
import code.backend.Meme;
import code.backend.Tag;
import code.backend.meme_comparators.MemeIdComparator;
import code.backend.meme_comparators.MemeLikesComparator;
import code.backend.meme_comparators.MemeTitleComparator;


public class MemeSearchHandler implements HttpHandler {


    private final Database database;

    private final Set<Tag> allTags;




    /**
    * Handles all request for meme searching
    *
    * @param database Database of the server
    */
    public MemeSearchHandler(Database database, Set<Tag> allTags) {
        this.database = database;
        this.allTags = allTags;
    }



    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - SEARCH");

        try (exchange) {
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "GET" -> getRequest(exchange, exchangeMethods);

                default -> exchangeMethods.errorResponse(405, ": Unsupported search method\n");
            }
        }

        // Search items are invalid
        catch (IllegalArgumentException e) {
            exchangeMethods.errorResponse(403, e.getMessage());
        }

        catch (Exception e) {
            exchangeMethods.errorResponse(500, e.getMessage());
        }
    }

    


    private void getRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods) {
        try {
            // Content type validity check
            Headers headers = exchange.getRequestHeaders();
            String content = exchangeMethods.getContent(headers);

            // Get the search details from the JSON
            JSONArray tagsArray = null;
            if (content.startsWith("{")) {
                JSONObject contentJson = new JSONObject(content);
                tagsArray = contentJson.optJSONArray("tags", null);
            }

            // Get search details from the query
            String[] variables = getVariables(exchange);
            String title = (variables[0] != null) ? variables[0].toLowerCase() : null;
            int id = (variables[1] != null) ? Integer.parseInt(variables[1]) : -1;
            SORT_TYPE sortingType = getMemeSortType(variables[2]);

            // Corresponding memes array
            JSONArray memePaths = filterMemes(exchange, title, id, tagsArray, sortingType);

            // No memes found
            if (memePaths.isEmpty()) {
                exchangeMethods.errorResponse(200, ": No memes found");
                return;
            }

            // Send the meme paths
            byte[] memeBytes = memePaths.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, memeBytes.length);
        }

        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(406, e.getMessage());
        }

        catch (JSONException e) {
            exchangeMethods.errorResponse(405, ": " + e.getMessage());
        }

    }




    /**
     * Gets all parameters from the query. If parameter is not given, it's set to NULL
     * 
     * @param  exchange HTTPS reguest handler
     * @return [title, id, sortingTypeString]
     * @throws NullPointerException If there are no variables given
     */
    private String[] getVariables(HttpExchange exchange) {

        String[] variableValues = {null, null, null};

        // Get the query
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();

        // No variables given
        if (query == null) {
            return variableValues;
        }

        // Split all variables
        String[] variables = query.split("&");

        // Iterate all variables
        for (String variable: variables) {

            // Split the variable to key and value
            String[] keyValue = variable.split("=");

            // Does variable have non NULL key and value
            if (keyValue.length != 2 || keyValue[1] == null) {
                continue;
            }

            // Check what variable it is
            switch (keyValue[0]) {

                // Title of the meme
                case "title" -> variableValues[0] = setVariable(keyValue);

                // ID of the meme
                case "id" -> variableValues[1] = setVariable(keyValue);

                // Sorting type
                case "sorting_type" -> variableValues[2] = setVariable(keyValue);
            }
        }
        return variableValues;
    }


    private String setVariable(String[] keyValue) {

        // Is value NULL
        if (keyValue[1].isEmpty()) {
            return null;
        }
        return keyValue[1];
    }


   

    private enum SORT_TYPE {
        ID,
        TITLE,
        LIKES,
        REVERSE_ID,
        REVERSE_TITLE,
        REVERSE_LIKES
    }

    private SORT_TYPE getMemeSortType(String sortType) {

        if (sortType == null) {
            return SORT_TYPE.ID;
        }

        switch (sortType.toLowerCase()) {
            case "id" -> {return SORT_TYPE.ID;}
            case "title" -> {return SORT_TYPE.TITLE;}
            case "likes" -> {return SORT_TYPE.LIKES;}
            case "reverse_id" -> {return SORT_TYPE.REVERSE_ID;}
            case "reverse_title" -> {return SORT_TYPE.REVERSE_TITLE;}
            case "reverse_likes" -> {return SORT_TYPE.REVERSE_LIKES;}
            default -> {return SORT_TYPE.ID;}
        }
    }




    private JSONArray filterMemes(HttpExchange exchange, String title, int id, JSONArray tags, SORT_TYPE sortingType) throws SQLException {
        boolean isFiltered = false;
        List<Meme> filteredMemes = new ArrayList<>();

        // Filter title
        if (title != null) {
            filterTitle(filteredMemes, title);
            isFiltered = true;
        }

        System.out.println("Tags");

        // Filter ID
        if (id != -1) {
            filterId(filteredMemes, id, isFiltered);
            isFiltered = true;
        }

        System.out.println("ID");

        // Filter with tags
        if (tags != null) {
            filterTags(filteredMemes, tags, isFiltered);
            isFiltered = true;
        }

        // Return the found memes as json array
        if (isFiltered) {
            return sortedMemeArray(exchange, filteredMemes, sortingType);
        } else {
            return sortedMemeArray(exchange, database.getMemesList(), sortingType);
        }
    }




    private void filterTitle(List<Meme> currentMemes, String title) throws SQLException {
        currentMemes.add(database.getMemeByTitle(title));
    }




    private void filterId(List<Meme> currentMemes, int id, boolean isFiltered) throws SQLException {

        // First filtering
        if (!isFiltered) {
            currentMemes.add(database.getMemeById(id));
            return;
        }

        // Iterate the current memes
        List<Meme> memesToBeRemoved = new ArrayList<>();
        for (Meme meme: currentMemes) {

            // Find non-corresponding memes
            if (meme.getID() != id) {
                memesToBeRemoved.add(meme);
            }
        } 
        
        // Remove non-corresponding memes
        currentMemes.removeAll(memesToBeRemoved);
    }




    private void filterTags(List<Meme> currentMemes, JSONArray tagsArray, boolean isFiltered) throws SQLException {
        Set<Tag> tags = getTagSet(tagsArray);

        // First filtering, so seek from all memes
        if (!isFiltered) {
            currentMemes.addAll(database.getMemesByTags(tags));
        }

        // Previous filters haven't filtered out everything
        else if (!currentMemes.isEmpty()) {
            for (Meme meme: currentMemes) {

                // Add all corresponding memes
                if (meme.containsTags(tags)) {
                    currentMemes.add(meme);
                }
            }
        }
   }


    private Set<Tag> getTagSet(JSONArray tagsArray) {

        // Iterate all tags
        Set<Tag> tags = new HashSet<>();
        for (int i = 0; i < tagsArray.length(); i++) {

            // Get the title of the tag
            JSONObject tagJson = tagsArray.getJSONObject(i);
            String title = tagJson.optString("title", "Title not provided");

            // Only valid tags are used
            Tag tag = new Tag(title, 0);
            if (!allTags.contains(tag)) {
                continue;
            }

            // Add the valid tag to the set
            tags.add(new Tag(title, 0));
        }

        return tags;
    }


    private JSONArray sortedMemeArray(HttpExchange exchange, List<Meme> filteredMemes, SORT_TYPE sortingType) {

        if (filteredMemes.isEmpty()) {
            return new JSONArray();
        }

        // Sort the memes
        switch (sortingType) {
            case ID -> Collections.sort(filteredMemes, new MemeIdComparator());
            case TITLE -> Collections.sort(filteredMemes, new MemeTitleComparator());
            case LIKES -> Collections.sort(filteredMemes, new MemeLikesComparator());
            case REVERSE_ID -> Collections.sort(filteredMemes, new MemeIdComparator().reversed());
            case REVERSE_TITLE -> Collections.sort(filteredMemes, new MemeTitleComparator().reversed());
            case REVERSE_LIKES -> Collections.sort(filteredMemes, new MemeLikesComparator().reversed());
        }

        System.out.println("Founded memes {");

        // Add all meme paths to the array
        JSONArray filteredMemesArray = new JSONArray();
        for (Meme meme: filteredMemes) {
            String fullPath = getFullPath(exchange, meme.getTitle());
            int[] memeSize = getMemeSizes(fullPath);

            JSONObject memeObject = new JSONObject()
                .put("path", fullPath)
                .put("width", memeSize[0])
                .put("height", memeSize[1])
                .put("title", meme.getTitle())
            ;

            filteredMemesArray.put(memeObject);
            System.out.println("    " + meme.getTitle());
        }

        System.out.println("}\n");
        return filteredMemesArray;
    }


    private String getFullPath(HttpExchange exchange, String title) {
        return exchange.getLocalAddress().getHostName()+"/memes/"+title;
    }


    private int[] getMemeSizes(String path) {
        ImageIcon meme = new ImageIcon(path);
        int[] size = {0, 0};
        size[0] = meme.getIconWidth();
        size[1] = meme.getIconHeight();

        return size;
    }

}
