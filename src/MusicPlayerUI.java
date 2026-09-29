import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import components.musicplayer.MusicPlayer;
import components.musicplayer.MusicPlayerOnQueue;

/**
 * "Nocturne": a desktop music player built on {@link MusicPlayer}.
 *
 * <p>
 * A spinning vinyl record with a tonearm, a clickable waveform, and a queue
 * whose first row is always the track on the turntable. Drop audio files (or
 * folders) anywhere on the window to add them.
 */
public final class MusicPlayerUI extends JFrame {

    /** Serialization id. */
    private static final long serialVersionUID = 1L;

    // ---------------------------------------------------------------- theme

    /** Background top. */
    private static final Color BG_TOP = new Color(0x14101F);
    /** Background bottom. */
    private static final Color BG_BOTTOM = new Color(0x0A0810);
    /** Main text. */
    private static final Color INK = new Color(0xF3EEE6);
    /** Secondary text. */
    private static final Color MUTED = new Color(0x8E869C);
    /** Hairlines / panels. */
    private static final Color LINE = new Color(255, 255, 255, 22);
    /** Accent when nothing is loaded. */
    private static final Color IDLE_ACCENT = new Color(0xE8A87C);
    /** Serif family for titles. */
    private static final String SERIF = pickFont("Georgia", "Palatino Linotype",
            "DejaVu Serif", Font.SERIF);
    /** Sans family for everything else. */
    private static final String SANS = pickFont("Segoe UI", "Helvetica Neue",
            "DejaVu Sans", Font.SANS_SERIF);

    // ------------------------------------------------------------ constants

    /** Frame timer delay in ms (~60 fps). */
    private static final int FRAME_MS = 16;
    /** Record speed: 33 1/3 rpm in degrees per second. */
    private static final double DEG_PER_SEC = 200;
    /** Seconds after which "previous" restarts the track instead. */
    private static final double RESTART_AFTER = 3;
    /** Seek step for shift+arrow, seconds. */
    private static final double SEEK_STEP = 5;
    /** Volume step for up/down arrows. */
    private static final double VOLUME_STEP = 0.05;
    /** Queue column width. */
    private static final int QUEUE_W = 330;
    /** Minimum window width. */
    private static final int MIN_W = 860;
    /** Minimum window height. */
    private static final int MIN_H = 560;
    /** Folder search depth for dropped folders. */
    private static final int DROP_DEPTH = 4;
    /** Seconds per minute. */
    private static final int MINUTE = 60;

    /** Repeat modes. */
    private enum Repeat {
        /** Stop after the whole queue has played once. */
        OFF,
        /** Loop the queue forever. */
        ALL,
        /** Loop the current track. */
        ONE
    }

    // ---------------------------------------------------------------- state

    /** The playlist component; its front is the loaded track. */
    private final MusicPlayer player = new MusicPlayerOnQueue();
    /** Decoded audio for {@link MusicPlayer#getTrack()}. */
    private transient AudioTrack track;
    /** Path of {@link #track}. */
    private String loadedPath;
    /** Song where the current run through the queue started. */
    private String startSong;
    /** Repeat mode. */
    private Repeat repeat = Repeat.OFF;
    /** Volume 0..1. */
    private double volume = 0.8;
    /** Record angle in degrees. */
    private double angle;
    /** Tonearm position 0 (parked) .. 1 (on the record). */
    private double arm;
    /** Accent color, eased toward the current track's color. */
    private Color accent = IDLE_ACCENT;
    /** Cached durations per path. */
    private final Map<String, String> durations = new HashMap<>();
    /** Time of the last frame, ns. */
    private long lastFrame = System.nanoTime();
    /** Last folder used in a file dialog. */
    private File lastDir;

    // -------------------------------------------------------------- widgets

    /** Queue rows (paths), in playlist order. */
    private final DefaultListModel<String> queueModel =
            new DefaultListModel<>();
    /** Queue list. */
    private final JList<String> queue = new JList<>(this.queueModel);
    /** Track title. */
    private final JLabel title = label("", SERIF, Font.PLAIN, 34, INK);
    /** Artist / status line. */
    private final JLabel subtitle = label("", SANS, Font.PLAIN, 13, MUTED);
    /** Elapsed time. */
    private final JLabel elapsed = label("0:00", SANS, Font.PLAIN, 12, MUTED);
    /** Total time. */
    private final JLabel total = label("0:00", SANS, Font.PLAIN, 12, MUTED);
    /** Queue header count. */
    private final JLabel queueCount = label("", SANS, Font.PLAIN, 12, MUTED);
    /** Record + tonearm. */
    private final Turntable turntable = new Turntable();
    /** Seekable waveform. */
    private final Waveform waveform = new Waveform();
    /** Volume control. */
    private final Slider volumeSlider = new Slider();
    /** Play / pause. */
    private final IconButton playButton = new IconButton(Icon.PLAY, 64, true);
    /** Shuffle. */
    private final IconButton shuffleButton = new IconButton(Icon.SHUFFLE, 40,
            false);
    /** Repeat. */
    private final IconButton repeatButton = new IconButton(Icon.REPEAT, 40,
            false);

    /**
     * Builds the window.
     */
    public MusicPlayerUI() {
        super("Nocturne");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Backdrop root = new Backdrop();
        root.setLayout(new BorderLayout());
        root.add(this.buildStage(), BorderLayout.CENTER);
        root.add(this.buildQueue(), BorderLayout.EAST);
        this.setContentPane(root);

        this.setTransferHandler(new DropHandler());
        this.queue.setTransferHandler(new DropHandler());
        this.bindKeys();
        this.refresh();

        new Timer(FRAME_MS, e -> this.tick()).start();

        this.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        this.setSize(Math.min(this.getWidth(), screen.width),
                Math.min(this.getHeight(), screen.height));
        this.setMinimumSize(new Dimension(Math.min(MIN_W, screen.width),
                Math.min(MIN_H, screen.height)));
        this.setLocationRelativeTo(null);
    }

    // =============================================================== layout

    /**
     * Left side: turntable, title, waveform and transport controls.
     *
     * @return the panel
     */
    private JComponent buildStage() {
        JPanel stage = clear(new BorderLayout());
        stage.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 28));
        stage.add(this.turntable, BorderLayout.CENTER);

        JPanel info = clear(null);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        this.title.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        info.add(this.title);
        info.add(Box.createVerticalStrut(4));
        info.add(this.subtitle);
        info.add(Box.createVerticalStrut(18));

        JPanel wave = clear(new BorderLayout(12, 0));
        wave.add(this.elapsed, BorderLayout.WEST);
        wave.add(this.waveform, BorderLayout.CENTER);
        wave.add(this.total, BorderLayout.EAST);
        info.add(wave);
        info.add(Box.createVerticalStrut(16));

        IconButton prev = new IconButton(Icon.PREV, 44, false);
        IconButton next = new IconButton(Icon.NEXT, 44, false);
        prev.addActionListener(e -> this.previous());
        next.addActionListener(e -> this.next(true));
        this.playButton.addActionListener(e -> this.togglePlay());
        this.shuffleButton.addActionListener(e -> this.shuffle());
        this.repeatButton.addActionListener(e -> this.cycleRepeat());
        this.shuffleButton.setToolTipText("Shuffle");
        prev.setToolTipText("Previous (Ctrl+Left)");
        this.playButton.setToolTipText("Play / pause (Space)");
        next.setToolTipText("Next (Ctrl+Right)");

        JPanel transport = clear(new FlowLayout(FlowLayout.CENTER, 14, 0));
        transport.add(this.shuffleButton);
        transport.add(prev);
        transport.add(this.playButton);
        transport.add(next);
        transport.add(this.repeatButton);
        info.add(transport);
        info.add(Box.createVerticalStrut(14));

        JPanel vol = clear(new FlowLayout(FlowLayout.CENTER, 8, 0));
        IconButton speaker = new IconButton(Icon.VOLUME, 22, false);
        speaker.setEnabled(false);
        vol.add(speaker);
        this.volumeSlider.setToolTipText("Volume (Up / Down)");
        vol.add(this.volumeSlider);
        info.add(vol);

        stage.add(info, BorderLayout.SOUTH);
        return stage;
    }

    /**
     * Right side: the queue.
     *
     * @return the panel
     */
    private JComponent buildQueue() {
        JPanel side = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setColor(new Color(0, 0, 0, 70));
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                g.setColor(LINE);
                g.drawLine(0, 0, 0, this.getHeight());
            }
        };
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(QUEUE_W, 0));
        side.setBorder(BorderFactory.createEmptyBorder(24, 18, 18, 18));

        JPanel head = clear(new BorderLayout());
        JPanel titles = clear(null);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel h = label("Queue", SERIF, Font.PLAIN, 24, INK);
        titles.add(h);
        titles.add(this.queueCount);
        head.add(titles, BorderLayout.WEST);
        JPanel actions = clear(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        IconButton add = new IconButton(Icon.PLUS, 32, false);
        add.setToolTipText("Add audio files");
        add.addActionListener(e -> this.chooseFiles());
        IconButton open = new IconButton(Icon.OPEN, 32, false);
        open.setToolTipText("Open playlist (.m3u)");
        open.addActionListener(e -> this.openPlaylist());
        IconButton save = new IconButton(Icon.SAVE, 32, false);
        save.setToolTipText("Save playlist (.m3u)");
        save.addActionListener(e -> this.savePlaylist());
        actions.add(add);
        actions.add(open);
        actions.add(save);
        head.add(actions, BorderLayout.EAST);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        side.add(head, BorderLayout.NORTH);

        this.queue.setOpaque(false);
        this.queue.setCellRenderer(new Row());
        this.queue.setFixedCellHeight(54);
        this.queue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = MusicPlayerUI.this.queue.locationToIndex(e.getPoint());
                if (i >= 0 && e.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(e)) {
                    MusicPlayerUI.this.jumpTo(i);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                this.popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                this.popup(e);
            }

            /**
             * Shows the row menu on the platform's popup trigger.
             *
             * @param e
             *            the mouse event
             */
            private void popup(MouseEvent e) {
                int i = MusicPlayerUI.this.queue.locationToIndex(e.getPoint());
                if (e.isPopupTrigger() && i >= 0) {
                    MusicPlayerUI.this.queue.setSelectedIndex(i);
                    MusicPlayerUI.this.rowMenu(i).show(
                            MusicPlayerUI.this.queue, e.getX(), e.getY());
                }
            }
        });
        this.queue.getInputMap().put(KeyStroke.getKeyStroke("DELETE"),
                "remove");
        this.queue.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"),
                "remove");
        this.queue.getActionMap().put("remove", action(() -> {
            int i = this.queue.getSelectedIndex();
            if (i >= 0) {
                this.removeRow(i);
            }
        }));
        this.queue.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "play");
        this.queue.getActionMap().put("play", action(() -> {
            int i = this.queue.getSelectedIndex();
            if (i >= 0) {
                this.jumpTo(i);
            }
        }));

        JScrollPane scroll = new JScrollPane(this.queue);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new ThinScrollBar());
        side.add(scroll, BorderLayout.CENTER);

        JLabel hint = label("<html>Drop files or folders anywhere &middot;"
                + " double-click to play &middot; right-click for more</html>",
                SANS, Font.PLAIN, 11, MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        side.add(hint, BorderLayout.SOUTH);
        return side;
    }

    /**
     * Context menu for a queue row.
     *
     * @param i
     *            row index
     * @return the menu
     */
    private JPopupMenu rowMenu(int i) {
        JPopupMenu m = new JPopupMenu();
        String song = this.queueModel.get(i);
        JMenuItem play = new JMenuItem("Play now");
        play.addActionListener(e -> this.jumpTo(i));
        m.add(play);
        if (i > 1) {
            JMenuItem nextUp = new JMenuItem("Play next");
            nextUp.addActionListener(e -> this.move(song, 1));
            m.add(nextUp);
        }
        if (i > 1) {
            JMenuItem up = new JMenuItem("Move up");
            up.addActionListener(e -> this.move(song, i - 1));
            m.add(up);
        }
        if (i > 0 && i < this.queueModel.size() - 1) {
            JMenuItem down = new JMenuItem("Move down");
            down.addActionListener(e -> this.move(song, i + 1));
            m.add(down);
        }
        m.addSeparator();
        JMenuItem rm = new JMenuItem("Remove");
        rm.addActionListener(e -> this.removeRow(i));
        m.add(rm);
        return m;
    }

    /**
     * Global keyboard shortcuts.
     */
    private void bindKeys() {
        JComponent root = this.getRootPane();
        Object[][] keys = {
            { "SPACE", action(this::togglePlay) },
            { "ctrl RIGHT", action(() -> this.next(true)) },
            { "ctrl LEFT", action(this::previous) },
            { "shift RIGHT", action(() -> this.seekBy(SEEK_STEP)) },
            { "shift LEFT", action(() -> this.seekBy(-SEEK_STEP)) },
            { "UP", action(() -> this.setVolume(this.volume + VOLUME_STEP)) },
            { "DOWN",
                action(() -> this.setVolume(this.volume - VOLUME_STEP)) }, };
        for (Object[] k : keys) {
            String name = (String) k[0];
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(name), name);
            root.getActionMap().put(name, (AbstractAction) k[1]);
        }
        // Keep the list from stealing Up / Down.
        this.queue.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "none");
    }

    // ============================================================ playback

    /**
     * Makes sure {@link #track} holds the front of the playlist.
     *
     * @return true if a track is loaded and playable
     */
    private boolean ensureLoaded() {
        String song = this.player.getTrack();
        if (song == null) {
            this.unload();
            return false;
        }
        if (song.equals(this.loadedPath) && this.track != null) {
            return true;
        }
        this.unload();
        this.loadedPath = song;
        try {
            this.track = new AudioTrack(new File(song));
            this.track.setVolume(this.volume);
            this.track.setOnEnd(() -> SwingUtilities.invokeLater(
                    this::onTrackEnd));
            return true;
        } catch (IOException e) {
            this.subtitle.setText("Can't play this file: " + e.getMessage());
            this.subtitle.setForeground(new Color(0xFF8A80));
            return false;
        }
    }

    /**
     * Releases the loaded audio.
     */
    private void unload() {
        if (this.track != null) {
            this.track.close();
        }
        this.track = null;
        this.loadedPath = null;
    }

    /**
     * Loads the front track and starts it if requested.
     *
     * @param play
     *            whether to start playback
     */
    private void load(boolean play) {
        if (this.ensureLoaded() && play) {
            this.track.play();
            this.player.play();
        } else {
            this.player.pause();
        }
        this.refresh();
    }

    /**
     * @return whether audio is currently playing
     */
    private boolean isPlaying() {
        return this.track != null && this.track.isPlaying();
    }

    /**
     * Play / pause.
     */
    private void togglePlay() {
        if (this.player.getPlaylistLength() == 0) {
            this.chooseFiles();
            return;
        }
        if (this.isPlaying()) {
            this.track.pause();
            this.player.pause();
            this.refresh();
            return;
        }
        if (this.startSong == null
                || !this.player.songs().contains(this.startSong)) {
            this.startSong = this.player.getTrack();
        }
        this.load(true);
    }

    /**
     * Advances to the next track.
     *
     * @param keepPlaying
     *            whether to continue playing if currently playing
     */
    private void next(boolean keepPlaying) {
        if (this.player.getPlaylistLength() == 0) {
            return;
        }
        boolean playing = this.isPlaying() && keepPlaying;
        this.player.next();
        this.load(playing);
    }

    /**
     * Restarts the track, or goes back one if near its start.
     */
    private void previous() {
        if (this.track != null && this.track.progress()
                * this.track.seconds() > RESTART_AFTER) {
            this.track.seek(0);
            return;
        }
        if (this.player.getPlaylistLength() == 0) {
            return;
        }
        boolean playing = this.isPlaying();
        this.player.previous();
        this.load(playing);
    }

    /**
     * Called on the EDT when a track finishes by itself.
     */
    private void onTrackEnd() {
        if (this.repeat == Repeat.ONE) {
            this.track.seek(0);
            this.track.play();
            return;
        }
        this.player.next();
        boolean wrapped = this.player.getTrack().equals(this.startSong);
        this.load(this.repeat == Repeat.ALL || !wrapped);
    }

    /**
     * Plays the queue row at an index now.
     *
     * @param index
     *            the row
     */
    private void jumpTo(int index) {
        for (int i = 0; i < index; i++) {
            this.player.next();
        }
        this.startSong = this.player.getTrack();
        this.load(true);
    }

    /**
     * Seeks relative to the current position.
     *
     * @param seconds
     *            offset in seconds
     */
    private void seekBy(double seconds) {
        if (this.track != null) {
            this.track.seek(this.track.progress()
                    + seconds / this.track.seconds());
        }
    }

    /**
     * Sets the volume.
     *
     * @param v
     *            0..1
     */
    private void setVolume(double v) {
        this.volume = Math.max(0, Math.min(1, v));
        if (this.track != null) {
            this.track.setVolume(this.volume);
        }
        this.volumeSlider.repaint();
    }

    /**
     * Shuffles the queue but keeps the current track on the turntable.
     */
    private void shuffle() {
        String current = this.player.getTrack();
        if (current == null) {
            return;
        }
        this.player.shuffle();
        this.player.adjustOrder(current, 0);
        this.startSong = current;
        this.refresh();
    }

    /**
     * Cycles repeat off → all → one.
     */
    private void cycleRepeat() {
        this.repeat = Repeat.values()[(this.repeat.ordinal() + 1)
                % Repeat.values().length];
        this.refresh();
    }

    // =============================================================== queue

    /**
     * Adds files (and audio files inside folders) to the queue.
     *
     * @param files
     *            the files or folders
     */
    private void addFiles(List<File> files) {
        boolean wasEmpty = this.player.getPlaylistLength() == 0;
        int added = 0;
        for (File f : files) {
            for (File audio : audioFiles(f)) {
                this.player.addSong(audio.getAbsolutePath());
                added++;
            }
        }
        if (added == 0) {
            this.subtitle.setText("No playable files (WAV, AIFF or AU)");
            this.subtitle.setForeground(new Color(0xFF8A80));
            return;
        }
        if (wasEmpty) {
            this.load(false);
        }
        this.refresh();
    }

    /**
     * Expands a file or folder into playable audio files.
     *
     * @param f
     *            a file or folder
     * @return the audio files, sorted by path
     */
    private static List<File> audioFiles(File f) {
        List<File> out = new ArrayList<>();
        if (f.isDirectory()) {
            try (Stream<Path> s = Files.walk(f.toPath(), DROP_DEPTH)) {
                s.map(Path::toFile).filter(MusicPlayerUI::isAudio).sorted()
                        .forEach(out::add);
            } catch (IOException e) {
                return out;
            }
        } else if (isAudio(f)) {
            out.add(f);
        }
        return out;
    }

    /**
     * @param f
     *            a file
     * @return whether it has a supported audio extension
     */
    private static boolean isAudio(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return f.isFile() && Arrays.stream(AudioTrack.EXTENSIONS)
                .anyMatch(ext -> n.endsWith("." + ext));
    }

    /**
     * Removes a queue row.
     *
     * @param index
     *            the row
     */
    private void removeRow(int index) {
        String song = this.queueModel.get(index);
        if (index == 0) {
            boolean playing = this.isPlaying();
            this.player.removeSong(song);
            this.load(playing);
        } else {
            // ponytail: removes the first copy of a duplicated song
            this.player.removeSong(song);
            this.refresh();
        }
    }

    /**
     * Moves a song to a queue position.
     *
     * @param song
     *            the song
     * @param index
     *            target row
     */
    private void move(String song, int index) {
        this.player.adjustOrder(song, index);
        if (!this.player.getTrack().equals(this.loadedPath)) {
            this.load(this.isPlaying());
        }
        this.refresh();
    }

    /**
     * Shows the operating system's own file dialog (File Explorer on
     * Windows).
     *
     * @param title
     *            dialog title
     * @param save
     *            save dialog instead of open
     * @param multi
     *            allow selecting several files
     * @param pattern
     *            initial file name / filter, e.g. "*.wav;*.au"
     * @param exts
     *            accepted extensions (for platforms that ignore the pattern)
     * @return the chosen files, empty if cancelled
     */
    private File[] pick(String title, boolean save, boolean multi,
            String pattern, String... exts) {
        java.awt.FileDialog fd = new java.awt.FileDialog(this, title,
                save ? java.awt.FileDialog.SAVE : java.awt.FileDialog.LOAD);
        fd.setMultipleMode(multi);
        if (this.lastDir != null) {
            fd.setDirectory(this.lastDir.getPath());
        }
        fd.setFile(pattern);
        if (exts.length > 0) {
            fd.setFilenameFilter((dir, name) -> Arrays.stream(exts).anyMatch(
                    e -> name.toLowerCase(Locale.ROOT).endsWith("." + e)));
        }
        fd.setVisible(true);
        File[] files = fd.getFiles();
        if (files.length > 0) {
            this.lastDir = files[0].getParentFile();
        }
        return files;
    }

    /**
     * Opens the system file dialog for audio files.
     */
    private void chooseFiles() {
        File[] files = this.pick("Add music", false, true,
                "*.wav;*.aif;*.aiff;*.au", AudioTrack.EXTENSIONS);
        if (files.length > 0) {
            this.addFiles(Arrays.asList(files));
        }
    }

    /**
     * Saves the queue as an .m3u playlist.
     */
    private void savePlaylist() {
        File[] picked = this.pick("Save playlist", true, false,
                "playlist.m3u");
        if (picked.length == 0) {
            return;
        }
        File out = picked[0];
        if (!out.getName().toLowerCase(Locale.ROOT).endsWith(".m3u")) {
            out = new File(out.getPath() + ".m3u");
        }
        List<String> lines = new ArrayList<>();
        lines.add("#EXTM3U");
        lines.addAll(this.player.songs());
        try {
            Files.write(out.toPath(), lines, StandardCharsets.UTF_8);
            this.flash("Saved " + out.getName());
        } catch (IOException e) {
            this.flash("Couldn't save: " + e.getMessage());
        }
    }

    /**
     * Loads an .m3u playlist, adding its tracks to the queue.
     */
    private void openPlaylist() {
        File[] picked = this.pick("Open playlist", false, false,
                "*.m3u;*.m3u8", "m3u", "m3u8");
        if (picked.length == 0) {
            return;
        }
        File m3u = picked[0];
        try {
            List<File> files = new ArrayList<>();
            for (String line : Files.readAllLines(m3u.toPath(),
                    StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("#")) {
                    File f = new File(t);
                    files.add(f.isAbsolute() ? f
                            : new File(m3u.getParentFile(), t));
                }
            }
            this.addFiles(files);
        } catch (IOException e) {
            this.flash("Couldn't open: " + e.getMessage());
        }
    }

    /**
     * Shows a short message in the subtitle line until the next refresh.
     *
     * @param msg
     *            the message
     */
    private void flash(String msg) {
        this.subtitle.setText(msg);
        this.subtitle.setForeground(this.accent);
    }

    // ============================================================= display

    /**
     * Syncs every label and the queue with the model.
     */
    private void refresh() {
        List<String> songs = this.player.songs();
        this.queueModel.clear();
        for (String s : songs) {
            this.queueModel.addElement(s);
        }
        int n = songs.size();
        this.queueCount.setText(n == 0 ? "empty"
                : n + (n == 1 ? " track" : " tracks"));

        String current = this.player.getTrack();
        boolean failed = current != null && this.track == null
                && current.equals(this.loadedPath);
        if (current == null) {
            this.title.setText("Nothing on the turntable");
            this.subtitle.setText("Drop WAV, AIFF or AU files here,"
                    + " or press Space to browse");
            this.subtitle.setForeground(MUTED);
        } else {
            String[] meta = meta(current);
            this.title.setText(meta[0]);
            if (!failed) {
                this.subtitle.setText(meta[1].toUpperCase(Locale.ROOT));
                this.subtitle.setForeground(MUTED);
            }
        }
        this.playButton.icon = this.isPlaying() ? Icon.PAUSE : Icon.PLAY;
        this.repeatButton.icon = this.repeat == Repeat.ONE ? Icon.REPEAT_ONE
                : Icon.REPEAT;
        this.repeatButton.active = this.repeat != Repeat.OFF;
        this.repeatButton.setToolTipText("Repeat: "
                + this.repeat.name().toLowerCase(Locale.ROOT));
        this.repaint();
    }

    /**
     * One animation frame.
     */
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - this.lastFrame) / 1e9);
        this.lastFrame = now;
        boolean playing = this.isPlaying();
        if (playing) {
            this.angle = (this.angle + DEG_PER_SEC * dt) % 360;
        }
        double target = this.track != null && (playing || this.progress() > 0)
                ? 1 : 0;
        this.arm += (target - this.arm) * Math.min(1, dt * 4);
        this.accent = mix(this.accent, this.targetAccent(), Math.min(1,
                dt * 3));
        Icon want = playing ? Icon.PAUSE : Icon.PLAY;
        if (this.playButton.icon != want) {
            this.playButton.icon = want;
        }
        if (this.track != null) {
            this.elapsed.setText(clock(this.progress() * this.track.seconds()));
            this.total.setText(clock(this.track.seconds()));
        } else {
            this.elapsed.setText("0:00");
            this.total.setText("0:00");
        }
        this.getContentPane().repaint();
    }

    /**
     * @return playback progress 0..1 (0 if nothing loaded)
     */
    private double progress() {
        return this.track == null ? 0 : this.track.progress();
    }

    /**
     * @return the accent color for the current track
     */
    private Color targetAccent() {
        String song = this.player.getTrack();
        if (song == null) {
            return IDLE_ACCENT;
        }
        return accentFor(song);
    }

    /**
     * Derives a warm-to-cool accent from a song's name.
     *
     * @param song
     *            the song path
     * @return the accent
     */
    private static Color accentFor(String song) {
        float hue = (meta(song)[0].hashCode() & 0xFFFF) / 65536f;
        return Color.getHSBColor(hue, 0.5f, 0.98f);
    }

    /**
     * Splits a file name like "Artist - Title.wav" into title and artist.
     *
     * @param path
     *            file path
     * @return {title, artist-or-folder}
     */
    static String[] meta(String path) {
        File f = new File(path);
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        name = name.replace('_', ' ').trim();
        int dash = name.indexOf(" - ");
        if (dash > 0) {
            return new String[] { name.substring(dash + 3).trim(),
                name.substring(0, dash).trim() };
        }
        File parent = f.getParentFile();
        return new String[] { name,
            parent == null ? "Unknown artist" : parent.getName() };
    }

    /**
     * @param seconds
     *            a duration
     * @return "m:ss"
     */
    static String clock(double seconds) {
        int s = (int) Math.max(0, seconds);
        return String.format("%d:%02d", s / MINUTE, s % MINUTE);
    }

    /**
     * @param path
     *            a song
     * @return its cached "m:ss" length, or "" if unknown
     */
    private String duration(String path) {
        return this.durations.computeIfAbsent(path, p -> {
            double d = AudioTrack.durationOf(new File(p));
            return d < 0 ? "" : clock(d);
        });
    }

    // ============================================================= helpers

    /**
     * @param names
     *            font families in order of preference
     * @return the first installed one
     */
    private static String pickFont(String... names) {
        List<String> have = Arrays.asList(GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String n : names) {
            if (have.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }

    /**
     * @param text
     *            text
     * @param family
     *            font family
     * @param style
     *            font style
     * @param size
     *            font size
     * @param color
     *            text color
     * @return a styled label
     */
    private static JLabel label(String text, String family, int style,
            int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(family, style, size));
        l.setForeground(color);
        return l;
    }

    /**
     * @param layout
     *            layout manager (may be null)
     * @return a transparent panel
     */
    private static JPanel clear(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /**
     * @param r
     *            code to run
     * @return a Swing action running it
     */
    private static AbstractAction action(Runnable r) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }

    /**
     * @param a
     *            from
     * @param b
     *            to
     * @param t
     *            0..1
     * @return the blend
     */
    private static Color mix(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(
                        a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(
                        a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /**
     * @param c
     *            a color
     * @param alpha
     *            0..255
     * @return the color with that alpha
     */
    private static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /**
     * @param g0
     *            graphics
     * @return an antialiased copy
     */
    private static Graphics2D smooth(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    // ========================================================== components

    /**
     * Window background: dark gradient with a glow in the track's color.
     */
    private final class Backdrop extends JPanel {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int w = this.getWidth();
            int h = this.getHeight();
            g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, h, BG_BOTTOM));
            g.fillRect(0, 0, w, h);
            float r = Math.max(w, h) * 0.55f;
            g.setPaint(new RadialGradientPaint(w * 0.3f, h * 0.38f, r,
                    new float[] { 0f, 1f },
                    new Color[] { alpha(MusicPlayerUI.this.accent, 55),
                        alpha(MusicPlayerUI.this.accent, 0) }));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    /**
     * The spinning record and its tonearm.
     */
    private final class Turntable extends JComponent {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;

        /** Constructor. */
        Turntable() {
            this.setPreferredSize(new Dimension(460, 380));
            this.setMinimumSize(new Dimension(200, 180));
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.setToolTipText("Click to play / pause");
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    MusicPlayerUI.this.togglePlay();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            MusicPlayerUI ui = MusicPlayerUI.this;
            double r = Math.min(this.getWidth() * 0.4, this.getHeight() * 0.47);
            double cx = this.getWidth() / 2.0 - r * 0.12;
            double cy = this.getHeight() / 2.0;

            // shadow
            g.setPaint(new RadialGradientPaint((float) cx, (float) (cy + r
                    * 0.06), (float) (r * 1.12), new float[] { 0.8f, 1f },
                    new Color[] { new Color(0, 0, 0, 150),
                        new Color(0, 0, 0, 0) }));
            g.fill(circle(cx, cy + r * 0.06, r * 1.12));

            // disc
            g.setPaint(new RadialGradientPaint((float) cx, (float) cy,
                    (float) r, new float[] { 0f, 1f },
                    new Color[] { new Color(0x1E1B22), new Color(0x0B0A0D) }));
            g.fill(circle(cx, cy, r));

            // grooves
            g.setStroke(new BasicStroke(1f));
            for (double k = 0.37; k < 0.97; k += 0.018) {
                int a = (int) (10 + 10 * Math.abs(Math.sin(k * 57)));
                g.setColor(new Color(255, 255, 255, a));
                g.draw(circle(cx, cy, r * k));
            }

            // fixed light reflections
            for (int start : new int[] { 28, 208 }) {
                g.setPaint(new RadialGradientPaint((float) cx, (float) cy,
                        (float) r, new float[] { 0.3f, 0.7f, 1f },
                        new Color[] { new Color(255, 255, 255, 0),
                            new Color(255, 255, 255, 34),
                            new Color(255, 255, 255, 0) }));
                g.fill(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, start,
                        34, Arc2D.PIE));
            }

            // label (rotates)
            Graphics2D lg = (Graphics2D) g.create();
            lg.rotate(Math.toRadians(ui.angle), cx, cy);
            double lr = r * 0.33;
            Color acc = ui.accent;
            lg.setPaint(new LinearGradientPaint((float) (cx - lr),
                    (float) (cy - lr), (float) (cx + lr), (float) (cy + lr),
                    new float[] { 0f, 1f },
                    new Color[] { acc, mix(acc, new Color(0x2A1030), 0.55) }));
            lg.fill(circle(cx, cy, lr));
            lg.setColor(new Color(0, 0, 0, 60));
            lg.draw(circle(cx, cy, lr * 0.82));
            String song = ui.player.getTrack();
            String word = song == null ? "NOCTURNE"
                    : meta(song)[0].toUpperCase(Locale.ROOT);
            this.drawArcText(lg, word, cx, cy, lr * 0.62);
            lg.setColor(new Color(20, 12, 24, 170));
            lg.setFont(new Font(SANS, Font.BOLD, (int) Math.max(8, lr * 0.12)));
            String side = "SIDE A · 33⅓";
            FontMetrics fm = lg.getFontMetrics();
            lg.drawString(side, (float) (cx - fm.stringWidth(side) / 2.0),
                    (float) (cy + lr * 0.55));
            lg.dispose();

            // spindle
            g.setColor(new Color(0xD8D2CC));
            g.fill(circle(cx, cy, r * 0.022));

            this.paintArm(g, cx, cy, r, ui.arm);
            g.dispose();
        }

        /**
         * Draws text along the top of a circle.
         *
         * @param g
         *            graphics
         * @param text
         *            the text
         * @param cx
         *            center x
         * @param cy
         *            center y
         * @param radius
         *            text radius
         */
        private void drawArcText(Graphics2D g, String text, double cx,
                double cy, double radius) {
            String t = text.length() > 22 ? text.substring(0, 21) + "…"
                    : text;
            g.setFont(new Font(SERIF, Font.BOLD,
                    (int) Math.max(9, radius * 0.24)));
            g.setColor(new Color(20, 12, 24, 210));
            FontMetrics fm = g.getFontMetrics();
            double total = 0;
            for (char c : t.toCharArray()) {
                total += fm.charWidth(c) + 1.5;
            }
            double sweep = Math.min(Math.PI * 1.6, total / radius);
            double a = -Math.PI / 2 - sweep / 2;
            double scale = sweep * radius / total;
            for (char c : t.toCharArray()) {
                double w = (fm.charWidth(c) + 1.5) * scale;
                double mid = a + w / 2 / radius;
                Graphics2D cg = (Graphics2D) g.create();
                cg.translate(cx + Math.cos(mid) * radius,
                        cy + Math.sin(mid) * radius);
                cg.rotate(mid + Math.PI / 2);
                cg.drawString(String.valueOf(c), -fm.charWidth(c) / 2f, 0);
                cg.dispose();
                a += w / radius;
            }
        }

        /**
         * Draws the tonearm.
         *
         * @param g
         *            graphics
         * @param cx
         *            record center x
         * @param cy
         *            record center y
         * @param r
         *            record radius
         * @param t
         *            0 parked .. 1 playing
         */
        private void paintArm(Graphics2D g, double cx, double cy, double r,
                double t) {
            double px = cx + r * 1.08;
            double py = cy - r * 0.78;
            double len = r * 1.32;
            double rest = Math.toRadians(95);
            double play = Math.toRadians(122);
            double ang = rest + (play - rest) * t;
            double ex = px + Math.cos(ang) * len;
            double ey = py + Math.sin(ang) * len;

            // base
            g.setPaint(new RadialGradientPaint((float) px, (float) py,
                    (float) (r * 0.13), new float[] { 0f, 1f },
                    new Color[] { new Color(0x55505C), new Color(0x1E1B22) }));
            g.fill(circle(px, py, r * 0.13));
            g.setColor(LINE);
            g.draw(circle(px, py, r * 0.13));

            // arm
            float wArm = (float) Math.max(3, r * 0.028);
            g.setStroke(new BasicStroke(wArm + 2, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 90));
            g.draw(new java.awt.geom.Line2D.Double(px + 3, py + 4, ex + 3,
                    ey + 4));
            g.setStroke(new BasicStroke(wArm, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.setPaint(new GradientPaint((float) px, (float) py,
                    new Color(0xE9E4DE), (float) ex, (float) ey,
                    new Color(0x9C96A0)));
            g.draw(new java.awt.geom.Line2D.Double(px, py, ex, ey));

            // headshell
            Graphics2D hg = (Graphics2D) g.create();
            hg.translate(ex, ey);
            hg.rotate(ang + Math.toRadians(20));
            double hw = r * 0.16;
            double hh = r * 0.075;
            hg.setColor(new Color(0xCFC8C2));
            hg.fill(new RoundRectangle2D.Double(-hw * 0.2, -hh / 2, hw, hh,
                    hh * 0.6, hh * 0.6));
            hg.setColor(MusicPlayerUI.this.accent);
            hg.fill(new RoundRectangle2D.Double(hw * 0.5, -hh / 2, hw * 0.25,
                    hh, hh * 0.3, hh * 0.3));
            hg.dispose();

            // pivot cap
            g.setColor(new Color(0xD8D2CC));
            g.fill(circle(px, py, r * 0.045));
        }
    }

    /**
     * @param cx
     *            center x
     * @param cy
     *            center y
     * @param r
     *            radius
     * @return a circle shape
     */
    private static Ellipse2D circle(double cx, double cy, double r) {
        return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
    }

    /**
     * Mirrored waveform that doubles as the seek bar.
     */
    private final class Waveform extends JComponent {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;
        /** Hover x, or -1. */
        private int hoverX = -1;

        /** Constructor. */
        Waveform() {
            this.setPreferredSize(new Dimension(420, 56));
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Waveform.this.seekTo(e.getX());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    Waveform.this.hoverX = e.getX();
                    Waveform.this.seekTo(e.getX());
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Waveform.this.hoverX = e.getX();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Waveform.this.hoverX = -1;
                }
            };
            this.addMouseListener(m);
            this.addMouseMotionListener(m);
        }

        /**
         * @param x
         *            pixel to seek to
         */
        private void seekTo(int x) {
            AudioTrack t = MusicPlayerUI.this.track;
            if (t != null) {
                t.seek((double) x / Math.max(1, this.getWidth()));
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            AudioTrack t = MusicPlayerUI.this.track;
            int w = this.getWidth();
            int h = this.getHeight();
            int mid = h / 2;
            int n = AudioTrack.PEAKS;
            double step = (double) w / n;
            double barW = Math.max(1.5, step * 0.55);
            double played = MusicPlayerUI.this.progress() * w;
            Color acc = MusicPlayerUI.this.accent;
            for (int i = 0; i < n; i++) {
                double x = i * step + (step - barW) / 2;
                double p = t == null ? 0.04 : Math.max(0.04, t.peaks()[i]);
                double bh = p * (h - 6) / 2;
                boolean done = x + barW / 2 <= played;
                boolean hover = this.hoverX >= 0 && x <= this.hoverX;
                g.setColor(done ? acc
                        : hover ? new Color(255, 255, 255, 110)
                                : new Color(255, 255, 255, 55));
                g.fill(new RoundRectangle2D.Double(x, mid - bh, barW, bh * 2,
                        barW, barW));
            }
            if (t != null) {
                g.setColor(INK);
                g.fill(new RoundRectangle2D.Double(played - 1, 2, 2, h - 4, 2,
                        2));
            }
            if (t != null && this.hoverX >= 0) {
                String s = clock(this.hoverX / (double) w * t.seconds());
                g.setFont(new Font(SANS, Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(s) + 10;
                int tx = Math.max(0, Math.min(w - tw, this.hoverX - tw / 2));
                g.setColor(new Color(0, 0, 0, 170));
                g.fillRoundRect(tx, 0, tw, fm.getHeight(), 8, 8);
                g.setColor(INK);
                g.drawString(s, tx + 5, fm.getAscent());
            }
            g.dispose();
        }
    }

    /**
     * Minimal horizontal slider for volume.
     */
    private final class Slider extends JComponent {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;

        /** Constructor. */
        Slider() {
            this.setPreferredSize(new Dimension(160, 22));
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Slider.this.set(e.getX());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    Slider.this.set(e.getX());
                }
            };
            this.addMouseListener(m);
            this.addMouseMotionListener(m);
            this.addMouseWheelListener(e -> MusicPlayerUI.this.setVolume(
                    MusicPlayerUI.this.volume
                            - e.getWheelRotation() * VOLUME_STEP));
        }

        /**
         * @param x
         *            pixel the user pointed at
         */
        private void set(int x) {
            int pad = this.getHeight() / 2;
            MusicPlayerUI.this.setVolume(
                    (double) (x - pad)
                            / Math.max(1, this.getWidth() - 2 * pad));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            int h = this.getHeight();
            int pad = h / 2;
            int w = this.getWidth() - 2 * pad;
            int y = h / 2 - 2;
            g.setColor(new Color(255, 255, 255, 40));
            g.fillRoundRect(pad, y, w, 4, 4, 4);
            int fx = (int) (w * MusicPlayerUI.this.volume);
            g.setColor(MusicPlayerUI.this.accent);
            g.fillRoundRect(pad, y, fx, 4, 4, 4);
            g.setColor(INK);
            g.fill(circle(pad + fx, h / 2.0, 6));
            g.dispose();
        }
    }

    /** Icons drawn by {@link IconButton}. */
    private enum Icon {
        /** Icons. */
        PLAY, PAUSE, NEXT, PREV, SHUFFLE, REPEAT, REPEAT_ONE, PLUS, OPEN,
        /** Icons. */
        SAVE, VOLUME
    }

    /**
     * Round, custom-painted icon button.
     */
    private final class IconButton extends JButton {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;
        /** Icon to draw. */
        private Icon icon;
        /** Filled accent disc (primary button). */
        private final boolean primary;
        /** Highlight as "on" (toggle buttons). */
        private boolean active;
        /** Mouse over. */
        private boolean hover;

        /**
         * @param icon
         *            the icon
         * @param size
         *            diameter in pixels
         * @param primary
         *            whether it is the main (filled) button
         */
        IconButton(Icon icon, int size, boolean primary) {
            this.icon = icon;
            this.primary = primary;
            this.setPreferredSize(new Dimension(size, size));
            this.setContentAreaFilled(false);
            this.setBorderPainted(false);
            this.setFocusPainted(false);
            this.setFocusable(false);
            this.setOpaque(false);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    IconButton.this.hover = true;
                    IconButton.this.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    IconButton.this.hover = false;
                    IconButton.this.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            double s = Math.min(this.getWidth(), this.getHeight());
            double c = s / 2;
            Color acc = MusicPlayerUI.this.accent;
            Color ink;
            if (this.primary) {
                double grow = this.getModel().isPressed() ? 0.94 : 1;
                g.setColor(alpha(acc, 60));
                g.fill(circle(c, c, c));
                g.setColor(this.hover ? mix(acc, Color.WHITE, 0.2) : acc);
                g.fill(circle(c, c, c * 0.84 * grow));
                ink = new Color(0x1A1022);
            } else {
                if (this.hover && this.isEnabled()) {
                    g.setColor(new Color(255, 255, 255, 22));
                    g.fill(circle(c, c, c));
                }
                ink = this.active ? acc
                        : this.isEnabled() && this.hover ? INK
                                : new Color(0xC9C2D2);
                if (!this.isEnabled()) {
                    ink = MUTED;
                }
            }
            g.setColor(ink);
            g.translate(c, c);
            double u = s * (this.primary ? 0.16 : 0.22); // icon unit
            g.setStroke(new BasicStroke((float) Math.max(1.6, u * 0.22),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            this.drawIcon(g, u);
            if (this.active) {
                g.fill(circle(0, u * 1.9, 2));
            }
            g.dispose();
        }

        /**
         * Draws the icon centered at the origin.
         *
         * @param g
         *            graphics, translated to the center
         * @param u
         *            size unit
         */
        private void drawIcon(Graphics2D g, double u) {
            Path2D p = new Path2D.Double();
            switch (this.icon) {
                case PLAY:
                    p.moveTo(-u * 0.7, -u);
                    p.lineTo(u * 1.05, 0);
                    p.lineTo(-u * 0.7, u);
                    p.closePath();
                    g.fill(p);
                    break;
                case PAUSE:
                    g.fill(new RoundRectangle2D.Double(-u * 0.8, -u, u * 0.55,
                            u * 2, u * 0.2, u * 0.2));
                    g.fill(new RoundRectangle2D.Double(u * 0.25, -u, u * 0.55,
                            u * 2, u * 0.2, u * 0.2));
                    break;
                case NEXT:
                case PREV:
                    double d = this.icon == Icon.NEXT ? 1 : -1;
                    p.moveTo(-u * 0.8 * d, -u * 0.8);
                    p.lineTo(u * 0.5 * d, 0);
                    p.lineTo(-u * 0.8 * d, u * 0.8);
                    p.closePath();
                    g.fill(p);
                    g.fill(new RoundRectangle2D.Double(
                            d > 0 ? u * 0.55 : -u * 0.8, -u * 0.8, u * 0.25,
                            u * 1.6, u * 0.2, u * 0.2));
                    break;
                case SHUFFLE:
                    p.moveTo(-u, -u * 0.6);
                    p.curveTo(0, -u * 0.6, 0, u * 0.6, u, u * 0.6);
                    p.moveTo(-u, u * 0.6);
                    p.curveTo(0, u * 0.6, 0, -u * 0.6, u, -u * 0.6);
                    p.moveTo(u * 0.65, -u * 0.95);
                    p.lineTo(u, -u * 0.6);
                    p.lineTo(u * 0.65, -u * 0.25);
                    p.moveTo(u * 0.65, u * 0.25);
                    p.lineTo(u, u * 0.6);
                    p.lineTo(u * 0.65, u * 0.95);
                    g.draw(p);
                    break;
                case REPEAT:
                case REPEAT_ONE:
                    g.draw(new RoundRectangle2D.Double(-u, -u * 0.6, u * 2,
                            u * 1.2, u * 0.8, u * 0.8));
                    p.moveTo(u * 0.1, -u * 0.95);
                    p.lineTo(u * 0.45, -u * 0.6);
                    p.lineTo(u * 0.1, -u * 0.25);
                    g.draw(p);
                    if (this.icon == Icon.REPEAT_ONE) {
                        g.setFont(new Font(SANS, Font.BOLD, (int) (u * 1.0)));
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString("1", -fm.stringWidth("1") / 2f,
                                (float) (u * 0.35));
                    }
                    break;
                case PLUS:
                    g.draw(new java.awt.geom.Line2D.Double(-u * 0.7, 0,
                            u * 0.7, 0));
                    g.draw(new java.awt.geom.Line2D.Double(0, -u * 0.7, 0,
                            u * 0.7));
                    break;
                case OPEN:
                    p.moveTo(-u, -u * 0.6);
                    p.lineTo(-u * 0.3, -u * 0.6);
                    p.lineTo(-u * 0.1, -u * 0.35);
                    p.lineTo(u, -u * 0.35);
                    p.lineTo(u, u * 0.7);
                    p.lineTo(-u, u * 0.7);
                    p.closePath();
                    g.draw(p);
                    break;
                case SAVE:
                    p.moveTo(0, -u * 0.9);
                    p.lineTo(0, u * 0.3);
                    p.moveTo(-u * 0.45, -u * 0.1);
                    p.lineTo(0, u * 0.35);
                    p.lineTo(u * 0.45, -u * 0.1);
                    p.moveTo(-u * 0.9, u * 0.5);
                    p.lineTo(-u * 0.9, u * 0.85);
                    p.lineTo(u * 0.9, u * 0.85);
                    p.lineTo(u * 0.9, u * 0.5);
                    g.draw(p);
                    break;
                default: // VOLUME
                    p.moveTo(-u, -u * 0.35);
                    p.lineTo(-u * 0.5, -u * 0.35);
                    p.lineTo(0, -u * 0.8);
                    p.lineTo(0, u * 0.8);
                    p.lineTo(-u * 0.5, u * 0.35);
                    p.lineTo(-u, u * 0.35);
                    p.closePath();
                    g.fill(p);
                    g.draw(new Arc2D.Double(-u * 0.5, -u * 0.6, u * 1.2,
                            u * 1.2, -50, 100, Arc2D.OPEN));
                    break;
            }
        }
    }

    /**
     * Queue row renderer.
     */
    private final class Row extends JComponent
            implements ListCellRenderer<String> {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;
        /** Row path. */
        private String path;
        /** Row index. */
        private int index;
        /** Selected. */
        private boolean selected;

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list, String value, int idx,
                boolean isSelected, boolean cellHasFocus) {
            this.path = value;
            this.index = idx;
            this.selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = smooth(g0);
            MusicPlayerUI ui = MusicPlayerUI.this;
            int w = this.getWidth();
            int h = this.getHeight();
            boolean current = this.index == 0;
            Color acc = ui.accent;
            if (current) {
                g.setColor(alpha(acc, 38));
                g.fillRoundRect(0, 3, w, h - 6, 14, 14);
            } else if (this.selected) {
                g.setColor(new Color(255, 255, 255, 18));
                g.fillRoundRect(0, 3, w, h - 6, 14, 14);
            }

            // index or equalizer
            int lead = 34;
            if (current && ui.isPlaying()) {
                double t = System.nanoTime() / 1e9;
                g.setColor(acc);
                for (int b = 0; b < 3; b++) {
                    double v = 0.35 + 0.65 * Math.abs(Math.sin(t * (3 + b * 1.7)
                            + b));
                    int bh = (int) (16 * v);
                    g.fillRoundRect(10 + b * 5, h / 2 + 8 - bh, 3, bh, 2, 2);
                }
            } else {
                g.setFont(new Font(SANS, Font.PLAIN, 12));
                g.setColor(current ? acc : MUTED);
                String n = current ? "●" : String.valueOf(this.index);
                g.drawString(n, 12, h / 2 + 5);
            }

            String[] m = meta(this.path);
            String dur = ui.duration(this.path);
            g.setFont(new Font(SANS, Font.PLAIN, 12));
            FontMetrics small = g.getFontMetrics();
            int durW = small.stringWidth(dur);
            int textW = w - lead - durW - 22;

            g.setFont(new Font(SANS, Font.BOLD, 14));
            g.setColor(current ? INK : new Color(0xDAD3E2));
            g.drawString(ellipsize(m[0], g.getFontMetrics(), textW), lead,
                    h / 2 - 2);
            g.setFont(new Font(SANS, Font.PLAIN, 12));
            g.setColor(MUTED);
            g.drawString(ellipsize(m[1], small, textW), lead, h / 2 + 14);
            g.drawString(dur, w - durW - 10, h / 2 + 5);

            g.dispose();
        }
    }

    /**
     * Shortens text with an ellipsis to fit a width.
     *
     * @param s
     *            the text
     * @param fm
     *            font metrics
     * @param max
     *            max width in pixels
     * @return the text, shortened if needed
     */
    private static String ellipsize(String s, FontMetrics fm, int max) {
        if (fm.stringWidth(s) <= max) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > max) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    /**
     * Slim, rounded scrollbar thumb with no arrow buttons.
     */
    private static final class ThinScrollBar
            extends javax.swing.plaf.basic.BasicScrollBarUI {

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        /**
         * @return a zero-size button
         */
        private static JButton noButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            Graphics2D g = smooth(g0);
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRoundRect(r.x, r.y, r.width, r.height, r.width, r.width);
            g.dispose();
        }
    }

    /**
     * Accepts dropped files and folders.
     */
    private final class DropHandler extends TransferHandler {
        /** Serialization id. */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean canImport(TransferSupport s) {
            return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport s) {
            try {
                MusicPlayerUI.this.addFiles((List<File>) s.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor));
                return true;
            } catch (java.awt.datatransfer.UnsupportedFlavorException
                    | IOException e) {
                return false;
            }
        }
    }

    /**
     * Launches the player. Any arguments are files or folders to queue.
     *
     * @param args
     *            files or folders
     */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException
                    | javax.swing.UnsupportedLookAndFeelException e) {
                // keep the default look
            }
            MusicPlayerUI ui = new MusicPlayerUI();
            List<File> files = new ArrayList<>();
            for (String a : args) {
                files.add(new File(a));
            }
            if (!files.isEmpty()) {
                ui.addFiles(files);
            }
            ui.setVisible(true);
        });
    }
}
