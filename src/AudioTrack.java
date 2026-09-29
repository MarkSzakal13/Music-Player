import java.io.File;
import java.io.IOException;
import java.util.Map;

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
 * Class to represent one song that is loaded and ready to play. The whole
 * song is decoded into memory so it can be played through a Clip, and a
 * waveform is calculated so the UI can draw it.
 *
 * <p>
 * Java can play WAV, AIFF, and AU files by itself. MP3 files work when the
 * mp3spi library is in the lib folder (run.bat downloads it).
 */
public final class AudioTrack {

    /**
     * Number of bars in the waveform.
     */
    public static final int WAVEFORM_BARS = 180;

    /**
     * File extensions this player can open.
     */
    public static final String[] EXTENSIONS = { "mp3", "wav", "aif", "aiff",
        "au" };

    /**
     * Bits in one decoded sample.
     */
    private static final int BITS_PER_SAMPLE = 16;
    /**
     * Bytes in one decoded sample.
     */
    private static final int BYTES_PER_SAMPLE = 2;
    /**
     * Largest possible value of a 16 bit sample.
     */
    private static final float MAX_SAMPLE = 32768f;
    /**
     * Number of bits to shift the high byte of a sample.
     */
    private static final int BYTE_SHIFT = 8;
    /**
     * Mask that turns a signed byte into an unsigned value.
     */
    private static final int BYTE_MASK = 0xFF;
    /**
     * Used to turn a volume into decibels.
     */
    private static final double DECIBELS_PER_DECADE = 20;
    /**
     * Microseconds in a second.
     */
    private static final double MICROSECONDS = 1_000_000;

    /**
     * The clip that plays the song.
     */
    private final Clip clip;
    /**
     * Height of each waveform bar, from 0 to 1.
     */
    private final float[] waveform = new float[WAVEFORM_BARS];
    /**
     * Code to run when the song finishes.
     */
    private Runnable onFinished;

    /**
     * Loads a song from a file.
     *
     * @param file
     *            The song file.
     * @throws IOException
     *             if the file can't be read, isn't a supported format, or
     *             there is no speaker to play it on.
     */
    public AudioTrack(File file) throws IOException {
        AudioFormat format;
        byte[] samples;

        try (AudioInputStream original = AudioSystem.getAudioInputStream(file)) {
            AudioFormat source = original.getFormat();
            int frameSize = source.getChannels() * BYTES_PER_SAMPLE;
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    source.getSampleRate(), BITS_PER_SAMPLE,
                    source.getChannels(), frameSize, source.getSampleRate(),
                    false);

            try (AudioInputStream decoded = AudioSystem
                    .getAudioInputStream(format, original)) {
                samples = decoded.readAllBytes();
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            String message = "unsupported format (use MP3, WAV, AIFF or AU)";
            if (file.getName().toLowerCase().endsWith(".mp3")) {
                message = "MP3 support missing - start the player with run.bat";
            }
            throw new IOException(message, e);
        }

        this.buildWaveform(samples, format.getChannels());

        try {
            this.clip = AudioSystem.getClip();
            this.clip.open(format, samples, 0, samples.length);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            throw new IOException("no audio output device available", e);
        }

        this.clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && this.isAtEnd()
                    && this.onFinished != null) {
                this.onFinished.run();
            }
        });
    }

    /**
     * Fills the waveform with the loudest sample in each slice of the song.
     *
     * @param samples
     *            The decoded song, as 16 bit little endian samples.
     * @param channels
     *            The number of channels (1 for mono, 2 for stereo).
     */
    private void buildWaveform(byte[] samples, int channels) {
        int frameSize = BYTES_PER_SAMPLE * channels;
        int frames = samples.length / frameSize;
        float loudest = 0;

        for (int bar = 0; bar < WAVEFORM_BARS; bar++) {
            int start = (int) ((long) frames * bar / WAVEFORM_BARS);
            int end = (int) ((long) frames * (bar + 1) / WAVEFORM_BARS);
            int peak = 0;

            for (int frame = start; frame < end; frame++) {
                // Only the first channel is checked, which is close enough.
                int i = frame * frameSize;
                int high = samples[i + 1] << BYTE_SHIFT;
                int low = samples[i] & BYTE_MASK;
                int sample = Math.abs((short) (high | low));
                peak = Math.max(peak, sample);
            }

            this.waveform[bar] = peak / MAX_SAMPLE;
            loudest = Math.max(loudest, this.waveform[bar]);
        }

        if (loudest > 0) {
            for (int bar = 0; bar < WAVEFORM_BARS; bar++) {
                this.waveform[bar] = this.waveform[bar] / loudest;
            }
        }
    }

    /**
     * Reads how long a song is without loading the whole file.
     *
     * @param file
     *            The song file.
     * @return the length in seconds, or -1 if it can't be read.
     */
    public static double lengthOf(File file) {
        double seconds = -1;
        try {
            AudioFileFormat format = AudioSystem.getAudioFileFormat(file);
            Map<String, Object> properties = format.properties();

            if (format.getFrameLength() > 0) {
                seconds = format.getFrameLength()
                        / format.getFormat().getFrameRate();
            } else if (properties.get("duration") instanceof Long) {
                // MP3 files store their length as a "duration" property.
                long microseconds = (Long) properties.get("duration");
                seconds = microseconds / MICROSECONDS;
            }
        } catch (UnsupportedAudioFileException | IOException e) {
            seconds = -1;
        }
        return seconds;
    }

    /**
     * Sets the code to run when the song finishes playing.
     *
     * @param onFinished
     *            The code to run. It runs on the audio thread.
     */
    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    /**
     * Checks if the song has played all the way through.
     *
     * @return true if the song is at its end.
     */
    private boolean isAtEnd() {
        return this.clip.getFramePosition() >= this.clip.getFrameLength() - 1;
    }

    /**
     * Plays the song from where it was paused, or from the start if it had
     * finished.
     */
    public void play() {
        if (this.isAtEnd()) {
            this.clip.setFramePosition(0);
        }
        this.clip.start();
    }

    /**
     * Pauses the song.
     */
    public void pause() {
        this.clip.stop();
    }

    /**
     * Checks if the song is playing.
     *
     * @return true if the song is playing.
     */
    public boolean isPlaying() {
        return this.clip.isRunning();
    }

    /**
     * Returns how far into the song playback is.
     *
     * @return the progress from 0 (start) to 1 (end).
     */
    public double progress() {
        int length = Math.max(1, this.clip.getFrameLength());
        return (double) this.clip.getFramePosition() / length;
    }

    /**
     * Jumps to a point in the song.
     *
     * @param progress
     *            Where to jump to, from 0 (start) to 1 (end).
     */
    public void seek(double progress) {
        double clamped = Math.max(0, Math.min(1, progress));
        int frame = (int) (clamped * (this.clip.getFrameLength() - 1));
        this.clip.setFramePosition(frame);
    }

    /**
     * Returns the length of the song.
     *
     * @return the length in seconds.
     */
    public double seconds() {
        return this.clip.getMicrosecondLength() / MICROSECONDS;
    }

    /**
     * Returns the waveform of the song. Each value is the height of one bar,
     * from 0 to 1.
     *
     * @return the waveform.
     */
    public float[] waveform() {
        return this.waveform;
    }

    /**
     * Sets the volume.
     *
     * @param volume
     *            The volume from 0 (silent) to 1 (full).
     */
    public void setVolume(double volume) {
        if (this.clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) this.clip
                    .getControl(FloatControl.Type.MASTER_GAIN);

            float decibels = gain.getMinimum();
            if (volume > 0) {
                decibels = (float) (DECIBELS_PER_DECADE * Math.log10(volume));
            }
            decibels = Math.max(gain.getMinimum(),
                    Math.min(gain.getMaximum(), decibels));
            gain.setValue(decibels);
        }
    }

    /**
     * Stops the song and frees the speaker for the next one.
     */
    public void close() {
        this.onFinished = null;
        this.clip.close();
    }
}
