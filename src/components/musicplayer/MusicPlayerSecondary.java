package components.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Secondary methods of {@link MusicPlayer}, plus {@code equals},
 * {@code hashCode} and {@code toString}, written using only kernel methods.
 */
public abstract class MusicPlayerSecondary implements MusicPlayer {

    /**
     * Removes and returns every song, front first.
     *
     * @return the songs that were in the playlist
     */
    private List<String> drain() {
        List<String> out = new ArrayList<>();
        while (this.getPlaylistLength() > 0) {
            String song = this.getTrack();
            this.removeSong(song);
            out.add(song);
        }
        return out;
    }

    /**
     * Appends every song in order.
     *
     * @param songs
     *            the songs to add
     */
    private void refill(List<String> songs) {
        for (String song : songs) {
            this.addSong(song);
        }
    }

    @Override
    public final void skip() {
        if (this.getPlaylistLength() > 0) {
            this.removeSong(this.getTrack());
        }
    }

    @Override
    public final void shuffle() {
        List<String> songs = this.drain();
        Collections.shuffle(songs);
        this.refill(songs);
    }

    @Override
    public final void adjustOrder(String song, int index) {
        List<String> songs = this.drain();
        if (songs.remove(song)) {
            songs.add(Math.max(0, Math.min(index, songs.size())), song);
        }
        this.refill(songs);
    }

    @Override
    public final void next() {
        if (this.getPlaylistLength() > 0) {
            String current = this.getTrack();
            this.removeSong(current);
            this.addSong(current);
        }
    }

    @Override
    public final void previous() {
        for (int i = 1; i < this.getPlaylistLength(); i++) {
            this.next();
        }
    }

    @Override
    public final List<String> songs() {
        List<String> songs = this.drain();
        this.refill(songs);
        return songs;
    }

    @Override
    public final boolean equals(Object o) {
        return o == this || (o instanceof MusicPlayer other
                && this.songs().equals(other.songs()));
    }

    @Override
    public final int hashCode() {
        return this.songs().hashCode();
    }

    @Override
    public final String toString() {
        return "{" + String.join(", ", this.songs()) + "}";
    }
}
