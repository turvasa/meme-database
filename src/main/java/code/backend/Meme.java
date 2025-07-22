package code.backend;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import code.backend.handlers.Tag;


public class Meme implements Comparable<Meme> {

    private String title;
    private Set<Tag> tags;
    private Integer id;
    private Integer likes;

    private static final String ERROR_MESSAGE = "-MEME:";




    public Meme(String title, JSONArray tagsJson, int id, int likes) {

        setTitle(title);
        setTags(tagsJson);
        setID(id);
        setLikes(likes);

        addTagsToSet(tagsJson);
   }




    public Meme(JSONObject meme) throws JSONException {
        String memeTitle = meme.getString("title");
        JSONArray tagsJson = meme.optJSONArray("tags", null);

        setTitle(memeTitle);
        this.tags = new HashSet<>();
        this.id = 0;
        setLikes(0);

        addTagsToSet(tagsJson);
    }



    private void addTagsToSet(JSONArray tagsJson) {
        // Iterate all tags
        for (int i = 0; i < tagsJson.length(); i++) {

            // Add the tag
            JSONObject tagJson = tagsJson.getJSONObject(i);
            String tagTitle = tagJson.getString("title");
            int tagCount = tagJson.getInt("count");
            tags.add(new Tag(tagTitle, tagCount));
        }
 
    }




    private void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            throw new NullPointerException(ERROR_MESSAGE + "Title of the meme mustn't be null or empty");
        }

        this.title = title.toLowerCase();
    }


    private void setID(int id) {
        if (id < 0) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "ID of the meme must be positive integer");
        }

        this.id = id;
    }


    private void setTags(JSONArray tags) {
        if (tags.length() == 0) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "Meme must have at least 1 tag");
        }

        this.tags = new HashSet<>();
    }


    private void setLikes(Integer likes) {
        if (likes == null || likes < 0) {
            throw new IllegalArgumentException(ERROR_MESSAGE + "Likes of the meme must be positive integer");
        }

        this.likes = likes;
    }




    public String getTitle() {
        return title;
    }


    public Set<Tag> getTags() {
        return tags;
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


    public Integer getID() {
        return id;
    }


    public Integer getLikes() {
        return likes;
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
    public String toString() {
        return ("Title: "+title+" - ID: "+id+" - Likes: "+likes+" - Tags: "+getTagsJsonString());
    }




    @Override
    public int compareTo(Meme other) {
        int titleComparasion = title.compareTo(other.getTitle());
        int idComparasion = id.compareTo(other.getID());
        int likesComparasion = likes.compareTo(other.getLikes());

        if (titleComparasion != 0) return titleComparasion;
        if (idComparasion != 0) return idComparasion;
        if (likesComparasion != 0) return likesComparasion;

        return 0;
    }


    @Override
    public boolean equals(Object other) {
        
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;

        Meme otherMeme = (Meme) other;   
        return
            title.equals(otherMeme.getTitle()) &&
            id.equals(otherMeme.getID()) &&
            likes.equals(otherMeme.getLikes());
    }

    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
