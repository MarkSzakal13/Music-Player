package components.musicplayer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * {@link MusicPlayer} kernel implemented on a queue of song names.
 *
 * @convention the current track is the front of $this.playlist
 * @correspondence playlist = $this.playlist, playing = $this.isPlaying
 */
public class MusicPlayerOnQueue extends MusicPlayerSecondary {

    /**
     * The playlist; its front is the current track.
     */
    private Deque<String> playlist;

    /**
     * Whether the current track is playing.
     */
    private boolean isPlaying;

    /**
     * Constructs an empty, paused playlist.
     */
    public MusicPlayerOnQueue() {
        this.createNewRep();
    }

    /**
     * Creates new rep of MusicPlayerOnQueue.
     */
    private void createNewRep() {
        this.playlist = new ArrayDeque<>();
        this.isPlaying = false;
    }

    @Override
    public final MusicPlayerOnQueue newInstance() {
        return new MusicPlayerOnQueue();
    }

    @Override
    public final void clear() {
        this.createNewRep();
    }

    @Override
    public final void transferFrom(MusicPlayer source) {
        MusicPlayerOnQueue localSource = (MusicPlayerOnQueue) source;
        this.playlist = localSource.playlist;
        this.isPlaying = localSource.isPlaying;
        localSource.createNewRep();
    }

    @Override
    public final void addSong(String song) {
        this.playlist.addLast(song);
    }

    @Override
    public final void removeSong(String song) {
        this.playlist.removeFirstOccurrence(song);
    }

    @Override
    public final boolean play() {
        this.isPlaying = !this.playlist.isEmpty();
        return this.isPlaying;
    }

    @Override
    public final boolean pause() {
        this.isPlaying = false;
        return this.isPlaying;
    }

    @Override
    public final String getTrack() {
        return this.playlist.peekFirst();
    }

    @Override
    public final int getPlaylistLength() {
        return this.playlist.size();
    }
}
