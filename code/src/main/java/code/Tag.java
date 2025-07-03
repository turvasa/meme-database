package code;

import java.sql.SQLException;

public class Tag {


    private String title;
    private int count;


    
    public Tag(String title, int count) {
        this.title = title;
        this.count = count;
    }


    public String getTitle() {
        return title;
    }


    public int getCount() {
        return count;
    }



    public void increaseCount(Database database) throws SQLException {
        this.count++;
        database.increaseTagCount(this);
    }


    public void deincreaseCount(Database database) throws SQLException {
        if (count > 0) {
            this.count--;
            database.decreaseTagCount(this);
        }
    }


    

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Tag)) {
            return false;
        }

        Tag other = (Tag) object;
        return title.equals(other.getTitle());
    }


    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
