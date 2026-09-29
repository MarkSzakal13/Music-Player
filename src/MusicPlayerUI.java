import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import components.musicplayer.MusicPlayer;
import components.musicplayer.MusicPlayerOnQueue;

/**
 * A desktop music player called Nocturne, built on the MusicPlayer component.
 * It shows a spinning record, a waveform that works as a seek bar, and the
 * queue of songs. The first song in the queue is always the one playing.
 */
public final class MusicPlayerUI extends JFrame {

    /**
     * Repeat modes.
     */
    private enum Repeat {
        /**
         * Stop after every song in the queue has played once.
         */
        OFF,
        /**
         * Keep looping through the queue.
         */
        ALL,
        /**
         * Keep replaying the current song.
         */
        ONE
    }

    /**
     * Serialization id.
     */
    private static final long serialVersionUID = 1L;

    static {
        // These have to be set before Java loads any fonts, or they are
        // ignored and text can look jagged.
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Milliseconds between animation frames (about 60 per second).
     */
    private static final int FRAME_DELAY = 16;
    /**
     * How fast the record spins, in degrees per second (33 1/3 rpm).
     */
    private static final double DEGREES_PER_SECOND = 200;
    /**
     * Degrees in a full turn.
     */
    private static final double FULL_TURN = 360;
    /**
     * Longest time step used for one frame, in seconds.
     */
    private static final double MAX_FRAME_TIME = 0.1;
    /**
     * How fast the arm moves toward its target.
     */
    private static final double ARM_SPEED = 4;
    /**
     * How fast the accent color changes to a new song's color.
     */
    private static final double COLOR_SPEED = 3;
    /**
     * Nanoseconds in a second.
     */
    private static final double NANOSECONDS = 1e9;
    /**
     * Seconds into a song after which "previous" restarts it instead.
     */
    private static final double RESTART_TIME = 3;
    /**
     * Seconds to jump when seeking with the keyboard.
     */
    private static final double SEEK_STEP = 5;
    /**
     * Volume change for each key press.
     */
    private static final double VOLUME_STEP = 0.05;
    /**
     * Starting volume.
     */
    private static final double START_VOLUME = 0.8;
    /**
     * How many folders deep to look when a folder is added.
     */
    private static final int FOLDER_DEPTH = 4;

    /**
     * Width of the queue panel.
     */
    private static final int QUEUE_WIDTH = 330;
    /**
     * Smallest window width.
     */
    private static final int MIN_WIDTH = 860;
    /**
     * Smallest window height.
     */
    private static final int MIN_HEIGHT = 560;
    /**
     * Space around the left side of the window.
     */
    private static final int STAGE_PADDING = 28;
    /**
     * Space around the queue panel.
     */
    private static final int QUEUE_PADDING = 18;
    /**
     * Small space between items.
     */
    private static final int SMALL_GAP = 6;
    /**
     * Medium space between items.
     */
    private static final int GAP = 14;
    /**
     * Size of the play button.
     */
    private static final int PLAY_BUTTON_SIZE = 64;
    /**
     * Size of the next and previous buttons.
     */
    private static final int SKIP_BUTTON_SIZE = 44;
    /**
     * Size of the shuffle and repeat buttons.
     */
    private static final int TOGGLE_BUTTON_SIZE = 40;
    /**
     * Size of the buttons above the queue.
     */
    private static final int SMALL_BUTTON_SIZE = 32;
    /**
     * Size of the speaker icon.
     */
    private static final int SPEAKER_SIZE = 22;
    /**
     * Font size of the song title.
     */
    private static final float TITLE_FONT = 34f;
    /**
     * Font size of the queue heading.
     */
    private static final float HEADING_FONT = 24f;
    /**
     * Font size of the artist line.
     */
    private static final float SUBTITLE_FONT = 13f;
    /**
     * Font size of small text.
     */
    private static final float SMALL_FONT = 12f;
    /**
     * Font size of the hint under the queue.
     */
    private static final float HINT_FONT = 11f;
    /**
     * Width of the queue scroll bar.
     */
    private static final int SCROLL_BAR_WIDTH = 6;
    /**
     * Scroll speed of the queue.
     */
    private static final int SCROLL_SPEED = 16;
    /**
     * Darkness of the queue panel.
     */
    private static final int QUEUE_SHADE = 70;
    /**
     * Brightness of the colored glow behind the record.
     */
    private static final int GLOW_ALPHA = 55;
    /**
     * Where the glow is, compared to the window width.
     */
    private static final float GLOW_X = 0.3f;
    /**
     * Where the glow is, compared to the window height.
     */
    private static final float GLOW_Y = 0.38f;
    /**
     * Size of the glow compared to the window.
     */
    private static final float GLOW_SIZE = 0.55f;

    /**
     * The playlist. The first song is the one loaded on the turntable.
     */
    private final MusicPlayer player = new MusicPlayerOnQueue();
    /**
     * The loaded song, or null if nothing is loaded.
     */
    private AudioTrack track;
    /**
     * Path of the loaded song.
     */
    private String loadedPath;
    /**
     * The song where playback started, used to know when the whole queue has
     * played.
     */
    private String firstSong;
    /**
     * The repeat mode.
     */
    private Repeat repeat = Repeat.OFF;
    /**
     * The volume from 0 to 1.
     */
    private double volume = START_VOLUME;
    /**
     * How far the record has turned, in degrees.
     */
    private double recordAngle;
    /**
     * Arm position from 0 (resting) to 1 (on the record).
     */
    private double armPosition;
    /**
     * The accent color, which slowly changes to match the current song.
     */
    private Color accent = Theme.IDLE_ACCENT;
    /**
     * Time of the last animation frame, in nanoseconds.
     */
    private long lastFrameTime = System.nanoTime();
    /**
     * Folder that was last used in a file window.
     */
    private File lastFolder;

    /**
     * The songs shown in the queue.
     */
    private final DefaultListModel<String> queueModel =
            new DefaultListModel<>();
    /**
     * The queue list.
     */
    private final JList<String> queue = new JList<>(this.queueModel);
    /**
     * Draws each row of the queue.
     */
    private final QueueCellRenderer queueRenderer = new QueueCellRenderer();
    /**
     * The spinning record.
     */
    private final Turntable turntable = new Turntable();
    /**
     * The waveform seek bar.
     */
    private final WaveformBar waveform = new WaveformBar();
    /**
     * The volume slider.
     */
    private final VolumeSlider volumeSlider;
    /**
     * Title of the current song.
     */
    private final JLabel titleLabel;
    /**
     * Artist of the current song, or a message.
     */
    private final JLabel subtitleLabel;
    /**
     * Time played so far.
     */
    private final JLabel elapsedLabel;
    /**
     * Length of the song.
     */
    private final JLabel lengthLabel;
    /**
     * Number of songs in the queue.
     */
    private final JLabel countLabel;
    /**
     * The play and pause button.
     */
    private final IconButton playButton;
    /**
     * The shuffle button.
     */
    private final IconButton shuffleButton;
    /**
     * The repeat button.
     */
    private final IconButton repeatButton;
    /**
     * Every icon button, so they can all get the accent color.
     */
    private final List<IconButton> buttons = new ArrayList<>();

    /**
     * Creates the player window.
     */
    public MusicPlayerUI() {
        super("Nocturne");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.titleLabel = createLabel("", Theme.SERIF, TITLE_FONT, Theme.TEXT);
        this.subtitleLabel = createLabel("", Theme.SANS, SUBTITLE_FONT,
                Theme.MUTED_TEXT);
        this.elapsedLabel = createLabel("0:00", Theme.SANS, SMALL_FONT,
                Theme.MUTED_TEXT);
        this.lengthLabel = createLabel("0:00", Theme.SANS, SMALL_FONT,
                Theme.MUTED_TEXT);
        this.countLabel = createLabel("", Theme.SANS, SMALL_FONT,
                Theme.MUTED_TEXT);
        this.playButton = this.createButton(IconButton.Icon.PLAY,
                PLAY_BUTTON_SIZE, true, "Play / pause (Space)");
        this.shuffleButton = this.createButton(IconButton.Icon.SHUFFLE,
                TOGGLE_BUTTON_SIZE, false, "Shuffle");
        this.repeatButton = this.createButton(IconButton.Icon.REPEAT,
                TOGGLE_BUTTON_SIZE, false, "Repeat: off");
        this.volumeSlider = new VolumeSlider(this.volume, this::setVolume);
        this.volumeSlider.setToolTipText("Volume (Up / Down)");

        JPanel root = new Backdrop();
        root.setLayout(new BorderLayout());
        root.add(this.buildStage(), BorderLayout.CENTER);
        root.add(this.buildQueuePanel(), BorderLayout.EAST);
        this.setContentPane(root);

        this.setTransferHandler(new DropHandler());
        this.queue.setTransferHandler(new DropHandler());
        this.bindKeys();
        this.refresh();

        Timer timer = new Timer(FRAME_DELAY, e -> this.animate());
        timer.start();

        this.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        this.setSize(Math.min(this.getWidth(), screen.width),
                Math.min(this.getHeight(), screen.height));
        this.setMinimumSize(new Dimension(Math.min(MIN_WIDTH, screen.width),
                Math.min(MIN_HEIGHT, screen.height)));
        this.setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Builds the left side of the window: the record, song title, waveform,
     * and playback buttons.
     *
     * @return the panel.
     */
    private JComponent buildStage() {
        JPanel stage = transparentPanel(new BorderLayout());
        stage.setBorder(BorderFactory.createEmptyBorder(STAGE_PADDING,
                STAGE_PADDING, STAGE_PADDING, STAGE_PADDING));

        this.turntable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                MusicPlayerUI.this.togglePlay();
            }
        });
        stage.add(this.turntable, BorderLayout.CENTER);

        JPanel info = transparentPanel(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        this.titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.add(this.titleLabel);
        info.add(Box.createVerticalStrut(SMALL_GAP));
        info.add(this.subtitleLabel);
        info.add(Box.createVerticalStrut(GAP));

        JPanel seekRow = transparentPanel(new BorderLayout(GAP, 0));
        seekRow.add(this.elapsedLabel, BorderLayout.WEST);
        seekRow.add(this.waveform, BorderLayout.CENTER);
        seekRow.add(this.lengthLabel, BorderLayout.EAST);
        info.add(seekRow);
        info.add(Box.createVerticalStrut(GAP));

        info.add(this.buildTransport());
        info.add(Box.createVerticalStrut(GAP));

        JPanel volumeRow = transparentPanel(
                new FlowLayout(FlowLayout.CENTER, SMALL_GAP, 0));
        IconButton speaker = this.createButton(IconButton.Icon.VOLUME,
                SPEAKER_SIZE, false, null);
        speaker.setEnabled(false);
        volumeRow.add(speaker);
        volumeRow.add(this.volumeSlider);
        info.add(volumeRow);

        stage.add(info, BorderLayout.SOUTH);
        return stage;
    }

    /**
     * Builds the row of playback buttons.
     *
     * @return the panel.
     */
    private JComponent buildTransport() {
        IconButton previous = this.createButton(IconButton.Icon.PREVIOUS,
                SKIP_BUTTON_SIZE, false, "Previous (Ctrl+Left)");
        IconButton next = this.createButton(IconButton.Icon.NEXT,
                SKIP_BUTTON_SIZE, false, "Next (Ctrl+Right)");

        previous.addActionListener(e -> this.playPrevious());
        next.addActionListener(e -> this.playNext());
        this.playButton.addActionListener(e -> this.togglePlay());
        this.shuffleButton.addActionListener(e -> this.shuffleQueue());
        this.repeatButton.addActionListener(e -> this.cycleRepeat());

        JPanel transport = transparentPanel(
                new FlowLayout(FlowLayout.CENTER, GAP, 0));
        transport.add(this.shuffleButton);
        transport.add(previous);
        transport.add(this.playButton);
        transport.add(next);
        transport.add(this.repeatButton);
        return transport;
    }

    /**
     * Builds the right side of the window: the queue.
     *
     * @return the panel.
     */
    private JComponent buildQueuePanel() {
        JPanel side = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, QUEUE_SHADE));
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                g.setColor(Theme.LINE);
                g.drawLine(0, 0, 0, this.getHeight());
            }
        };
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(QUEUE_WIDTH, 0));
        side.setBorder(BorderFactory.createEmptyBorder(STAGE_PADDING,
                QUEUE_PADDING, QUEUE_PADDING, QUEUE_PADDING));

        side.add(this.buildQueueHeader(), BorderLayout.NORTH);

        this.setUpQueueList();
        JScrollPane scroll = new JScrollPane(this.queue);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        scroll.getVerticalScrollBar()
                .setPreferredSize(new Dimension(SCROLL_BAR_WIDTH, 0));
        side.add(scroll, BorderLayout.CENTER);

        JLabel hint = createLabel("<html>Drop files or folders anywhere. "
                + "Double-click to play, right-click for more.</html>",
                Theme.SANS, HINT_FONT, Theme.MUTED_TEXT);
        hint.setBorder(BorderFactory.createEmptyBorder(GAP, 0, 0, 0));
        side.add(hint, BorderLayout.SOUTH);
        return side;
    }

    /**
     * Builds the queue heading with the add, open, and save buttons.
     *
     * @return the panel.
     */
    private JComponent buildQueueHeader() {
        JPanel titles = transparentPanel(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(createLabel("Queue", Theme.SERIF, HEADING_FONT,
                Theme.TEXT));
        titles.add(this.countLabel);

        IconButton add = this.createButton(IconButton.Icon.ADD,
                SMALL_BUTTON_SIZE, false, "Add songs");
        IconButton open = this.createButton(IconButton.Icon.OPEN,
                SMALL_BUTTON_SIZE, false, "Open playlist (.m3u)");
        IconButton save = this.createButton(IconButton.Icon.SAVE,
                SMALL_BUTTON_SIZE, false, "Save playlist (.m3u)");
        add.addActionListener(e -> this.chooseFiles());
        open.addActionListener(e -> this.openPlaylist());
        save.addActionListener(e -> this.savePlaylist());

        JPanel actions = transparentPanel(
                new FlowLayout(FlowLayout.RIGHT, SMALL_GAP, SMALL_GAP));
        actions.add(add);
        actions.add(open);
        actions.add(save);

        JPanel header = transparentPanel(new BorderLayout());
        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, GAP, 0));
        return header;
    }

    /**
     * Sets up the queue list: how rows look, double-click to play,
     * right-click menu, and the Delete and Enter keys.
     */
    private void setUpQueueList() {
        this.queue.setOpaque(false);
        this.queue.setCellRenderer(this.queueRenderer);
        this.queue.setFixedCellHeight(QueueCellRenderer.ROW_HEIGHT);

        this.queue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = MusicPlayerUI.this.queue
                        .locationToIndex(e.getPoint());
                if (row >= 0 && e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    MusicPlayerUI.this.playRow(row);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                MusicPlayerUI.this.showRowMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                MusicPlayerUI.this.showRowMenu(e);
            }
        });

        bindKey(this.queue, JComponent.WHEN_FOCUSED, "DELETE", () -> {
            if (this.queue.getSelectedIndex() >= 0) {
                this.removeRow(this.queue.getSelectedIndex());
            }
        });
        bindKey(this.queue, JComponent.WHEN_FOCUSED, "ENTER", () -> {
            if (this.queue.getSelectedIndex() >= 0) {
                this.playRow(this.queue.getSelectedIndex());
            }
        });
    }

    /**
     * Shows the right-click menu for a queue row, if the mouse event is a
     * right-click.
     *
     * @param e
     *            The mouse event.
     */
    private void showRowMenu(MouseEvent e) {
        int row = this.queue.locationToIndex(e.getPoint());
        if (!e.isPopupTrigger() || row < 0) {
            return;
        }
        this.queue.setSelectedIndex(row);
        String song = this.queueModel.get(row);
        JPopupMenu menu = new JPopupMenu();

        JMenuItem playNow = new JMenuItem("Play now");
        playNow.addActionListener(event -> this.playRow(row));
        menu.add(playNow);

        if (row > 1) {
            JMenuItem playNext = new JMenuItem("Play next");
            playNext.addActionListener(event -> this.moveSong(song, 1));
            menu.add(playNext);

            JMenuItem moveUp = new JMenuItem("Move up");
            moveUp.addActionListener(event -> this.moveSong(song, row - 1));
            menu.add(moveUp);
        }
        if (row > 0 && row < this.queueModel.size() - 1) {
            JMenuItem moveDown = new JMenuItem("Move down");
            moveDown.addActionListener(event -> this.moveSong(song, row + 1));
            menu.add(moveDown);
        }

        menu.addSeparator();
        JMenuItem remove = new JMenuItem("Remove");
        remove.addActionListener(event -> this.removeRow(row));
        menu.add(remove);

        menu.show(this.queue, e.getX(), e.getY());
    }

    /**
     * Sets up the keyboard shortcuts for the whole window.
     */
    private void bindKeys() {
        JComponent root = this.getRootPane();
        int scope = JComponent.WHEN_IN_FOCUSED_WINDOW;

        bindKey(root, scope, "SPACE", this::togglePlay);
        bindKey(root, scope, "ctrl RIGHT", this::playNext);
        bindKey(root, scope, "ctrl LEFT", this::playPrevious);
        bindKey(root, scope, "shift RIGHT", () -> this.seekBy(SEEK_STEP));
        bindKey(root, scope, "shift LEFT", () -> this.seekBy(-SEEK_STEP));
        bindKey(root, scope, "UP",
                () -> this.setVolume(this.volume + VOLUME_STEP));
        bindKey(root, scope, "DOWN",
                () -> this.setVolume(this.volume - VOLUME_STEP));
    }

    // ------------------------------------------------------------------
    // Playback
    // ------------------------------------------------------------------

    /**
     * Makes sure the song at the front of the playlist is the one loaded.
     *
     * @return true if a song is loaded and can play.
     */
    private boolean loadCurrentSong() {
        String song = this.player.getTrack();
        if (song == null) {
            this.unloadSong();
            return false;
        }
        if (song.equals(this.loadedPath) && this.track != null) {
            return true;
        }

        this.unloadSong();
        this.loadedPath = song;
        try {
            this.track = new AudioTrack(new File(song));
            this.track.setVolume(this.volume);
            this.track.setOnFinished(
                    () -> SwingUtilities.invokeLater(this::songFinished));
        } catch (IOException e) {
            this.showMessage("Can't play this file: " + e.getMessage(),
                    Theme.ERROR_TEXT);
        }
        this.waveform.setTrack(this.track);
        return this.track != null;
    }

    /**
     * Unloads the current song.
     */
    private void unloadSong() {
        if (this.track != null) {
            this.track.close();
        }
        this.track = null;
        this.loadedPath = null;
        this.waveform.setTrack(null);
    }

    /**
     * Loads the song at the front of the playlist and plays it if asked.
     *
     * @param shouldPlay
     *            True to start playing.
     */
    private void showCurrentSong(boolean shouldPlay) {
        boolean loaded = this.loadCurrentSong();
        if (loaded && shouldPlay) {
            this.track.play();
            this.player.play();
        } else {
            this.player.pause();
        }
        this.refresh();
    }

    /**
     * Checks if a song is playing.
     *
     * @return true if a song is playing.
     */
    private boolean isPlaying() {
        return this.track != null && this.track.isPlaying();
    }

    /**
     * Plays or pauses. If the queue is empty, asks for songs to add.
     */
    private void togglePlay() {
        if (this.player.getPlaylistLength() == 0) {
            this.chooseFiles();
        } else if (this.isPlaying()) {
            this.track.pause();
            this.player.pause();
            this.refresh();
        } else {
            if (this.firstSong == null
                    || !this.player.songs().contains(this.firstSong)) {
                this.firstSong = this.player.getTrack();
            }
            this.showCurrentSong(true);
        }
    }

    /**
     * Skips to the next song.
     */
    private void playNext() {
        if (this.player.getPlaylistLength() > 0) {
            boolean wasPlaying = this.isPlaying();
            this.player.next();
            this.showCurrentSong(wasPlaying);
        }
    }

    /**
     * Restarts the song, or goes back to the previous song if the current
     * one just started.
     */
    private void playPrevious() {
        if (this.track != null
                && this.track.progress() * this.track.seconds() > RESTART_TIME) {
            this.track.seek(0);
        } else if (this.player.getPlaylistLength() > 0) {
            boolean wasPlaying = this.isPlaying();
            this.player.previous();
            this.showCurrentSong(wasPlaying);
        }
    }

    /**
     * Called when a song plays to the end. Moves on based on the repeat mode.
     */
    private void songFinished() {
        if (this.repeat == Repeat.ONE) {
            this.track.seek(0);
            this.track.play();
        } else {
            this.player.next();
            boolean playedAll = this.player.getTrack().equals(this.firstSong);
            this.showCurrentSong(this.repeat == Repeat.ALL || !playedAll);
        }
    }

    /**
     * Plays the song in a row of the queue.
     *
     * @param row
     *            The row to play.
     */
    private void playRow(int row) {
        for (int i = 0; i < row; i++) {
            this.player.next();
        }
        this.firstSong = this.player.getTrack();
        this.showCurrentSong(true);
    }

    /**
     * Jumps forward or back in the song.
     *
     * @param seconds
     *            How far to jump. Negative jumps back.
     */
    private void seekBy(double seconds) {
        if (this.track != null) {
            double jump = seconds / this.track.seconds();
            this.track.seek(this.track.progress() + jump);
        }
    }

    /**
     * Sets the volume.
     *
     * @param newVolume
     *            The volume from 0 to 1.
     */
    private void setVolume(double newVolume) {
        this.volume = Math.max(0, Math.min(1, newVolume));
        if (this.track != null) {
            this.track.setVolume(this.volume);
        }
        this.volumeSlider.setValue(this.volume);
    }

    /**
     * Shuffles the queue but keeps the current song playing.
     */
    private void shuffleQueue() {
        String current = this.player.getTrack();
        if (current != null) {
            this.player.shuffle();
            this.player.adjustOrder(current, 0);
            this.firstSong = current;
            this.refresh();
        }
    }

    /**
     * Switches the repeat mode from off, to all, to one, and back to off.
     */
    private void cycleRepeat() {
        if (this.repeat == Repeat.OFF) {
            this.repeat = Repeat.ALL;
        } else if (this.repeat == Repeat.ALL) {
            this.repeat = Repeat.ONE;
        } else {
            this.repeat = Repeat.OFF;
        }
        this.refresh();
    }

    // ------------------------------------------------------------------
    // Queue
    // ------------------------------------------------------------------

    /**
     * Adds songs to the queue. Folders are searched for songs.
     *
     * @param files
     *            The files and folders to add.
     */
    private void addFiles(List<File> files) {
        boolean wasEmpty = this.player.getPlaylistLength() == 0;
        List<File> songs = new ArrayList<>();
        for (File file : files) {
            findSongs(file, FOLDER_DEPTH, songs);
        }

        if (songs.isEmpty()) {
            this.showMessage("No playable files (MP3, WAV, AIFF or AU)",
                    Theme.ERROR_TEXT);
            return;
        }

        for (File song : songs) {
            this.player.addSong(song.getAbsolutePath());
        }
        if (wasEmpty) {
            this.showCurrentSong(false);
        }
        this.refresh();
    }

    /**
     * Finds the songs in a file or folder.
     *
     * @param file
     *            A song or a folder.
     * @param depth
     *            How many more folders deep to look.
     * @param songs
     *            The list the songs are added to.
     */
    private static void findSongs(File file, int depth, List<File> songs) {
        if (file.isDirectory() && depth > 0) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children);
                for (File child : children) {
                    findSongs(child, depth - 1, songs);
                }
            }
        } else if (isSong(file)) {
            songs.add(file);
        }
    }

    /**
     * Checks if a file is a song this player can open.
     *
     * @param file
     *            The file.
     * @return true if the file has a supported extension.
     */
    private static boolean isSong(File file) {
        String name = file.getName().toLowerCase();
        boolean isSong = false;
        for (String extension : AudioTrack.EXTENSIONS) {
            if (name.endsWith("." + extension)) {
                isSong = true;
            }
        }
        return file.isFile() && isSong;
    }

    /**
     * Removes a row from the queue.
     *
     * @param row
     *            The row to remove.
     */
    private void removeRow(int row) {
        String song = this.queueModel.get(row);
        if (row == 0) {
            boolean wasPlaying = this.isPlaying();
            this.player.removeSong(song);
            this.showCurrentSong(wasPlaying);
        } else {
            this.player.removeSong(song);
            this.refresh();
        }
    }

    /**
     * Moves a song to a new position in the queue.
     *
     * @param song
     *            The song to move.
     * @param row
     *            The row to move it to.
     */
    private void moveSong(String song, int row) {
        this.player.adjustOrder(song, row);
        if (!this.player.getTrack().equals(this.loadedPath)) {
            this.showCurrentSong(this.isPlaying());
        }
        this.refresh();
    }

    /**
     * Opens the computer's file window so the user can pick files.
     *
     * @param title
     *            The window title.
     * @param isSave
     *            True for a save window, false for an open window.
     * @param filter
     *            The starting file name or filter, like "*.mp3;*.wav".
     * @param extensions
     *            File extensions to show (used on computers that ignore the
     *            filter).
     * @return the files picked, or an empty array if the user cancelled.
     */
    private File[] pickFiles(String title, boolean isSave, String filter,
            String... extensions) {
        int mode = FileDialog.LOAD;
        if (isSave) {
            mode = FileDialog.SAVE;
        }
        FileDialog dialog = new FileDialog(this, title, mode);
        dialog.setMultipleMode(!isSave);
        dialog.setFile(filter);
        if (this.lastFolder != null) {
            dialog.setDirectory(this.lastFolder.getPath());
        }
        if (extensions.length > 0) {
            dialog.setFilenameFilter((folder, name) -> {
                boolean matches = false;
                for (String extension : extensions) {
                    if (name.toLowerCase().endsWith("." + extension)) {
                        matches = true;
                    }
                }
                return matches;
            });
        }

        dialog.setVisible(true);
        File[] files = dialog.getFiles();
        if (files.length > 0) {
            this.lastFolder = files[0].getParentFile();
        }
        return files;
    }

    /**
     * Lets the user pick songs to add.
     */
    private void chooseFiles() {
        File[] files = this.pickFiles("Add songs", false,
                "*.mp3;*.wav;*.aif;*.aiff;*.au", AudioTrack.EXTENSIONS);
        if (files.length > 0) {
            this.addFiles(Arrays.asList(files));
        }
    }

    /**
     * Saves the queue as an .m3u playlist file.
     */
    private void savePlaylist() {
        File[] picked = this.pickFiles("Save playlist", true, "playlist.m3u");
        if (picked.length == 0) {
            return;
        }

        File file = picked[0];
        if (!file.getName().toLowerCase().endsWith(".m3u")) {
            file = new File(file.getPath() + ".m3u");
        }
        List<String> lines = new ArrayList<>();
        lines.add("#EXTM3U");
        lines.addAll(this.player.songs());

        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            this.showMessage("Saved " + file.getName(), this.accent);
        } catch (IOException e) {
            this.showMessage("Couldn't save: " + e.getMessage(),
                    Theme.ERROR_TEXT);
        }
    }

    /**
     * Opens an .m3u playlist file and adds its songs to the queue.
     */
    private void openPlaylist() {
        File[] picked = this.pickFiles("Open playlist", false,
                "*.m3u;*.m3u8", "m3u", "m3u8");
        if (picked.length == 0) {
            return;
        }

        File playlist = picked[0];
        try {
            List<File> songs = new ArrayList<>();
            for (String line : Files.readAllLines(playlist.toPath(),
                    StandardCharsets.UTF_8)) {
                String path = line.trim();
                // Lines starting with # are comments in .m3u files.
                if (!path.isEmpty() && !path.startsWith("#")) {
                    File song = new File(path);
                    if (!song.isAbsolute()) {
                        song = new File(playlist.getParentFile(), path);
                    }
                    songs.add(song);
                }
            }
            this.addFiles(songs);
        } catch (IOException e) {
            this.showMessage("Couldn't open: " + e.getMessage(),
                    Theme.ERROR_TEXT);
        }
    }

    // ------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------

    /**
     * Updates the labels, buttons, and queue to match the playlist.
     */
    private void refresh() {
        this.queueModel.clear();
        List<String> songs = this.player.songs();
        for (String song : songs) {
            this.queueModel.addElement(song);
        }

        if (songs.isEmpty()) {
            this.countLabel.setText("empty");
        } else if (songs.size() == 1) {
            this.countLabel.setText("1 song");
        } else {
            this.countLabel.setText(songs.size() + " songs");
        }

        String current = this.player.getTrack();
        boolean failedToLoad = current != null && this.track == null
                && current.equals(this.loadedPath);
        if (current == null) {
            this.titleLabel.setText("Nothing on the turntable");
            this.showMessage(
                    "Drop MP3 or WAV files here, or press Space to browse",
                    Theme.MUTED_TEXT);
        } else {
            this.titleLabel.setText(TrackInfo.title(current));
            if (!failedToLoad) {
                this.showMessage(TrackInfo.artist(current).toUpperCase(),
                        Theme.MUTED_TEXT);
            }
        }

        if (this.repeat == Repeat.ONE) {
            this.repeatButton.setIcon(IconButton.Icon.REPEAT_ONE);
        } else {
            this.repeatButton.setIcon(IconButton.Icon.REPEAT);
        }
        this.repeatButton.setActive(this.repeat != Repeat.OFF);
        this.repeatButton.setToolTipText(
                "Repeat: " + this.repeat.name().toLowerCase());
        this.repaint();
    }

    /**
     * Shows a message under the song title.
     *
     * @param message
     *            The message.
     * @param color
     *            The text color.
     */
    private void showMessage(String message, Color color) {
        this.subtitleLabel.setText(message);
        this.subtitleLabel.setForeground(color);
    }

    /**
     * Draws one animation frame: spins the record, moves the arm, fades the
     * accent color, and updates the times.
     */
    private void animate() {
        long now = System.nanoTime();
        double frameTime = Math.min(MAX_FRAME_TIME,
                (now - this.lastFrameTime) / NANOSECONDS);
        this.lastFrameTime = now;
        boolean playing = this.isPlaying();

        if (playing) {
            this.recordAngle = (this.recordAngle
                    + DEGREES_PER_SECOND * frameTime) % FULL_TURN;
        }

        double armTarget = 0;
        if (this.track != null && (playing || this.track.progress() > 0)) {
            armTarget = 1;
        }
        this.armPosition += (armTarget - this.armPosition)
                * Math.min(1, frameTime * ARM_SPEED);

        String current = this.player.getTrack();
        Color target = Theme.IDLE_ACCENT;
        String labelText = "Nocturne";
        if (current != null) {
            labelText = TrackInfo.title(current);
            target = Theme.accentFor(labelText);
        }
        this.accent = Theme.blend(this.accent, target,
                Math.min(1, frameTime * COLOR_SPEED));

        if (playing) {
            this.playButton.setIcon(IconButton.Icon.PAUSE);
        } else {
            this.playButton.setIcon(IconButton.Icon.PLAY);
        }
        if (this.track != null) {
            double seconds = this.track.seconds();
            this.elapsedLabel.setText(
                    TrackInfo.formatTime(this.track.progress() * seconds));
            this.lengthLabel.setText(TrackInfo.formatTime(seconds));
        } else {
            this.elapsedLabel.setText("0:00");
            this.lengthLabel.setText("0:00");
        }

        for (IconButton button : this.buttons) {
            button.setAccent(this.accent);
        }
        this.waveform.setAccent(this.accent);
        this.volumeSlider.setAccent(this.accent);
        this.queueRenderer.update(this.accent, playing);
        this.turntable.update(this.recordAngle, this.armPosition, this.accent,
                labelText);
        this.getContentPane().repaint();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Creates an icon button and remembers it so it gets the accent color.
     *
     * @param icon
     *            The icon.
     * @param size
     *            The button size.
     * @param isMain
     *            True for the big play button.
     * @param tooltip
     *            Text shown when hovering, or null for none.
     * @return the button.
     */
    private IconButton createButton(IconButton.Icon icon, int size,
            boolean isMain, String tooltip) {
        IconButton button = new IconButton(icon, size, isMain);
        button.setToolTipText(tooltip);
        this.buttons.add(button);
        return button;
    }

    /**
     * Creates a label with smooth text.
     *
     * @param text
     *            The text.
     * @param family
     *            The font family.
     * @param size
     *            The font size.
     * @param color
     *            The text color.
     * @return the label.
     */
    private static JLabel createLabel(String text, String family, float size,
            Color color) {
        JLabel label = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D smooth = Theme.smooth(g);
                super.paintComponent(smooth);
                smooth.dispose();
            }
        };
        label.setFont(Theme.font(family, Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    /**
     * Creates a panel with no background.
     *
     * @param layout
     *            The layout to use, or null.
     * @return the panel.
     */
    private static JPanel transparentPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Connects a key to an action.
     *
     * @param component
     *            The component that listens for the key.
     * @param scope
     *            When the key works, like JComponent.WHEN_FOCUSED.
     * @param key
     *            The key, like "SPACE" or "ctrl RIGHT".
     * @param action
     *            What to do when the key is pressed.
     */
    private static void bindKey(JComponent component, int scope, String key,
            Runnable action) {
        component.getInputMap(scope).put(KeyStroke.getKeyStroke(key), key);
        component.getActionMap().put(key, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /**
     * The window background: a dark gradient with a glow in the current
     * song's color.
     */
    private final class Backdrop extends JPanel {

        /**
         * Serialization id.
         */
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Theme.smooth(graphics);
            int width = this.getWidth();
            int height = this.getHeight();
            Color accent = MusicPlayerUI.this.accent;

            g.setPaint(new GradientPaint(0, 0, Theme.BACKGROUND_TOP, 0, height,
                    Theme.BACKGROUND_BOTTOM));
            g.fillRect(0, 0, width, height);

            float glowSize = Math.max(width, height) * GLOW_SIZE;
            g.setPaint(new RadialGradientPaint(width * GLOW_X, height * GLOW_Y,
                    glowSize, new float[] { 0f, 1f },
                    new Color[] { Theme.withAlpha(accent, GLOW_ALPHA),
                        Theme.withAlpha(accent, 0) }));
            g.fillRect(0, 0, width, height);
            g.dispose();
        }
    }

    /**
     * Lets files and folders be dropped onto the window.
     */
    private final class DropHandler extends TransferHandler {

        /**
         * Serialization id.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport support) {
            return support
                    .isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            try {
                Object data = support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                List<File> files = new ArrayList<>();
                for (Object item : (List<?>) data) {
                    files.add((File) item);
                }
                MusicPlayerUI.this.addFiles(files);
                return true;
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }
        }
    }

    /**
     * Starts the music player. Any files or folders given on the command line
     * are added to the queue.
     *
     * @param args
     *            Files or folders to add.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Use FlatLaf if run.bat downloaded it, since it looks much
            // cleaner. Otherwise use the computer's normal look.
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            } catch (ReflectiveOperationException
                    | UnsupportedLookAndFeelException e) {
                try {
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
                } catch (ReflectiveOperationException
                        | UnsupportedLookAndFeelException e2) {
                    System.err.println("Using the default look and feel.");
                }
            }

            MusicPlayerUI window = new MusicPlayerUI();
            List<File> files = new ArrayList<>();
            for (String arg : args) {
                files.add(new File(arg));
            }
            if (!files.isEmpty()) {
                window.addFiles(files);
            }
            window.setVisible(true);
        });
    }
}
