package code.backend;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Database {


    private Connection connection = null;
    private static Database instance = null;

    private static final String ERROR_MESSAGE = " - DATABASE: ";


    
    /**
    * Creates database
    *
    * @param  databaseName  Name of the .dp database file
    */
    private Database(String databaseName) throws SQLException {

        // Create connection session for SQLite database
        String database = "jdbc:sqlite:"+databaseName;
        this.connection = DriverManager.getConnection(database);

        // Create users table
        String userTable =  "CREATE TABLE IF NOT EXISTS User (" +
                                "name varchar(50) NOT NULL, " +
                                "password TEXT NOT NULL, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ")";

        // Create tags table
        String tagsTable =  "CREATE TABLE IF NOT EXISTS Tag (" +
                                "title VARCHAR(20) NOT NULL UNIQUE, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ")"; 
        
        // Create memes table
        String memesTable =  "CREATE TABLE IF NOT EXISTS Meme (" +
                                "title VARCHAR(50) NOT NULL UNIQUE, " +
                                "likes INTEGER NOT NULL, " +
                                "username VARCHAR(50) NOT NULL, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "FOREIGN KEY (username) REFERENCES User(name)" +
                             ")";

        // Create hasTag table
        String hasTag = "CREATE TABLE IF NOT EXISTS HasTag (" +
                            "tagId INTEGER NOT NULL, " +
                            "memeId INTEGER NOT NULL, " +
                            "FOREIGN KEY (tagId) REFERENCES Tag(id) ON DELETE CASCADE, " +
                            "FOREIGN KEY (memeId) REFERENCES Meme(id) ON DELETE CASCADE, " +
                            "UNIQUE (tagId, memeId)" +
                        ")";
                                
        // Add all tables to database
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(userTable);
            statement.executeUpdate(tagsTable);
            statement.executeUpdate(memesTable);
            statement.executeUpdate(hasTag);
        }

        // Add default user to the table
        createDefaultUser("u");

        System.out.println("\nDatabase created");
    }


    /**
     * Creates hard coded default user for testing
     * 
     * @param  username Hard coded username
     */
    private void createDefaultUser(String username) throws JSONException, SQLException {
        if (!isRegistered(username)) {
            addUser(username, "p");
        }
    }




    /**
    * Creates new database if it isn't yet created
    *
    * @param  databaseName  Name of the .dp database file
    * @return Existing database
    */
    public static synchronized Database open(String databaseName) throws SQLException {
        if (instance == null) {
            instance = new Database(databaseName);
        }

        return instance;
    }




    /**
    * Close the connection to the database
    *
    */
    public void close() throws SQLException {
        if (connection == null) {
            return;
        }

        connection.close();
        System.out.println("Database closed\n\n");
        connection = null;
    }





// ▛               ▜
//    User methdos 
// ▙               ▟



    /**
    * Check is the user registered to the database.
    *
    * @param  username  User's username
    * @return Boolean value wheter the user is registered or not
    */
    private boolean isRegistered(String username) throws SQLException {

        // Set the SQL command
        String command = "SELECT COUNT(*) FROM User WHERE name = ?";
        
        // Get the count of given users in the table
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, username);
            try (ResultSet users = statement.executeQuery()) {

                // Check is the user already registered
                return users.next() && users.getInt(1) > 0; // User is registered, if the count > 0
            }
        }
    }



    /**
    * Adds user to the database.
    *
    * @param  username User's username
    * @param  password User's password
    * @throws IllegalArgumentException If user is already added to the database
    */
    public void addUser(String username, String password) throws SQLException, IllegalArgumentException {

        // Check is the user registered already
        if (isRegistered(username)) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "User already registered\n");
        }

        // Set the SQL command
        String command = "INSERT INTO User(name, password) VALUES(?, ?)";

        // Send user's credentials to database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, username);
            statement.setString(2, encryptPassword(password));

            statement.executeUpdate();
        }
    }




    /**
     * Encrypt the password with random salt.
     * 
     * @param  password Password to be encrypt
     * @return Encrypted user JSON object string
     */
    private String encryptPassword(String password) {

        // Create salt
        byte[] bytes = new byte[13];
        SecureRandom secure = new SecureRandom();
        secure.nextBytes(bytes);
        String saltBytes = Base64.getEncoder().encodeToString(bytes);
        String salt = ("$6$" + saltBytes).replace("+", "a");

        // Ecrypt the password
        return Crypt.crypt(password, salt);
    }



    /**
     * Checks is the given username-password combination valid.
     * 
     * @param  username User's username
     * @param  password Checkable password
     * @return Validity of the user
     */
    public boolean isValidUser(String username, String password) throws SQLException {

        // Get user's valid password
        String validPassword = getPassword(username);

        // Check the validity of the given password
        return validPassword.equals(Crypt.crypt(password, validPassword));
    }
    
    
    /**
    * Gets user's crypted password.
    *
    * @param  username User's username
    * @return User's crypted password
    * @throws IllegalArgumentException If user is not found
    */
    private String getPassword(String username) throws SQLException, IllegalArgumentException {

        // Set the SQL command
        String command = "SELECT password FROM User WHERE name = ?";

        // Get the given user from users table
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, username);
            try (ResultSet user = statement.executeQuery()) {
                
                // Does data of the user exist
                if (user.next()) {
                    return user.getString("password");
                }

                // User is not found from users table
                else {
                    throw new IllegalArgumentException(ERROR_MESSAGE + "User not found\n");
                }
            }
        }
    }





// ▛              ▜
//    Tag methdos 
// ▙              ▟



    /**
     * Add new tag to the database. If the given tag isn't unique, nothing happens.
     * 
     * @param tag Tag to be added
     */
    public void addNewTag(Tag tag) throws SQLException {

        // Set the SQL command
        String command = "INSERT INTO Tag(title) VALUES(?)";

        // Send the tag to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());

            statement.executeUpdate();
        }

        // If the tag is already added, then continue
        catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                throw new SQLException(e.getMessage());
            }
        }
    }


    /**
     * Gets all tags from the database as JSON array. If any tags haven't found the set is empty.
     * 
     * @return All tags as JSON array
     */
    public JSONArray getTagArray() throws SQLException {

        // Create new JSON array
        JSONArray tagArray = new JSONArray();

        // Set SQL command
        String command = "SELECT title FROM tags";

        // Seek all tags
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            try (ResultSet tags = statement.executeQuery()) {

                // Iterate all tags
                while (tags.next()) {

                    // Create tag
                    String title = tags.getString("title");
                    int count = getMemeTagCount(title);
                    Tag tag = new Tag(title, count);

                    // Add the tag to the set
                    tagArray.put(tag.toJSONString());
                }                
            }
        }

        return tagArray;
    }


    /**
     * Deletes the given tag from the database
     * 
     * @param tagTitle Tag to be deleted
     */
    public void deleteTag(String tagTitle) throws SQLException {

        // Set SQL command
        String command =
            "DELETE FROM Tag" +
            "WHERE title = ?"
        ;

        // Delete tag
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tagTitle);

            statement.executeUpdate();
        }
    }





// ▛                ▜
//    Meme methdos 
// ▙                ▟



    /**
     * Adds the given meme to the database, if it has unique title
     * 
     * @param meme Meme to be added
     * @throws IllegalArgumentException Meme don't have unique title
     */
    public void addMeme(Meme meme, String username) throws SQLException, IllegalArgumentException {

        // Set SQL command
        String command = 
            "INSERT INTO Meme(title, likes, username) " +
            "VALUES(?, ?, ?)"
        ;

        // Send the meme to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, meme.getTitle());
            statement.setInt(2, meme.getLikes());
            statement.setString(3, username);

            statement.executeUpdate();
        }

        // Edit possibly unique SQL error to be IllegalArgumentException
        catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                throw new IllegalArgumentException(": Meme \"" + meme.getTitle() + "\" is already added. Title of the meme must be unique one.");
            }
            throw e;
        }

        // Iterate all tags
        for (Tag tag: meme.getTagsSet()) {
            // Add tag to the database, if it's new
            addNewTag(tag);

            // Link the meme and the tag
            addTagOfTheMeme(meme, tag);
        }
    }

    
    /**
     * Gets all memes as array list from the database
     * 
     * @return All memes array list
     */
    public List<Meme> getMemesList() throws SQLException {
        List<Meme> memeList = new ArrayList<>();

        // Set SQL command
        String command = "SELECT title, likes, id FROM Meme";

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create meme
                    String title = memes.getString("title");
                    int id = memes.getInt("id");
                    int likes = memes.getInt("likes");
                    JSONArray tags = getMemeTags(id);
                    Meme meme = new Meme(title, tags, id, likes);

                    // Add the meme to the Map
                    memeList.add(meme);
                }
            }
        }

        return memeList;
    }


    /**
     * Gets meme by the given ID from the database
     * 
     * @param id ID of the meme
     * @return Founded meme as a list (only 1 node)
     * @throws IllegalArgumentException If no memes are found
     */
    public List<Meme> getMemeById(int id) throws SQLException {
        List<Meme> memeList = new ArrayList<>();

        // Set SQL command
        String command = "SELECT title, likes FROM Meme WHERE id = ?";

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setInt(1, id);
            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create meme
                    String title = memes.getString("title");
                    JSONArray tags = getMemeTags(id);
                    int likes = memes.getInt("likes");
                    memeList.add(new Meme(title, tags, id, likes));
                }
            }
        }

        return memeList;
    }



    /**
     * Gets memes with titles containing the given title
     * 
     * @param title Title of a meme
     * @return Founded memes list
     * @throws IllegalArgumentException If no memes are found
     */
    public List<Meme> getMemesContainingTitle(String title) throws SQLException {
        List<Meme> memeList = new ArrayList<>();

        // Set SQL command
        String command = 
            "SELECT tags, likes, id FROM memes" +
            "WHERE title LIKE ?"
        ;

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {

            // Search memes containing the title
            title = "%" + title + "%";
            statement.setString(1, title);
            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create meme
                    JSONArray tags = new JSONArray(memes.getString("tags"));
                    int likes = memes.getInt("likes");
                    int id = memes.getInt("id");
                    memeList.add(new Meme(title, tags, id, likes));
                }
            }
        }

        return memeList;
    }



    /**
     * Gets memes that contains all given tags from the database
     * 
     * @param tagSet Tags used for the search
     * @return List of the founded memes
     */
    public List<Meme> getMemesByTags(List<Tag> tagSet) throws SQLException {
        List<Meme> memeSet = new ArrayList<>();

        String placeHolders = new String();
        for (int i = 0; i < tagSet.size(); i++) {
            placeHolders += "?";
            if ((i + 1) != tagSet.size()) {
                placeHolders += ",";
            }
        }

        // Set SQL command
        String command = 
            "SELECT m.title, m.likes, m.id" +
            "FROM Meme AS m" +
            "JOIN HasTag AS ht ON m.id = ht.memeId" +
            "JOIN Tag AS t ON ht.tagId = t.id" +
            "WHERE t.title IN (" + placeHolders + ")" +
            "GROUP BY m.id"
        ;

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {

            // Bind every tag
            int index = 0;
            for (Tag tag: tagSet) {
                statement.setString(index++, tag.getTitle());
            }

            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create the meme
                    String title = memes.getString("title");
                    int likes = memes.getInt("likes");
                    int id = memes.getInt("id");
                    JSONArray tags = getMemeTags(id);
                    Meme meme = new Meme(title, tags, id, likes);

                    // Add the meme to the
                    memeSet.add(meme);
                }
            }
        }

        return memeSet;
    }


    /**
     * Edits the given meme and/or its tags
     * 
     * @param meme Meme to be edited (has possily the new title and/or tags)
     * @param newTitle New title for the meme, NULL if don't need editing
     * @return The pre-edited meme 
     * @throws IllegalArgumentException Given meme does n't exist in the database
     */
    public void editMeme(Meme meme, String newTitle, String username) throws SQLException {

        // Edit tags
        for (Tag tag: meme.getTagsSet()) {
            if (tag != null && tag.getTitle() != null) {
                editMemeTags(tag, meme.getTitle(), username);
            }
        }

        // Edit title
        if (newTitle != null && newTitle != meme.getTitle()) {
            editMemeTitle(meme, newTitle, username);
        }
    }


    /**
     * Adds the tag to the database, if the given user is the uploader of the meme. Only new tags are added.
     * 
     * @param  tag Tag to be added
     * @param  memeTitle Title of the meme
     * @param  username Uploader's username
     */
    private void editMemeTags(Tag tag, String memeTitle, String username) throws SQLException {

        // Set SQL exception
        String command = 
            "INSERT INTO HasTag(tagId, memeID)" + 
            "VALUES(Tag.id, Meme.id)"+
            "JOIN Tag" +
            "JOIN Meme" +
            "WHERE Tag.title = ? AND Meme.title = ? AND MEME.username = ?"
        ;

        // Add tag
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.setString(2, memeTitle);
            statement.setString(3, username);

            statement.executeUpdate();
        }

        // Don't throw error if the tag isn't unique
        catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                throw new SQLException(e.getMessage());
            }
        }
    }


    /**
     * Edits memes title, if the given user is the uploader of the meme.
     * 
     * @param  meme Meme to be edited
     * @param  newTitle New title of the meme
     * @param  username Uploader's name
     */
    private void editMemeTitle(Meme meme, String newTitle, String username) throws SQLException {
        try {

            // Set the SQL command
            String command = 
                "UPDATE memes" +
                "SET title = ?" +
                "WHERE title = ? AND username = ?"
            ;

        // Update the tags
            try (PreparedStatement statement = connection.prepareStatement(command)) {
                statement.setString(1, newTitle);
                statement.setString(2, meme.getTitle());
                statement.setString(3, username);

                statement.executeUpdate();
            }
        }

        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(": Meme must exist before it can be edited.");
        }
    }


    /**
     * Delete the given meme from the database. The given user must be the uploader of the meme.
     * 
     * @param  memeTitle Meme to be deleted
     * @param  username Uploader's name
     */
    public void deleteMeme(String memeTitle, String username) throws SQLException {

        // Set SQL command
        String command =
            "DELETE FROM Meme" +
            "WHERE title = ? AND username = ?"
        ;

        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, memeTitle);
            statement.setString(2, username);

            statement.executeUpdate();
        }
    }





// ▛                  ▜
//    HasTag methdos 
// ▙                  ▟



    /**
     * Add tag of the meme to the database. This table is basically a link between tables Meme and Tag
     * 
     * @param meme What meme does the tag refer to
     * @param tag One of the meme's tags
     */
    private void addTagOfTheMeme(Meme meme, Tag tag) throws SQLException {

        // Set SQL command
        String command = 
            "INSERT INTO Tag(tagId, memeId)" +
            "VALUES(Tag.id, Meme.id)" +
            "FROM Tag" +
            "JOIN Meme" +
            "WHERE" +
                "Tag.title = ? AND" +
                "Meme.title = ?"
        ;

        // Send link between meme and tag to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.setString(2, meme.getTitle());

            statement.executeUpdate();
        }
    }


    /**
     * Gets all tags of the given meme
     * 
     * @param  memeId ID of the meme
     * @return Tags JSON array
     */
    private JSONArray getMemeTags(int memeId) throws SQLException {
        JSONArray tags = new JSONArray();

        // Set SQL command
        String command = 
            "SELECT UNIQUE title" +
            "FROM Tag" +
            "JOIN HasTag ON Tag.id = HasTag.tagId" +
            "JOIN Meme" + 
            "WHERE HasTag.memeId = ?"
        ;

        // Seek the meme and tags links
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setInt(1, memeId);
            try (ResultSet tagSet = statement.executeQuery()) {

                // Iterate the found tags
                while (tagSet.next()) {
                    String memeTitle = tagSet.getString("title");

                    // Create tag JSON
                    JSONObject tag = new JSONObject();
                    tag.put("title", memeTitle);
                    tag.put("count", getMemeTagCount(memeTitle));

                    // Add to the array
                    tags.put(tag);
                }
            }
        }

        return tags;
    }


    /**
     * Gets the tag count of the given meme
     * 
     * @param  memeTitle Title of the meme
     * @return Tag count
     * @throws SQLException
     */
    private int getMemeTagCount(String memeTitle) throws SQLException {

        // Set SQl command
        String command =
            "SELECT COUNT(*) FROM HasTag" +
            "WHERE memeId = ?"
        ;

        // Get the tag count
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, memeTitle);
            try (ResultSet tagCount = statement.executeQuery()) {
                while (tagCount.next()) {
                    return tagCount.getInt(1);
                }
            }
        }

        // Return 0, if something fails
        return 0;
    }


    /**
     * Deletes the given tag from the meme
     * 
     * @param  memeTitle Title of the meme
     * @param  tagTitle Tag to be deleted
     * @throws SQLException
     */
    public void deleteMemeTag(String memeTitle, String tagTitle) throws SQLException {

        // Set SQL command
        String command = 
            "DELETE FROM HasTag" +
            "WHERE " +
                "tagId = (" +
                    "SELECT id FROM Tag" +
                    "WHERE title = ?" +
                ") AND " +
                "memeId = (" +
                    "SELECT id FROM Meme" +
                    "WHERE title = ?" +
                ")"
        ;

        // Delete the tag
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tagTitle);
            statement.setString(2, memeTitle);

            statement.executeUpdate();
        }
    }

}