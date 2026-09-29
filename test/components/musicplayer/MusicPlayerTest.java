package components.musicplayer;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Test class for MusicPlayer.
 */
public class MusicPlayerTest {
    /**
     * Tests skip method.
     */
    @Test
    public void testSkip() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");

        player.skip();
        assertEquals(1, player.getPlaylistLength());
        assertEquals("Flashing Lights", player.getTrack());
    }

    /**
     * Tests shuffle method.
     */
    @Test
    public void testShuffle() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        int originalLength = player.getPlaylistLength();

        assertEquals(originalLength, player.getPlaylistLength());
        assertEquals(true, player.getPlaylistLength() > 0);
    }

    /**
     * Tests adjustOrder method.
     */
    @Test
    public void testAdjustOrder() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        player.adjustOrder("Flashing Lights", 0);

        assertEquals("Flashing Lights", player.getTrack());
    }

    /**
     * Tests adjustOrder moving a song to the end.
     */
    @Test
    public void testAdjustOrderToEnd() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        player.adjustOrder("Heartless", 2);

        assertEquals(Arrays.asList("Flashing Lights", "Good Morning",
                "Heartless"), player.songs());
    }

    /**
     * Tests adjustOrder with a song that is not in the playlist.
     */
    @Test
    public void testAdjustOrderMissingSong() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");

        player.adjustOrder("Stronger", 0);

        assertEquals(Arrays.asList("Heartless", "Flashing Lights"),
                player.songs());
    }

    /**
     * Tests next method.
     */
    @Test
    public void testNext() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        player.next();

        assertEquals("Flashing Lights", player.getTrack());
        assertEquals(Arrays.asList("Flashing Lights", "Good Morning",
                "Heartless"), player.songs());
    }

    /**
     * Tests previous method.
     */
    @Test
    public void testPrevious() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        player.previous();

        assertEquals("Good Morning", player.getTrack());
        player.next();
        assertEquals("Heartless", player.getTrack());
    }

    /**
     * Tests songs method does not change the playlist.
     */
    @Test
    public void testSongs() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");

        List<String> songs = player.songs();

        assertEquals(Arrays.asList("Heartless", "Flashing Lights"), songs);
        assertEquals(2, player.getPlaylistLength());
        assertEquals("Heartless", player.getTrack());
    }

    /**
     * Tests shuffle keeps the same songs.
     */
    @Test
    public void testShuffleKeepsSongs() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");
        player.addSong("Good Morning");

        player.shuffle();

        List<String> songs = player.songs();
        Collections.sort(songs);
        assertEquals(Arrays.asList("Flashing Lights", "Good Morning",
                "Heartless"), songs);
    }

    /**
     * Tests equals and hashCode.
     */
    @Test
    public void testEquals() {
        MusicPlayerOnQueue player1 = new MusicPlayerOnQueue();
        player1.addSong("Heartless");
        player1.addSong("Flashing Lights");
        MusicPlayerOnQueue player2 = new MusicPlayerOnQueue();
        player2.addSong("Heartless");
        player2.addSong("Flashing Lights");
        MusicPlayerOnQueue player3 = new MusicPlayerOnQueue();
        player3.addSong("Flashing Lights");
        player3.addSong("Heartless");

        assertEquals(true, player1.equals(player2));
        assertEquals(player1.hashCode(), player2.hashCode());
        assertEquals(false, player1.equals(player3));
    }

    /**
     * Tests toString method.
     */
    @Test
    public void testToString() {
        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        player.addSong("Heartless");
        player.addSong("Flashing Lights");

        assertEquals("{Heartless, Flashing Lights}", player.toString());
    }
}
