package components.musicplayer;

import static org.junit.Assert.assertEquals;

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
     * Builds a player with the given songs.
     *
     * @param songs
     *            the songs
     * @return the player
     */
    private static MusicPlayer of(String... songs) {
        MusicPlayer p = new MusicPlayerOnQueue();
        for (String s : songs) {
            p.addSong(s);
        }
        return p;
    }

    /**
     * Tests adjustOrder to the end and with an unknown song.
     */
    @Test
    public void testAdjustOrderEdges() {
        MusicPlayer p = of("A", "B", "C");
        p.adjustOrder("A", 2);
        assertEquals(java.util.List.of("B", "C", "A"), p.songs());
        p.adjustOrder("Z", 0);
        assertEquals(java.util.List.of("B", "C", "A"), p.songs());
    }

    /**
     * Tests next and previous rotate the playlist.
     */
    @Test
    public void testNextPrevious() {
        MusicPlayer p = of("A", "B", "C");
        p.next();
        assertEquals(java.util.List.of("B", "C", "A"), p.songs());
        p.previous();
        assertEquals(java.util.List.of("A", "B", "C"), p.songs());
        p.previous();
        assertEquals("C", p.getTrack());
    }

    /**
     * Tests songs() leaves the player unchanged, and shuffle keeps songs.
     */
    @Test
    public void testSongsAndShuffle() {
        MusicPlayer p = of("A", "B", "C");
        p.play();
        p.songs();
        assertEquals(3, p.getPlaylistLength());
        assertEquals(true, p.play());
        p.shuffle();
        java.util.List<String> s = new java.util.ArrayList<>(p.songs());
        java.util.Collections.sort(s);
        assertEquals(java.util.List.of("A", "B", "C"), s);
    }

    /**
     * Tests equals, hashCode and toString.
     */
    @Test
    public void testEqualsToString() {
        assertEquals(of("A", "B"), of("A", "B"));
        assertEquals(of("A", "B").hashCode(), of("A", "B").hashCode());
        assertEquals(false, of("A", "B").equals(of("B", "A")));
        assertEquals("{A, B}", of("A", "B").toString());
    }
}
