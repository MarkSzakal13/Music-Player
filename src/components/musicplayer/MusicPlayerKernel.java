package components.musicplayer;

/**
 * Kernel interface for a music player. The playlist is a queue: its front is
 * the current track.
 */
public interface MusicPlayerKernel {

    /**
     * Resets this to an empty, paused playlist.
     *
     * @clears this
     */
    void clear();

    /**
     * Returns a new, empty object of the same type.
     *
     * @return the new object
     */
    MusicPlayer newInstance();

    /**
     * Moves the contents of source into this and clears source.
     *
     * @param source
     *            the object to take the contents of
     * @replaces this
     * @clears source
     */
    void transferFrom(MusicPlayer source);

    /**
     * Adds a song to the playlist.
     *
     * @param song
     *            The name of the song being added.
     * @updates this
     * @requires song != null
     * @ensures playlist = #playlist * {song}
     */
    void addSong(String song);

    /**
     * Removes a song from the playlist.
     *
     * @param song
     *            The name of the song being removed.
     * @updates this
     * @requires playlist.length() > 0 && song != null
     * @ensures playlist = #playlist - {song}
     */
    void removeSong(String song);

    /**
     * Plays the current track.
     *
     * @return whether or not the song is playing
     * @updates this
     * @requires playlist.length() > 0
     * @ensures currentTrack != null
     */
    boolean play();

    /**
     * Pauses the current track.
     *
     * @return whether or not the song is paused
     * @updates this
     * @requires playlist.length() > 0
     * @ensures currentTrack != null
     */
    boolean pause();

    /**
     * Returns the current track.
     *
     * @return the current track
     * @requires playlist.length() > 0
     * @ensures getTrack = currentTrack
     */
    String getTrack();

    /**
     * Returns the current track.
     *
     * @return the playlists length
     * @requires playlist.length() > 0
     * @ensures getPlaylistLength = number of tracks in the playlist
     */
    int getPlaylistLength();
}
