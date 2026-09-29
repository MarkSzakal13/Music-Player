import java.io.File;

/**
 * Helper methods for reading a song's title and artist from its file name and
 * for formatting times.
 */
public final class TrackInfo {

    /**
     * Separator between the artist and title in a file name.
     */
    private static final String SEPARATOR = " - ";
    /**
     * Seconds in a minute.
     */
    private static final int SECONDS_PER_MINUTE = 60;

    /**
     * Private constructor so this class is not instantiated.
     */
    private TrackInfo() {
    }

    /**
     * Returns the file name without its folder or extension. Underscores are
     * turned into spaces.
     *
     * @param path
     *            The path to the song.
     * @return the cleaned up file name.
     */
    private static String baseName(String path) {
        String name = new File(path).getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replace('_', ' ').trim();
    }

    /**
     * Returns the title of a song. For a file named "Artist - Title.mp3" this
     * is "Title", otherwise it is the whole file name.
     *
     * @param path
     *            The path to the song.
     * @return the title.
     */
    public static String title(String path) {
        String name = baseName(path);
        int split = name.indexOf(SEPARATOR);
        if (split > 0) {
            name = name.substring(split + SEPARATOR.length()).trim();
        }
        return name;
    }

    /**
     * Returns the artist of a song. For a file named "Artist - Title.mp3" this
     * is "Artist", otherwise it is the name of the folder the song is in.
     *
     * @param path
     *            The path to the song.
     * @return the artist.
     */
    public static String artist(String path) {
        String name = baseName(path);
        int split = name.indexOf(SEPARATOR);
        if (split > 0) {
            return name.substring(0, split).trim();
        }

        File folder = new File(path).getParentFile();
        if (folder == null) {
            return "Unknown artist";
        }
        return folder.getName();
    }

    /**
     * Formats a number of seconds as minutes and seconds, like "3:07".
     *
     * @param seconds
     *            The time in seconds.
     * @return the formatted time.
     */
    public static String formatTime(double seconds) {
        int total = (int) Math.max(0, seconds);
        int minutes = total / SECONDS_PER_MINUTE;
        int remainder = total % SECONDS_PER_MINUTE;
        return String.format("%d:%02d", minutes, remainder);
    }
}
