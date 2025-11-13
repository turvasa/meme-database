package code.backend.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.fileupload.MultipartStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import code.backend.Database;
import code.backend.HttpExchangeMethods;
import code.backend.Meme;


public class MemeHandler implements HttpHandler {

    private final Database database;
    private final Map<String, String> sessions;



    public MemeHandler(Database database, Map<String, String> sessions) {
        this.database = database;
        this.sessions = sessions;
    }




    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeMethods exchangeMethods = new HttpExchangeMethods(exchange, "[ERROR] - POST: ");

        try (exchange) {
            String username = exchangeMethods.checkUserValidity(sessions);
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "POST" -> postRequest(exchange, exchangeMethods, username);

                case "PUT" -> putRequest(exchange, exchangeMethods, username);

                case "DELETE" -> deleteRequest(exchange, exchangeMethods, username);
                
                default -> exchangeMethods.errorResponse(405, ": Unsupported post method\n");
            }
        }

        catch (Exception e) {
            exchangeMethods.errorResponse(500, e.getMessage());
        } 
    }





// ▛               ▜
//    POST Request 
// ▙               ▟



    private void postRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods, String username) {
        try {

            // Content validity check
            Headers headers = exchange.getRequestHeaders();
            MultipartStream multipartStream = exchangeMethods.getMultipartContent(headers);

            // Get the meme from the content
            Meme meme = parseMultipartStream(multipartStream);

            // Add meme if its new one
            database.addMeme(meme, username);

            // Send success message
            exchange.sendResponseHeaders(200, -1);
            System.out.println("Meme added succesfully\n");
        } 
        
        catch (IOException | IllegalArgumentException | SQLException e) {
            exchangeMethods.errorResponse(406, e.getMessage());
        } 
        
        catch (JSONException e) {
            exchangeMethods.errorResponse(405, ": " + e.getMessage());
        }
    }


    private Meme parseMultipartStream(MultipartStream multipartStream) throws IOException {
        Meme meme = null;
        boolean next = true; // multipartStream.skipPreamble();

        while (next) {
            
            // Get the data
            String headers = multipartStream.readHeaders();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            multipartStream.readBodyData(output);
            byte[] data = output.toByteArray();

            // Get meme information
            if (headers.contains("meme")) {
                meme = new Meme(getMemeJson(data));
            }

            // Get the meme file and save it
            else if (headers.contains("image")) {
                saveMemeFile(data, meme);
            }

            next = multipartStream.readBoundary();
        }

        // Check the stream validity to prevent future errors
        if (meme == null) {
            throw new IllegalArgumentException("The stream must include meme information json and the meme image file.");
        }

        return meme;
    }


    private JSONObject getMemeJson(byte[] memeJsonBytes) {
        String memeJsonString = new String(memeJsonBytes, StandardCharsets.UTF_8);
        return new JSONObject(memeJsonString);
    }


    private void saveMemeFile(byte[] memeFileBytes, Meme meme) throws IOException {
        if (meme == null) {
            throw new IllegalArgumentException("The stream must have the meme information json given first.");
        }

        // Chech image validity and get its
        String memeType = getMemeFileType(memeFileBytes);

        // Check the memes directory
        File memesDirectory = new File("memes");
        if (!memesDirectory.exists()) memesDirectory.mkdirs();

        // Save the meme
        File memeFile = new File(memesDirectory + "/" + meme.getTitle() + memeType);
        try (FileOutputStream stream = new FileOutputStream(memeFile)) {
            stream.write(memeFileBytes);
        }
    }


    private String getMemeFileType(byte[] file) {

        // Check that the file is image
        if (!isValidImageFile(file)) {
            throw new IllegalArgumentException("The given file must be an image file");
        }

        // Get the type (GIF or PNG)
        if (file[0] == 0x47 && file[1] == 0x49 && file[2] == 0x46 && file[3] == 0x38) {
            return ".gif";
        }
        return ".png";
    }


    private boolean isValidImageFile(byte[] file) {
        try (InputStream stream = new ByteArrayInputStream(file)) {
            BufferedImage image = ImageIO.read(stream);
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }


    


// ▛              ▜
//    PUT Request 
// ▙              ▟



    private void putRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods, String username) {
         try {
            // Content validity check
            Headers headers = exchange.getRequestHeaders();
            String content = exchangeMethods.getContent(headers);

            // Get the meme from the content
            JSONObject memeJson = new JSONObject(content);
            Meme meme = new Meme(memeJson);

            // Edit the meme if it exists
            database.editMeme(meme, memeJson.optString("newTitle", null), username);

            // Send success message
            exchange.sendResponseHeaders(200, -1);
            System.out.println("Meme added succesfully");
        } 
        
        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(406, ": " + e.getMessage());
        }

        catch (IllegalArgumentException e) {
            exchangeMethods.errorResponse(406, e.getMessage());
        }
        
        catch (JSONException e) {
            exchangeMethods.errorResponse(405, e.getMessage());
        }
    }




// ▛                  ▜
//    DELETE Request 
// ▙                  ▟



    private void deleteRequest(HttpExchange exchange, HttpExchangeMethods exchangeMethods, String username) {
        try {
            //Content validity check
            Headers headers = exchange.getRequestHeaders();
            String content = exchangeMethods.getContent(headers);

            // Get meme and/or tag to be deleted
            JSONObject contentJSON = new JSONObject(content);
            String memeTitle = contentJSON.optString("memeTitle");

            // Delete the meme
            database.deleteMeme(memeTitle, username);
            deleteMemeFile(memeTitle);
       }

        catch (IOException | SQLException e) {
            exchangeMethods.errorResponse(406, ": " + e.getMessage());
        }

        catch (JSONException e) {
            exchangeMethods.errorResponse(405, e.getMessage());
        }
    }


    private void deleteMemeFile(String memeTitle) {

        // Delete PNG file
        try {
            File memeFile = new File("memes/" + memeTitle + ".png");
            memeFile.delete();
        }

        // Delete GIF file
        catch (Exception e) {
            File memeFile = new File("memes/" + memeTitle + ".gif");
            memeFile.delete();
        }
    }

}
