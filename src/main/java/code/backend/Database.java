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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import code.backend.tag_comparators.TagTitleComparator;
import code.backend.user.User;


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
                                "username varchar(50) NOT NULL, " +
                                "user TEXT NOT NULL, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ")";

        // Create tags table
        String tagsTable =  "CREATE TABLE IF NOT EXISTS Tag (" +
                                "title VARCHAR(20) NOT NULL UNIQUE, " +
                                "count INTEGER NOT NULL, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT" +
                            ")"; 
        
        // Create memes table
        String memesTable =  "CREATE TABLE IF NOT EXISTS Meme (" +
                                "title VARCHAR(50) NOT NULL UNIQUE, " +
                                "tagCount INTEGER NOT NULL, " +
                                "likes INTEGER NOT NULL, " +
                                "userId INTEGER NOT NULL, " +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "FOREING KEY (userId) REFERENCES User(id)" +
                             ")";

        // Create hasTag table
        String hasTag = "CREATE TABLE IF NOT EXISTS HasTag (" +
                            "tagId INTEGER NOT NULL, " +
                            "memeId INTEGER NOT NULL, " +
                            "FOREING KEY (tagId) REFERENCES Tag(id) ON DELETE CASCADE" +
                            "FOREING KEY (  memeID) REDERENCES Meme(id) ON DELETE CASCADE" +
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
        // Check is the default user already added
        if (!isRegistered(username)) {

            // Create new user
            JSONObject defaultUser = new JSONObject();
            defaultUser.put("username", "u");
            defaultUser.put("password", "p");
            defaultUser.put("email", "el.psy@kongroo.com");
            defaultUser.put("nickname", "Tatteus");
            User user = new User(defaultUser);

            // Add it to the users table
            addUser(user.toJSONString(), username);
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
    * Check is the user registered to the database
    *
    * @param  username  User's username
    * @return Boolean value wheter the user is registered or not
    */
    private boolean isRegistered(String username) throws SQLException {

        // Set the SQL command
        String command = "SELECT COUNT(*) FROM User WHERE username = ?";
        
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
    * Adds user to the database
    *
    * @param  user User's credentials as JSON object string
    * @param  username  User's username
    * @throws IllegalArgumentException If user is already added to the database
    */
    public void addUser(String user, String username) throws SQLException, IllegalArgumentException {

        // Check is the user registered already
        if (isRegistered(username)) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "User already registered\n");
        }

        String encryptedUser = encryptedUser(user);

        // Set the SQL command
        String command = "INSERT INTO User(username, user) VALUES(?, ?)";

        // Send user's credentials to database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, username);
            statement.setString(2, encryptedUser);

            statement.executeUpdate();
        }
    }




    /**
     * Encrypt the user with random salt
     * 
     * @param  user User's credentials as JSON object string
     * @return Encrypted user JSON object string
     */
    private String encryptedUser(String user) {
        JSONObject userObj = new JSONObject(user);
        String password = userObj.getString("password");

        // Create salt
        byte[] bytes = new byte[13];
        SecureRandom secure = new SecureRandom();
        secure.nextBytes(bytes);
        String saltBytes = Base64.getEncoder().encodeToString(bytes);
        String salt = ("$6$" + saltBytes).replace("+", "a");

        // Ecrypt the password
        String encryptedPassword = Crypt.crypt(password, salt);

        // Create new encrypted user JSON string with
        JSONObject encryptedUser = new JSONObject();
        encryptedUser.put("username", userObj.getString("username"));
        encryptedUser.put("password", encryptedPassword);
        encryptedUser.put("email", userObj.getString("email"));
        encryptedUser.put("nickname", userObj.getString("nickname"));

        // Return encrypted user
        return encryptedUser.toString();
    }



    /**
     * Checks is the given username-password combination valid
     * 
     * @param  username User's username
     * @param  password Checkable password
     * @return Validity of the user
     */
    public boolean isValidUser(String username, String password) throws SQLException {

        // Get user's valid password and it's salt
        JSONObject user = getUser(username);
        String validPassword = user.getString("password");

        // Check the validity of the password
        return validPassword.equals(Crypt.crypt(password, validPassword));
    }
    
    
    /**
    * Gets user's credentials
    *
    * @param  username  User's username
    * @return User as JSON object
    * @throws IllegalArgumentException If user is not found
    */
    private JSONObject getUser(String username) throws SQLException, IllegalArgumentException {

        // Set the SQL command
        String command = "SELECT user FROM User WHERE username = ?";

        // Get the given user from users table
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, username);
            try (ResultSet user = statement.executeQuery()) {
                
                // Does data of the user exist
                if (user.next()) {
                    String userStr = user.getString("user");

                    return new JSONObject(userStr);
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
     * Add tag to the database
     * 
     * @param tag Tag to be added
     */
    public void addTag(Tag tag) throws SQLException {

        // Set the SQL command
        String command = "INSERT INTO Tag(title, count) VALUES(?, ?)";

        // Send the tag to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.setInt(2, tag.getCount());

            statement.executeUpdate();
        }
    }


/*
    /**
     * Gets all tags from the database
     * 
     * @return All tags as BST set. If any tags haven't found the set is empty
     *
    public TreeSet<Tag> getTagSet() throws SQLException {

        // Create new tag set
        TreeSet<Tag> tagSet = new TreeSet<>(new TagTitleComparator());

        // Set SQL command
        String command = "SELECT title, count FROM tags";

        // Seek all tags
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            try (ResultSet tags = statement.executeQuery()) {

                // Iterate all tags
                while (tags.next()) {

                    // Create tag
                    String title = tags.getString("title");
                    int count = tags.getInt("count");
                    Tag tag = new Tag(title, count);

                    // Add the tag to the set
                    tagSet.add(tag);
                }                
            }
        }

        return tagSet;
    }
*/


    /**
     * Gets 
     * 
     * @param memeId
     * @return
     * @throws SQLException
     */
    private JSONArray getMemeTags(int memeId) throws SQLException {
        JSONArray tags = new JSONArray();

        // Set SQL command
        String command = 
            "SELECT UNIQUE title, count" +
            "FROM Tag" +
            "JOIN HasTag ON Tag.id = HasTag.tagId" +
            "JOIN Meme" + 
            "WHERE HasTag.memeId = ?";

        // Seek the meme and tags links
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setInt(1, memeId);
            try (ResultSet tagSet = statement.executeQuery()) {

                // Iterate the found tags
                while (tagSet.next()) {

                    // Create tag JSON
                    JSONObject tag = new JSONObject();
                    tag.put("title", tagSet.getString("title"));
                    tag.put("count", tagSet.getString("count"));

                    // Add to the array
                    tags.put(tag);
                }
            }
        }

        return tags;
    }


    /**
     * Updates the increase (by 1) of the given tag's count to the database
     * 
     * @param tag
     */
    public void increaseTagCount(Tag tag) throws SQLException {

        // Set SQL command
        String command = 
            "UPDATE Tag " + 
            "SET count = count + 1 " +
            "WHERE title = ?"
        ;

        // Update the count increasing
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.executeUpdate();            
        }
    }


    /**
     * Updates the decrease (by 1) of the given tag's count to the database
     * 
     * @param tag
     */
    public void decreaseTagCount(Tag tag) throws SQLException {

        // Set SQL command
        String command = 
            "UPDATE Tag " +
            "SET count = count - 1 " +
            "WHERE title = ? AND count > 0"
        ;

        // Update the count decreasing
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
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
            "INSERT INTO Meme(title, likes, userId) " +
            "VALUES(?, ?, User.id)" +
            "FROM User" +
            "WHERE User.username = ?"
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

        // Link the meme and the tags
        for (Tag tag: meme.getTagsSet()) {
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
    public List<Meme> getMemesByTags(Set<Tag> tagSet) throws SQLException {
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
            "GROUP BY m.id";

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
     * Edits the given meme
     * 
     * @param meme Meme to be edited (has possily the new tags)
     * @param newTitle New title for the meme, NULL if don't need editing
     * @return The pre-edited meme 
     * @throws IllegalArgumentException Given meme does n't exist in the database
     */
    public void editMeme(Meme meme, String newTitle) throws SQLException {
        try {

            // Set the SQL command
            String command = "UPDATE memes SET tags = ?, title = ? WHERE title = ?";

            // Update the tags
            try (PreparedStatement statement = connection.prepareStatement(command)) {
                statement.setString(1, meme.getTagsJsonString());
                if (newTitle != null) statement.setString(2, newTitle);
                statement.setString(3, meme.getTitle());

                statement.executeUpdate();
            }

            jkKKkkkkkkkkkkkk
        }

        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(": Meme must exist before it can be edited.");
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
            "INSERT INTO Tag (tagId, memeId)" +
                "Tag.id, " +
                "Meme.id§" +
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




}