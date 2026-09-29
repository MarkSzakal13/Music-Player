import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * One loaded audio file: decoded into memory, playable through a
 * {@link Clip}, with a precomputed waveform for drawing.
 *
 * <p>
 * Uses only the JDK's {@code javax.sound}, so it plays WAV, AIFF and AU.
 */
public final class AudioTrack {

    /** Number of waveform bars. */
    public static final int PEAKS = 180;

    /** File extensions the JDK can decode. */
    public static final String[] EXTENSIONS = { "wav", "aif", "aiff", "au" };

    /** Bytes per 16-bit sample. */
    private static final int BYTES = 2;

    /** Largest 16-bit sample value. */
    private static final float MAX = 32768f;

    /** Decibels per factor of ten in amplitude. */
    private static final float DB = 20f;

    /** The playing clip. */
    private final Clip clip;

    /** Normalized peak per waveform bar, 0..1. */
    private final float[] peaks = new float[PEAKS];

    /** Called on the audio thread when playback reaches the end. */
    private Runnable onEnd = () -> { };

    /**
     * Decodes a file and opens a clip for it.
     *
     * @param file
     *            the audio file
     * @throws IOException
     *             if the file cannot be read or decoded
     */
    public AudioTrack(File file) throws IOException {
        AudioFormat pcm;
        byte[] data;
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(file)) {
            AudioFormat src = raw.getFormat();
            pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    src.getSampleRate(), 16, src.getChannels(),
                    src.getChannels() * BYTES, src.getSampleRate(), false);
            try (AudioInputStream in = AudioSystem.getAudioInputStream(pcm,
                    raw)) {
                data = in.readAllBytes();
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            throw new IOException("unsupported format (use WAV, AIFF or AU)",
                    e);
        }
        this.computePeaks(data, pcm.getChannels());
        try {
            this.clip = AudioSystem.getClip();
            this.clip.open(pcm, data, 0, data.length);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            throw new IOException("no audio output device available", e);
        }
        this.clip.addLineListener(e -> {
            if (e.getType() == LineEvent.Type.STOP && this.clip
                    .getFramePosition() >= this.clip.getFrameLength() - 1) {
                this.onEnd.run();
            }
        });
    }

    /**
     * Fills {@link #peaks} with the loudest sample of each slice.
     *
     * @param data
     *            16-bit little-endian PCM
     * @param channels
     *            channel count
     */
    private void computePeaks(byte[] data, int channels) {
        int frames = data.length / (BYTES * channels);
        float loudest = 1e-6f;
        for (int b = 0; b < PEAKS; b++) {
            int from = (int) ((long) frames * b / PEAKS);
            int to = (int) ((long) frames * (b + 1) / PEAKS);
            int max = 0;
            for (int f = from; f < to; f++) {
                int i = f * BYTES * channels; // first channel is enough
                int s = Math.abs((short) ((data[i + 1] << 8)
                        | (data[i] & 0xFF)));
                max = Math.max(max, s);
            }
            this.peaks[b] = max / MAX;
            loudest = Math.max(loudest, this.peaks[b]);
        }
        for (int b = 0; b < PEAKS; b++) {
            this.peaks[b] /= loudest;
        }
    }

    /**
     * Reads a file's length without decoding it.
     *
     * @param file
     *            the audio file
     * @return length in seconds, or -1 if unknown / unsupported
     */
    public static double durationOf(File file) {
        try {
            AudioFileFormat f = AudioSystem.getAudioFileFormat(file);
            if (f.getFrameLength() > 0) {
                return f.getFrameLength() / f.getFormat().getFrameRate();
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            return -1;
        }
        return -1;
    }

    /**
     * @param r
     *            called (on the audio thread) when the track ends
     */
    public void setOnEnd(Runnable r) {
        this.onEnd = r;
    }

    /** Starts or resumes playback. */
    public void play() {
        if (this.clip.getFramePosition() >= this.clip.getFrameLength() - 1) {
            this.clip.setFramePosition(0);
        }
        this.clip.start();
    }

    /** Pauses playback. */
    public void pause() {
        this.clip.stop();
    }

    /**
     * @return whether audio is playing
     */
    public boolean isPlaying() {
        return this.clip.isRunning();
    }

    /**
     * @return playback position, 0..1
     */
    public double progress() {
        return (double) this.clip.getFramePosition()
                / Math.max(1, this.clip.getFrameLength());
    }

    /**
     * @param fraction
     *            position to jump to, 0..1
     */
    public void seek(double fraction) {
        double f = Math.max(0, Math.min(1, fraction));
        this.clip.setFramePosition((int) (f * (this.clip.getFrameLength()
                - 1)));
    }

    /**
     * @return length in seconds
     */
    public double seconds() {
        return this.clip.getMicrosecondLength() / 1e6;
    }

    /**
     * @return normalized waveform peaks (do not modify)
     */
    public float[] peaks() {
        return this.peaks;
    }

    /**
     * @param volume
     *            0..1
     */
    public void setVolume(double volume) {
        if (!this.clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) this.clip
                .getControl(FloatControl.Type.MASTER_GAIN);
        float db = volume <= 0 ? gain.getMinimum()
                : (float) (DB * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(),
                Math.min(gain.getMaximum(), db)));
    }

    /** Stops playback and frees the audio line. */
    public void close() {
        this.onEnd = () -> { };
        this.clip.close();
    }
}
