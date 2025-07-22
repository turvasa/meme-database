package code.backend.user;

import org.json.JSONObject;

public final class User {

    private String username;
    private String password;
    private String email;
    private String nickname;

    private static final String ERROR_MESSAGE = " - USER: ";



    public User(JSONObject user) {
        setUsername(user.getString("username"));
        setPassword(user.getString("password"));
        setEmail(user.getString("email"));
        setNickname(user.getString("nickname"));
    }


    private void setUsername(String username) {
        isEmpty(username, "Username");
        this.username = username;
    }

    private void setPassword(String password) {
        isEmpty(password, "Password");
        this.password = password;
    }

    private void setEmail(String email) {
        isEmpty(email, "Email");
        this.email = email;
    }


    private void setNickname(String nickname) {
        isEmpty(nickname, "Nickname");
        this.nickname = nickname;
    }


    private void isEmpty(String argument, String argumentName) {
        if (argument == null || argument.isEmpty()) {
            throw new NullPointerException(ERROR_MESSAGE + argumentName+" is NULL\n");
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
        user.put("email", email);
        user.put("nickname", nickname);

        return user.toString();
    }
}
