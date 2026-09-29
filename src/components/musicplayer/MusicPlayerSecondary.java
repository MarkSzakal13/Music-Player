package components.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class to implement MusicPlayer. Includes methods such as skip,
 * shuffle, adjustOrder, next, previous, songs, equals, and toString.
 */
public abstract class MusicPlayerSecondary implements MusicPlayer {

    /**
     * Skips current song and set the next song as current.
     */
    @Override
    public void skip() {
        if (this.getPlaylistLength() > 0) {
            this.removeSong(this.getTrack());
        }
    }

    /**
     * Randomly shuffles the order of songs in the playlist.
     */
    @Override
    public void shuffle() {
        List<String> songs = this.songs();
        while (this.getPlaylistLength() > 0) {
            this.removeSong(this.getTrack());
        }

        Collections.shuffle(songs);

        for (String song : songs) {
            this.addSong(song);
        }
    }

    /**
     * Adjusts the order of the playlist based on the index inputted by the
     * user.
     *
     * @param song
     *            The song being moved.
     * @param index
     *            The index to which the song will be moved.
     */
    @Override
    public void adjustOrder(String song, int index) {
        List<String> songs = this.songs();

        if (songs.remove(song)) {
            int newIndex = Math.max(0, Math.min(index, songs.size()));
            songs.add(newIndex, song);

            while (this.getPlaylistLength() > 0) {
                this.removeSong(this.getTrack());
            }
            for (String currentSong : songs) {
                this.addSong(currentSong);
            }
        }
    }

    /**
     * Moves to the next song. The current song goes to the end of the
     * playlist so it is not lost.
     */
    @Override
    public void next() {
        if (this.getPlaylistLength() > 0) {
            String currentSong = this.getTrack();
            this.removeSong(currentSong);
            this.addSong(currentSong);
        }
    }

    /**
     * Moves back to the previous song, which is the last song in the
     * playlist.
     */
    @Override
    public void previous() {
        int length = this.getPlaylistLength();
        for (int i = 0; i < length - 1; i++) {
            this.next();
        }
    }

    /**
     * Returns every song in the playlist, starting with the current song.
     *
     * @return A list of the songs in order.
     */
    @Override
    public List<String> songs() {
        List<String> songs = new ArrayList<>();
        int length = this.getPlaylistLength();

        for (int i = 0; i < length; i++) {
            songs.add(this.getTrack());
            this.next();
        }

        return songs;
    }

    /**
     * Compares the current object with another object.
     *
     * @param o
     *            object being compared.
     * @return isEqual Return if the objects are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof MusicPlayer)) {
            return false;
        }
        MusicPlayer other = (MusicPlayer) o;

        return this.songs().equals(other.songs());
    }

    /**
     * Creates a hash code from the songs in the playlist.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return this.songs().hashCode();
    }

    /**
     * Converts MusicPlayerSecondary to a string.
     *
     * @return the string version of MusicPlayerSecondary.
     */
    @Override
    public String toString() {
        StringBuilder stringBuilt = new StringBuilder("{");
        List<String> songs = this.songs();

        for (int i = 0; i < songs.size(); i++) {
            stringBuilt.append(songs.get(i));
            if (i < songs.size() - 1) {
                stringBuilt.append(", ");
            }
        }

        stringBuilt.append("}");
        return stringBuilt.toString();
    }
}
