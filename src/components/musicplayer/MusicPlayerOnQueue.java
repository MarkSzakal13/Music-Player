package components.musicplayer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Class to represent MusicPlayer kernel methods including addSong, removeSong,
 * play, pause, and getTrack.
 */
public class MusicPlayerOnQueue extends MusicPlayerSecondary {

    /**
     * Initializes empty playlist. The front of the queue is the current track.
     */
    private Queue<String> playlist;
    /**
     * Initializes isPlaying as paused.
     */
    private boolean isPlaying;

    /**
     * Constructs an empty playlist and starts isPlaying as paused.
     */
    public MusicPlayerOnQueue() {
        this.createNewRep();
    }

    /**
     * Creates new rep of MusicPlayerOnQueue.
     */
    private void createNewRep() {
        this.playlist = new LinkedList<>();
        this.isPlaying = false;
    }

    /**
     * Creates a new instance of MusicPlayerOnQueue.
     */
    @Override
    public MusicPlayerOnQueue newInstance() {
        return new MusicPlayerOnQueue();
    }

    /**
     * Clears MusicPlayerOnQueue.
     */
    @Override
    public void clear() {
        this.createNewRep();
    }

    /**
     * Transfers MusicPlayerOnQueues instance to the current state.
     */
    @Override
    public void transferFrom(MusicPlayer source) {
        MusicPlayerOnQueue localSource = (MusicPlayerOnQueue) source;
        this.playlist = localSource.playlist;
        this.isPlaying = localSource.isPlaying;
        localSource.createNewRep();
    }

    /**
     * Adds a song to the playlist.
     *
     * @param song
     *            The song being added.
     */
    @Override
    public void addSong(String song) {
        this.playlist.add(song);
    }

    /**
     * Removes song from the playlist.
     *
     * @param song
     *            The song being removed.
     */
    @Override
    public void removeSong(String song) {
        Queue<String> temp = new LinkedList<>();
        boolean found = false;

        while (this.playlist.size() > 0) {
            String currentSong = this.playlist.remove();

            if (!found && currentSong.equals(song)) {
                found = true;
            } else {
                temp.add(currentSong);
            }
        }

        this.playlist = temp;
    }

    /**
     * Plays the current song.
     *
     * @return isPlaying as true if the song starts playing.
     */
    @Override
    public boolean play() {
        this.isPlaying = this.playlist.size() > 0;
        return this.isPlaying;
    }

    /**
     * Pauses the current song.
     *
     * @return isPlaying as false once the song is paused.
     */
    @Override
    public boolean pause() {
        this.isPlaying = false;
        return this.isPlaying;
    }

    /**
     * Gets the current track.
     *
     * @return Returns the current track, or null if the playlist is empty.
     */
    @Override
    public String getTrack() {
        return this.playlist.peek();
    }

    /**
     * Gets the length of the playlist.
     *
     * @return Returns the playlist length.
     */
    @Override
    public int getPlaylistLength() {
        return this.playlist.size();
    }
}
