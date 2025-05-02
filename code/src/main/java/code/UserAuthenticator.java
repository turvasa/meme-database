package code;

import java.sql.SQLException;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.BasicAuthenticator;


public final class UserAuthenticator extends BasicAuthenticator {


    private final Database database;


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
    @SuppressWarnings("UseSpecificCatch")
    public boolean checkCredentials(String username, String password) {
        try {
            return database.isValidUser(username, password);
        } 
        
        catch (SQLException | JSONException | IllegalArgumentException e) {
            System.out.println("[ERROR] - AUTHENTICATION: "+e.getMessage());
            return false;
        }

        catch (Exception e) {
            System.out.println("[ERROR] - Something went wrong with authentication: "+e.getMessage());
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
