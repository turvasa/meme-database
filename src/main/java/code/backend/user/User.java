package code.backend.user;

import org.json.JSONObject;

public final class User {

    private String username;
    private String password;

    private static final String ERROR_MESSAGE = " - USER: ";



    public User(JSONObject user) {
        setUsername(user.getString("name"));
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
            throw new NullPointerException(ERROR_MESSAGE + argumentName+" is NULL\n");
        }
    }


    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }

}
