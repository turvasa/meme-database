package code;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Database {


    private Connection connection = null;
    private static Database instance = null;


    
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
        
        // Create memes table
        String memesTable =  "CREATE TABLE IF NOT EXISTS memes ("+
                                "title TEXT NOT NULL, "+
                                "tags TEXT NOT NULL, "+
                                "id INTEGER PRIMARY KEY AUTOINCREMENT"+
                             ")";
                                
        // Create tags table
        String tagsTable = "CREATE TABLE IF NOT EXISTS tags ("+
                                "tags TEXT NOT NULL"+
                            ")";

        // Add all tables to database
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(userTable);
            statement.executeUpdate(memesTable);
            statement.executeUpdate(tagsTable);
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
            defaultUser.put("userNickname", "Tatteus");
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
            throw new IllegalArgumentException("User already registered\n");
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
        encryptedUser.put("userNickname", userObj.getString("userNickname"));

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
                    throw new IllegalArgumentException("User not found\n");
                }
            }
        }
    }



    public Set<String> getTagSet() throws SQLException {

        // Get tags
        JSONArray tagsArray = getTags();
        Set<String> tagSet = new HashSet<>();

        // Put them to the Set
        for (int i = 0; i < tagsArray.length(); i++) {
            tagSet.add(tagsArray.getString(i));
        }

        return tagSet;
    }




    public void addNewTags(JSONArray tags) throws SQLException {
        JSONArray currentTags = getTags();

        // Add new tags to the table
        for (int i = 0; i < tags.length(); i++) {
            currentTags.put(tags.getString(i));
        }

        // Set SQL command
        String command = "UPDATE tags SET tags = ?";

        // Update tags
        try (PreparedStatement statement = connection.prepareStatement(command)) {
            statement.setString(1, currentTags.toString());
            statement.executeUpdate();
        }
    }


    private JSONArray getTags() throws SQLException {
        // Set SQL command
        String command = "SELECT tags FROM tags";

        try (PreparedStatement statement = connection.prepareStatement(command)) {
            try (ResultSet tags = statement.executeQuery()) {

                // Do tags exist
                if (!tags.next()) {
                    throw new NullPointerException("Tags array not found");
                }

                // Get tags
                return new JSONArray(tags.getString("tags"));
            } 
        }
    }

}