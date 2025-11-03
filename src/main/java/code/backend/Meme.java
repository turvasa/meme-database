package code.backend;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Meme implements Comparable<Meme> {

    private String title;
    private JSONArray tags;
    private Integer tagCount;
    private Integer id;
    private Integer likes;

    private static final String ERROR_MESSAGE = "-MEME:";




    public Meme(String title, JSONArray tagsJson, int id, int likes) {

        setTitle(title);
        setTags(tagsJson);
        this.tagCount = tagsJson.length();
        setID(id);
        setLikes(likes);
   }




    public Meme(JSONObject meme) throws JSONException {
        String memeTitle = meme.getString("title");
        JSONArray tags = meme.optJSONArray("tags", null);

        setTitle(memeTitle);
        setTags(tags);
        this.tagCount = tags.length();
        setID(0);
        setLikes(0);
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

        this.tags = tags;
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


    public JSONArray getTagsJSON() {
        return tags;
    }


    public Set<Tag> getTagsSet() {
        Set<Tag> tagSet = new HashSet<>();

        // Iterate all tags
        for (int i = 0; i < tags.length(); i++) {

            // Add the tag
            JSONObject tagJson = tags.getJSONObject(i);
            String tagTitle = tagJson.getString("title");
            int tagCount = tagJson.getInt("count");
            tagSet.add(new Tag(tagTitle, tagCount));
        }
 
        return tagSet;
    }


    public Integer getTagCount() {
        return tagCount;
    }


    /*
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
    */

    public Integer getID() {
        return id;
    }


    public Integer getLikes() {
        return likes;
    }



/*
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
*/


    public JSONObject toJSONString() { 
        JSONObject memeJson = new JSONObject();

        memeJson.put("title", title);
        memeJson.put("tags", tags);
        memeJson.put("tagCount", tagCount);
        memeJson.put("id", id);
        memeJson.put("likes", likes);

        return memeJson;
    }


    @Override
    public String toString() {
        return ("Title: "+title+" - ID: "+id+" - Likes: "+likes+" - Tags: "+getTagsJSON());
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
