package code.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.Database;
import code.HttpExchangeMethods;
import code.Meme;
import code.Tag;
import code.meme_comparators.MemeTitleComparator;

public class MemePostHandler implements HttpHandler {

    private final Database database;
    private final SortedSet<Tag> allTags;
    private final TreeSet<Meme> memes;




    public MemePostHandler(Database database, SortedSet<Tag> allTags, SortedMap<Integer, Meme> memes) {
        this.database = database;
        this.allTags = allTags;

        this.memes = new TreeSet<>(new MemeTitleComparator());
        this.memes.addAll(memes.values());
    }




    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - POST");

        try (exchange) {
            exchangeMethods.checkUserValidity();
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "POST" -> postRequest(exchange, exchangeMethods);

                case "PUT" -> putRequest(exchange, exchangeMethods);
                
                default -> exchangeMethods.errorResponse(405, ": Unsupported post method\n");
            }
        }

        catch (Exception e) {
            exchangeMethods.errorResponse(500, e.getMessage());
        } 
    }



    private void postRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods) {
        try {
            // Content validity check
            Headers headers = exchange.getRequestHeaders();
            String content = exchangeMethods.getContent(headers);

            // Get the meme from the content
            Meme meme = new Meme(new JSONObject(content));

            // Add meme if its new one
            addMeme(meme);

            // Chech if meme contains new tags
            meme.checkForNewTags(allTags, database);

            // Increment tags' count
            increaseTagsCount(meme.getTags());

            // Send success message
            exchange.sendResponseHeaders(200, -1);
            System.out.println("Meme added succesfully");
        } 
        
        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(406, e.getMessage());
        } 
        
        catch (JSONException e) {
            exchangeMethods.errorResponse(405, e.getMessage());
        }
    }


    private void addMeme(Meme meme) throws SQLException {

        // Check meme title uniquety
        if (memes.contains(meme)) {
            throw new IllegalArgumentException("Meme \"" + meme.getTitle() + "\" is already added. Title of the meme must be unique one.");
        }

        // Add the meme
        memes.add(meme);
        database.addMeme(meme);
    }


    private void increaseTagsCount(Set<Tag> tags) throws SQLException {
        for (Tag tag: tags) {
            tag.increaseCount(database);
        }
    }




    private void putRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods) {
         try {
            // Content validity check
            Headers headers = exchange.getRequestHeaders();
            String content = exchangeMethods.getContent(headers);

            // Get the meme from the content
            Meme meme = new Meme(new JSONObject(content));

            // Edit the meme if it exists
            editMeme(meme);

            // Chech if meme contains new tags
            meme.checkForNewTags(allTags, database);

            // Send success message
            exchange.sendResponseHeaders(200, -1);
            System.out.println("Meme added succesfully");
        } 
        
        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(406, e.getMessage());
        } 
        
        catch (JSONException e) {
            exchangeMethods.errorResponse(405, e.getMessage());
        }
    }


    private Meme editMeme(Meme meme) throws SQLException {

        // Check if the meme exists
        if (!memes.contains(meme)) {
            throw new IllegalArgumentException("Meme must exist before editing.");
        }

        // Edit the meme to the set
        Meme currentMeme = memes.floor(meme);
        memes.remove(meme);
        memes.add(meme);

        // Edit the meme to the database
        database.editMeme(meme);

        // Edit the tag counts
        changeTagCounts(meme, currentMeme);

        return currentMeme;
    }
    

    private void changeTagCounts(Meme newMeme, Meme currentMeme) throws SQLException {
        increaseNewTagsCount(newMeme, currentMeme);
        decreaseOldTagsCount(newMeme, currentMeme);
    }


    private void increaseNewTagsCount(Meme newMeme, Meme currentMeme) throws SQLException {
        // Find new tags added to the meme (that aren't entirely new)
        for (Tag tag: newMeme.getTags()) {
           
            if (!currentMeme.getTags().contains(tag) && allTags.contains(tag)) {
                tag.increaseCount(database);
            }
        }
    }


    private void decreaseOldTagsCount(Meme newMeme, Meme currentMeme) throws SQLException {
        // Find new tags added to the meme (that aren't entirely new)
        for (Tag tag: currentMeme.getTags()) {
           
            if (!newMeme.getTags().contains(tag) && allTags.contains(tag)) {
                tag.deincreaseCount(database);
            }
        }
    }
}
