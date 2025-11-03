package code.backend.tag_comparators;

import java.util.Comparator;

import code.backend.Tag;



public class TagTitleComparator implements Comparator<Tag> {
    
    @Override
    public int compare(Tag tagTitle, Tag otherTagTitle) {

        if (tagTitle != null && otherTagTitle != null) {
            return tagTitle.compareTo(otherTagTitle);
        }

        return 0;
    }
}
