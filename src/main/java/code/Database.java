package code;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import code.user.User;


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
        String userTable =  "CREATE TABLE IF NOT EXISTS users ("+
                                "username varchar(50) NOT NULL PRIMARY KEY, "+
                                "user TEXT NOT NULL"+
                            ")";

        // Create tags table
        String tagsTable =  "CREATE TABLE IF NOT EXISTS tags ("+
                                "title TEXT NOT NULL, "+
                                "count INTEGER"+
                            ")"; 
        
        // Create memes table
        String memesTable =  "CREATE TABLE IF NOT EXISTS memes ("+
                                "title TEXT NOT NULL, "+
                                "tags TEXT NOT NULL, "+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, "+
                                "likes INTEGER"+
                             ")";
                                
        // Add all tables to database
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(userTable);
            statement.executeUpdate(tagsTable);
            statement.executeUpdate(memesTable);
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



    /**
    * Check is the user registered to the database
    *
    * @param  username  User's username
    * @return Boolean value wheter the user is registered or not
    */
    private boolean isRegistered(String username) throws SQLException {

        // Set the SQL command
        String command = "SELECT COUNT(*) FROM users WHERE username = ?";
        
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
    public void addUser(String user, String username) throws SQLException {

        // Check is the user registered already
        if (isRegistered(username)) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "User already registered\n");
        }

        String encryptedUser = encryptedUser(user);

        // Set the SQL command
        String command = "INSERT INTO users VALUES(?, ?)";

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
    private JSONObject getUser(String username) throws SQLException {

        // Set the SQL command
        String command = "SELECT user FROM users WHERE username = ?";

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



    public void addTag(Tag tag) throws SQLException {

        // Set the SQL command
        String command = "INSERT INTO tags VALUES(?, ?)";

        // Send the tag to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.setInt(2, tag.getCount());

            statement.executeUpdate();
        }
    }


    public SortedSet<Tag> getTagSet() throws SQLException {

        // Create new tag set
        SortedSet<Tag> tagSet = new TreeSet<>();

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


    public void increaseTagCount(Tag tag) throws SQLException {

        // Set SQL command
        String command = "UPDATE tags SET count = count + 1 WHERE title = ?";

        // Update the count increasing
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.executeQuery();            
        }
    }


    public void decreaseTagCount(Tag tag) throws SQLException {

        // Set SQL command
        String command = "UPDATE tags SET count = count - 1 WHERE tag = ? AND count > 0";

        // Update the count decreasing
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, tag.getTitle());
            statement.executeQuery();            
        }
    }




    public void addMeme(Meme meme) throws SQLException {

        // Set SQL command
        String command = "INSERT INTO memes VALUES(?, ?, ?)";

        // Send the meme to the database
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, meme.getTitle());
            statement.setString(2, meme.getTagsJsonString());
            statement.setInt(3, meme.getLikes());
        }
    }


    public SortedMap<Integer, Meme> getMemesTree() throws SQLException {

        // Create new meme map
        SortedMap<Integer, Meme> memeMap = new TreeMap<>();

        // Set SQL command
        String command = "SELECT title, tags, id, likes FROM memes";

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create meme
                    String title = memes.getString("title");
                    JSONArray tags = new JSONArray(memes.getString("tags"));
                    int id = memes.getInt("id");
                    int likes = memes.getInt("likes");
                    Meme meme = new Meme(title, tags, id, likes);

                    // Add the meme to the Map
                    memeMap.put(id, meme);
                }
            }
        }

        return memeMap;
    }


    public Map<Integer, Meme> getMemeById(int id) throws SQLException {

        // Create a meme set
        Map<Integer, Meme> memeSet = new HashMap<>();

        // Set SQL command
        String command = "SELECT title, tags, likes FROM memes WHERE id = ?";

        // Seek all memes
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setInt(1, id);
            try (ResultSet memes = statement.executeQuery()) {

                // Iterate all memes
                while (memes.next()) {

                    // Create meme
                    String title = memes.getString("title");
                    JSONArray tags = new JSONArray(memes.getString("tags"));
                    int likes = memes.getInt("likes");
                    memeSet.put(id, new Meme(title, tags, id, likes));
                }
            }
        }

        return memeSet;
    }




    public void editMeme(Meme meme) throws SQLException {

        String command = "UPDATE tags FROM memes WHERE title = ?";

        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, meme.getTagsJsonString());
            statement.executeQuery();
        }
    }

}