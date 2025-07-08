package code.user;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.BasicAuthenticator;

import code.Database;


public final class UserAuthenticator extends BasicAuthenticator {


    private final Database database;
    private static final String ERROR_MESSAGE = " - ATHENTICATION";


    /**
    * Creates authenticator for users
    *
    * @param database Database of the server
    */
    public UserAuthenticator(Database database) {
        super("datarecord");
        this.database = database;
    }

    
    /**
    * Checks that the given user is authenticated
    *
    * @param  username User's name
    * @param  password User's password
    * @return Is given user authenticated
    */
    @Override
    public boolean checkCredentials(String username, String password) {
        try {
            return database.isValidUser(username, password);
        } 
        
        catch (SQLException | JSONException | IllegalArgumentException e) {
            System.out.println(ERROR_MESSAGE+e.getMessage());
            return false;
        }
    }


    /**
    * Adds user to the hashmap, if it's a new one
    *
    * @param  userObj User JSON object holding all credentials
    */
    public void addUser(JSONObject userObj) throws SQLException {

        // Create new user
        User user = new User(userObj);
        String username = user.getUsername();

        // Add user to user table of the database
        database.addUser(user.toJSONString(), username);
    }
}
