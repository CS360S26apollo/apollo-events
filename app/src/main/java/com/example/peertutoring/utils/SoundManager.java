package com.example.peertutoring.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SoundManager — plays UI sound effects using AudioTrack.
 *
 * AudioTrack writes PCM samples directly to the audio hardware,
 * so there is no load delay, no file system dependency, and no
 * SoundPool async timing issue.
 *
 * Sounds play on a background thread so they never block the UI.
 *
 * Usage:
 *   SoundManager.playClick(context);
 *   SoundManager.playSuccess(context);
 *   SoundManager.playError(context);
 *   SoundManager.attachClick(context, anyView);
 */
public class SoundManager {

    private static final int SAMPLE_RATE = 44100;
    // Single-thread executor so sounds queue up and don't overlap badly
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Short soft tap — use on every button press */
    public static void playClick(Context context) {
        playTone(new int[]{1200}, new int[]{40}, new float[]{0.3f});
    }

    /** Rising two-note chime — use on success / booking confirmed */
    public static void playSuccess(Context context) {
        playTone(new int[]{880, 1100}, new int[]{120, 180}, new float[]{0.5f, 0.6f});
    }

    /** Low descending buzz — use on errors / insufficient tokens */
    public static void playError(Context context) {
        playTone(new int[]{400, 300}, new int[]{100, 150}, new float[]{0.5f, 0.4f});
    }

    /**
     * Attaches a click sound to a view's touch DOWN event.
     * Does NOT replace or interfere with existing click listeners.
     */
    public static void attachClick(final Context context, final View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                playClick(context);
            }
            return false; // pass through — click listener still fires
        });
    }

    /**
     * Plays a sequence of sine-wave tones back to back.
     * Each tone is defined by its frequency (Hz), duration (ms), and amplitude (0..1).
     * Runs entirely on a background thread.
     */
    private static void playTone(int[] freqs, int[] durationsMs, float[] amplitudes) {
        executor.execute(() -> {
            for (int i = 0; i < freqs.length; i++) {
                playOneTone(freqs[i], durationsMs[i], amplitudes[i]);
            }
        });
    }

    private static void playOneTone(int freqHz, int durationMs, float amplitude) {
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        short[] samples = new short[numSamples];

        // Generate sine wave with fade-in and fade-out envelope
        int fadeLen = Math.min(numSamples / 8, 200);
        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * i * freqHz / SAMPLE_RATE;
            float env = 1.0f;
            if (i < fadeLen) {
                env = (float) i / fadeLen;
            } else if (i > numSamples - fadeLen) {
                env = (float) (numSamples - i) / fadeLen;
            }
            samples[i] = (short) (Math.sin(angle) * amplitude * env * Short.MAX_VALUE);
        }

        // Build AudioTrack
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();

        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioTrack track = null;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(Math.max(bufferSize, numSamples * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();

            track.write(samples, 0, numSamples);
            track.play();

            // Wait for playback to finish before returning
            // (so sequential tones don't overlap)
            Thread.sleep(durationMs + 10);

        } catch (Exception e) {
            // Silently ignore — sound failure should never crash the app
        } finally {
            if (track != null) {
                try {
                    track.stop();
                    track.release();
                } catch (Exception ignored) {}
            }
        }
    }
}