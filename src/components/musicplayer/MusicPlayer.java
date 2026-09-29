package components.musicplayer;

import java.util.List;

/**
 * Music Player Interface.
 */
public interface MusicPlayer extends MusicPlayerKernel {

    /**
     * Skips current track.
     *
     * @updates this
     * @requires song != null
     * @ensures playlist = #playlist - {song}
     */
    void skip();

    /**
     * Songs shuffled in random order.
     *
     * @updates this
     * @requires playlist.length > 0
     * @ensures |playlist| = |#playlist|
     */
    void shuffle();

    /**
     * Song is moved to index in order.
     *
     * @param song
     *            The song being moved.
     * @param index
     *            Location to where the song is being moved.
     * @updates this
     * @requires playlist.length() > 1 && song != null
     * @ensures playlist.contains(song) && playlist.indexOf(song) == index
     */
    void adjustOrder(String song, int index);

    /**
     * Moves to the next song and puts the current song at the end.
     *
     * @updates this
     * @requires playlist.length() > 0
     * @ensures getTrack() = the song after #getTrack()
     */
    void next();

    /**
     * Moves back to the previous song and puts the last song at the front.
     *
     * @updates this
     * @requires playlist.length() > 0
     * @ensures getTrack() = the last song of #playlist
     */
    void previous();

    /**
     * Returns the songs in the playlist, starting with the current song.
     *
     * @return a list of the songs in order
     * @ensures songs = playlist
     */
    List<String> songs();
}
