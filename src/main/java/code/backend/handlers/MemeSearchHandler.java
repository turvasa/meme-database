package code.backend.handlers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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




    /**
    * Handles all request for meme searching
    *
    * @param database Database of the server
    */
    public MemeSearchHandler(Database database) {
        this.database = database;
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

            // Get search details from the query
            String[] query = getQuery(exchange);
            String sortingQuerry = query[0];
            SORT_TYPE sortingType = getMemeSortType(query[1]);

            // Corresponding memes array
            JSONArray memes = filterMemes(exchange, sortingQuerry, sortingType);

            // No memes found
            if (memes.isEmpty()) {
                throw new NullPointerException("No memes found");
            }

            // Send the meme paths
            byte[] memeBytes = memes.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, memeBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(memeBytes);
            }


            System.out.println("Success");
        }

        catch (NullPointerException e) {
            exchangeMethods.errorResponse(404, e.getMessage());
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
     * @return [querry, sortingTypeString]
     * @throws NullPointerException If there are no variables given
     */
    private String[] getQuery(HttpExchange exchange) {

        String[] variableValues = {null, null};

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

                // Search querry
                case "search_query" -> variableValues[0] = setVariable(keyValue);

                // Sorting type
                case "sorting_type" -> variableValues[1] = setVariable(keyValue);
            }
        }
        return variableValues;
    }


    private String setVariable(String[] keyValue) {

        // Is value NULL
        if (keyValue[1].isEmpty()) {
            return null;
        }
        return keyValue[1].toLowerCase();
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
        if (sortType == null) return SORT_TYPE.ID;

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




    private JSONArray filterMemes(HttpExchange exchange, String sortingQuerry, SORT_TYPE sortingType) throws SQLException {
        List<Meme> filteredMemes = new ArrayList<>();

        // Querry is valid ID
        if (isValidID(sortingQuerry)) {
            findByID(filteredMemes, Integer.parseInt(sortingQuerry));
        }

        // Query is string
        else if (sortingQuerry != null) {

            // Find memes
            findByTitle(filteredMemes, sortingQuerry);
            findByTags(filteredMemes, sortingQuerry);
        }
        
        // Return the found memes as json array
        if (sortingQuerry == null) {
            filteredMemes = database.getMemesList();
        }

        return sortedMemeArray(exchange, filteredMemes, sortingType);

    }


    private boolean isValidID(String str) {
        try {
            // Is Integer
            Integer id = Integer.parseInt(str);

            // Is positive
            if (id >= 0) {
                return true;
            }
            else return false;
        }

        catch (NumberFormatException e) {
            return false;
        }
    }


    private void findByID(List<Meme> currentMemes, int id) throws SQLException {
        currentMemes.addAll(database.getMemeById(id));
    }


    private void findByTitle(List<Meme> currentMemes, String title) throws SQLException {
      currentMemes.addAll(database.getMemesContainingTitle(title)); 
    }


    private void findByTags(List<Meme> currentMemes, String querry) throws SQLException {
        List<Tag> tagList = new ArrayList<>();
        String[] tags = querry.split(" ");

        // Parse the tags from the querry
        for (String tagStr: tags) {
            tagList.add(new Tag(tagStr, 0));
        } 

        // Add non-dublicant memes
        List<Meme> foundMemes = database.getMemesByTags(tagList);
        for (Meme meme: foundMemes) {
            if (!currentMemes.contains(meme)) {
                currentMemes.add(meme);
            }
        }
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
        String memeDir = System.getProperty("user.dir") + "/memes/";
        String memePath = memeDir + title;

        // Is .png file found
        File png = new File(memePath + ".png");
        if (png.exists()) {
            return "/api/meme/dir/" + title + ".png";
        }

        // Is .gif file found
        File gif = new File(memePath + ".gif");
        if (gif.exists()) {
            return "/api/meme/dir/" + title + ".gif";
        }

        // No file found
        throw new NullPointerException("Meme file not found");
    }


    private int[] getMemeSizes(String path) {
        ImageIcon meme = new ImageIcon(path);
        int[] size = {0, 0};
        size[0] = meme.getIconWidth();
        size[1] = meme.getIconHeight();

        return size;
    }

}
