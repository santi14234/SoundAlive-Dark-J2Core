package com.sec.android.app.soundalive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

public class SoundAliveService extends Service {

    private final Map<Integer, Equalizer> mEqualizers = new HashMap<>();
    private final Map<Integer, Virtualizer> mVirtualizers = new HashMap<>();
    private final Map<Integer, PresetReverb> mReverbs = new HashMap<>();
    private final Map<Integer, BassBoost> mBassBoosts = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotification();
        attachSession(0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR_BAD_VALUE);

            if (sessionId != AudioEffect.ERROR_BAD_VALUE) {
                if (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
                    attachSession(sessionId);
                } else if (AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
                    detachSession(sessionId);
                }
            }
        }

        applyAllSettings();
        return START_STICKY;
    }

    private synchronized void attachSession(int sessionId) {
        try {
            if (!mEqualizers.containsKey(sessionId)) {
                Equalizer eq = new Equalizer(0, sessionId);
                eq.setEnabled(true);
                mEqualizers.put(sessionId, eq);
            }
            if (!mVirtualizers.containsKey(sessionId)) {
                Virtualizer virt = new Virtualizer(0, sessionId);
                virt.setEnabled(true);
                mVirtualizers.put(sessionId, virt);
            }
            if (!mReverbs.containsKey(sessionId)) {
                PresetReverb reverb = new PresetReverb(0, sessionId);
                reverb.setEnabled(true);
                mReverbs.put(sessionId, reverb);
            }
            if (!mBassBoosts.containsKey(sessionId)) {
                BassBoost bass = new BassBoost(0, sessionId);
                bass.setEnabled(true);
                mBassBoosts.put(sessionId, bass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void applyAllSettings() {
        SharedPreferences prefs = getSharedPreferences("SoundAlive_Settings", Context.MODE_PRIVATE);

        boolean isSurround = prefs.getBoolean("surround_on", false);
        boolean isConcert = prefs.getBoolean("concert_on", false);
        boolean isTube = prefs.getBoolean("tube_on", false);

        for (Equalizer eq : mEqualizers.values()) {
            if (eq == null) continue;
            try {
                short numBands = eq.getNumberOfBands();
                short minEQ = eq.getBandLevelRange()[0];
                short maxEQ = eq.getBandLevelRange()[1];

                for (short i = 0; i < 7 && i < numBands; i++) {
                    int milliBels = prefs.getInt("band_" + i, 0);

                    if (isTube) {
                        if (i <= 1) milliBels = Math.min(milliBels + 450, maxEQ);
                        if (i >= 5) milliBels = Math.max(milliBels - 350, minEQ);
                    }

                    eq.setBandLevel(i, (short) milliBels);
                }
            } catch (Exception ignored) {}
        }

        for (Virtualizer virt : mVirtualizers.values()) {
            if (virt == null) continue;
            try {
                if (virt.getStrengthSupported()) {
                    virt.setStrength((short) (isSurround ? 1000 : 0));
                }
                virt.setEnabled(isSurround);
            } catch (Exception ignored) {}
        }

        for (PresetReverb reverb : mReverbs.values()) {
            if (reverb == null) continue;
            try {
                reverb.setPreset(isConcert ? PresetReverb.PRESET_LARGEHALL : PresetReverb.PRESET_NONE);
                reverb.setEnabled(isConcert);
            } catch (Exception ignored) {}
        }

        for (BassBoost bass : mBassBoosts.values()) {
            if (bass == null) continue;
            try {
                if (bass.getStrengthSupported()) {
                    bass.setStrength((short) (isTube ? 500 : 0));
                }
                bass.setEnabled(isTube);
            } catch (Exception ignored) {}
        }
    }

    private synchronized void detachSession(int sessionId) {
        if (mEqualizers.containsKey(sessionId)) {
            mEqualizers.get(sessionId).release();
            mEqualizers.remove(sessionId);
        }
        if (mVirtualizers.containsKey(sessionId)) {
            mVirtualizers.get(sessionId).release();
            mVirtualizers.remove(sessionId);
        }
        if (mReverbs.containsKey(sessionId)) {
            mReverbs.get(sessionId).release();
            mReverbs.remove(sessionId);
        }
        if (mBassBoosts.containsKey(sessionId)) {
            mBassBoosts.get(sessionId).release();
            mBassBoosts.remove(sessionId);
        }
    }

    private void startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "soundalive_dark_channel",
                    "SoundAlive Dark Engine",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, "soundalive_dark_channel")
                    .setContentTitle("SoundAlive")
                    .setContentText("Procesamiento de sonido activo")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build();

            startForeground(101, notification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
