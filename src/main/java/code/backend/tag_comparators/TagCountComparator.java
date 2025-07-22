package code.backend.tag_comparators;

import java.util.Comparator;

import code.backend.handlers.Tag;



public class TagCountComparator implements Comparator<Tag> {
    
    @Override
    public int compare(Tag tagCount, Tag otherTagCount) {

        if (tagCount != null && otherTagCount != null) {
            return tagCount.compareTo(otherTagCount);
        }

        return 0;
    }
}

