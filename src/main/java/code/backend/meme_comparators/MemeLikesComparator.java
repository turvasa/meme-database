package code.backend.meme_comparators;

import java.util.Comparator;

import code.backend.Meme;



public class MemeLikesComparator implements Comparator<Meme> {
    
    @Override
    public int compare(Meme memeLikes, Meme otherMemeLikes) {

        if (memeLikes != null && otherMemeLikes != null) {
            return memeLikes.getLikes().compareTo(otherMemeLikes.getLikes());
        }

        return 0;
    }
}
