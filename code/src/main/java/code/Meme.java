package code;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;


public class Meme implements Comparable<Meme> {

    private String title;
    private Set<Tag> tags;


    public Meme(String title, JSONArray tagsJson) {

        this.title = title;
        this.tags = new HashSet<>();

        // Add all tags to the set
        for (int i = 0; i < tagsJson.length(); i++) {
            JSONObject tag = tagsJson.getJSONObject(i);
            String tagTitle = tag.optString("title", "Title not provided");
            int tagCount = tag.getInt("count");

            tags.add(new Tag(tagTitle, tagCount));
        }
    }


    public String getTitle() {
        return this.title;
    }


    public String getTagsJsonString() {

        JSONArray tagsArray = new JSONArray();

        // Iterate the tag set
        for (Tag tag: tags) {

            // Create tag json object
            JSONObject tagJson = new JSONObject()
                .put("title", tag.getTitle())
                .put("count", tag.getCount())
            ;

            // Add the tag json to the array
            tagsArray.put(tagJson);
        }
        return tagsArray.toString();
    }


    public boolean containsTags(Set<Tag> givenTags) {
        return tags.containsAll(givenTags);
    }


    public void checkForNewTags(Set<Tag> allTags, Database database) throws SQLException {

        // Skip iteration if all tags are already added
        if (allTags.containsAll(tags)) {
            return;
        }

        // Add all missing tags
        for (Tag tag: tags) {
            if (!allTags.contains(tag)) {
                allTags.add(tag);
                database.addTag(tag);
            }
        }
    }




    @Override
    public int compareTo(Meme other) {
        return title.compareTo(other.getTitle());
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Meme)) {
            return false;
        }

        Meme other = (Meme) object;
        return title.equals(other.getTitle());
    }

    
    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
