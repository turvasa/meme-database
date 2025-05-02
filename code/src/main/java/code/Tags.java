package code;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;


public class Tags {

    
    private Set<String> tags;



    public Tags(Database database, JSONArray tagsArray) throws SQLException {

        // Get tags as Set
        tags = new HashSet<>();
        for (int i = 0; i < tagsArray.length(); i++) {
            tags.add(tagsArray.getString(i));
        }

        checkTags(database);
    }


    private void checkTags(Database database) throws SQLException {

        Set<String> currentTags = database.getTagSet();

        // Get new tags
        JSONArray newTags = new JSONArray();
        for (String tag: tags) {
            if (!currentTags.contains(tag) && tag != null && !tag.isEmpty()) {
                newTags.put(tag);
            }
        }

        // Add new tags
        if (newTags.length() != 0) {
            database.addNewTags(newTags);
        }
    }



    public Set<String> getTagSet() {
        return tags;
    }

}
