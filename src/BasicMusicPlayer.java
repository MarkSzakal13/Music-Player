import components.musicplayer.MusicPlayerOnQueue;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * A basic music player that allows the user to interact with a playlist by
 * adding songs, skipping the current track, viewing the playlist, or quitting
 * the program.
 */
public final class BasicMusicPlayer {

    /**
     * Private constructor to prevent instantiation.
     */
    private BasicMusicPlayer() {
    }

    /**
     * Reads a line as an integer; -1 if it is not a number.
     *
     * @param in
     *            input
     * @return the number, or -1
     */
    private static int readInt(Scanner in) {
        try {
            return Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Main method to run the program.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        PrintStream out = System.out;

        MusicPlayerOnQueue player = new MusicPlayerOnQueue();
        boolean running = true;

        while (running) {
            out.println("1: Add a song");
            out.println("2: Skip current track");
            out.println("3: View playlist");
            out.println("4: Quit");
            out.print("Choose an option: ");
            int choice = readInt(in);
            out.println();

            if (choice == 1) {
                out.print("Enter a song: ");
                String song = in.nextLine();
                player.addSong(song);
                out.println("Added: " + song + " to your playlist!");
            } else if (choice == 2) {
                String currentTrack = player.getTrack();
                if (currentTrack != null) {
                    out.println("Now playing: " + currentTrack);
                    player.skip();
                } else {
                    out.println("Your player is empty!");
                }
            } else if (choice == 3) {
                if (player.getPlaylistLength() > 0) {
                    out.println("Playlist:");
                    for (String track : player.songs()) {
                        out.println("- " + track);
                    }
                } else {
                    out.println("Your playlist is empty!");
                }
            } else if (choice == 4) {
                running = false;
                out.println("Goodbye!");
            } else {
                out.println("Please choose a valid option.");
            }
            out.println();
        }

        in.close();
    }
}
