package code.backend.meme_comparators;

import java.util.Comparator;

import code.backend.Meme;



public class MemeTitleComparator implements Comparator<Meme> {
    
    @Override
    public int compare(Meme memeTitle, Meme otherMemeTitle) {

        if (memeTitle != null && otherMemeTitle != null) {
            return memeTitle.compareTo(otherMemeTitle);
        }

        return 0;
    }
}
