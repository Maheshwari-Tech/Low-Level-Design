package code.behavioral.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class IteratorDemo {
    private IteratorDemo() {
    }

    private static final class Playlist implements Iterable<String> {
        private final List<String> songs = new ArrayList<>();

        Playlist add(String song) {
            songs.add(song);
            return this;
        }

        @Override
        public Iterator<String> iterator() {
            return new PlaylistIterator(List.copyOf(songs));
        }
    }

    private static final class PlaylistIterator implements Iterator<String> {
        private final List<String> snapshot;
        private int index;

        private PlaylistIterator(List<String> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean hasNext() {
            return index < snapshot.size();
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("playlist exhausted");
            }
            return snapshot.get(index++);
        }
    }

    public static void main(String[] args) {
        Playlist playlist = new Playlist().add("Intro").add("Bridge").add("Finale");
        for (String song : playlist) {
            System.out.println(song);
        }
    }
}
