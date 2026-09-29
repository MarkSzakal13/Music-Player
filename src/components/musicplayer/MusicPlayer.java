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
     * Advances to the next track, moving the current one to the end.
     *
     * @updates this
     * @ensures playlist = #playlist[1, |#playlist|) * <#playlist[0]>
     */
    void next();

    /**
     * Goes back to the previous track, moving the last one to the front.
     *
     * @updates this
     * @ensures #playlist = playlist[1, |playlist|) * <playlist[0]>
     */
    void previous();

    /**
     * Returns the playlist in order, current track first.
     *
     * @return a copy of the playlist
     * @ensures songs = playlist
     */
    List<String> songs();
}
