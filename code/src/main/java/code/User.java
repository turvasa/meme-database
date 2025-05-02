package code;

import org.json.JSONObject;

public final class User {

    private String username;
    private String password;


    public User(JSONObject user) {
        setUsername(user.getString("username"));
        setPassword(user.getString("password"));
    }


    private void setUsername(String username) {
        isEmpty(username, "Username");
        this.username = username;
    }

    private void setPassword(String password) {
        isEmpty(password, "Password");
        this.password = password;
    }

    private void isEmpty(String argument, String argumentName) {
        if (argument == null || argument.isEmpty()) {
            throw new NullPointerException(argumentName+" is NULL\n");
        }
    }


    public String getUsername() {
        return username;
    }


    /**
     * Gets user as a JSON object string
     * 
     * @return User string
     */
    public String toJSONString() {
        JSONObject user = new JSONObject();

        // Add user's credentials to the JSON object
        user.put("username", username);
        user.put("password", password);

        return user.toString();
    }
}
