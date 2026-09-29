package com.sec.android.app.soundalive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.Switch;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private short minEQ = -1500;
    private short maxEQ = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        prefs = getSharedPreferences("SoundAlive_Settings", Context.MODE_PRIVATE);

        // Solicitar permiso especial si la app no lo tiene concedido
        checkWriteSettingsPermission();

        // Obtener rango exacto de frecuencias del hardware del teléfono
        try {
            Equalizer tempEq = new Equalizer(0, 0);
            minEQ = tempEq.getBandLevelRange()[0];
            maxEQ = tempEq.getBandLevelRange()[1];
            tempEq.release();
        } catch (Exception ignored) {}

        startAudioService();
        setupControls();
    }

    private void checkWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }

    private void startAudioService() {
        Intent intent = new Intent(this, SoundAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void setupControls() {
        int[] bandIds = {
            R.id.band_60, R.id.band_150, R.id.band_400,
            R.id.band_1k, R.id.band_3k, R.id.band_8k, R.id.band_16k
        };

        int totalRange = maxEQ - minEQ;

        for (int i = 0; i < bandIds.length; i++) {
            SeekBar seekBar = findViewById(bandIds[i]);
            if (seekBar == null) continue;

            int savedMilliBels = prefs.getInt("band_" + i, 0);

            seekBar.setMax(totalRange);
            seekBar.setProgress(savedMilliBels - minEQ);

            final int index = i;
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser) {
                        int milliBels = progress + minEQ;
                        prefs.edit().putInt("band_" + index, milliBels).apply();
                        startAudioService();
                    }
                }

                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        // Switch Surround
        Switch switchSurround = findViewById(R.id.switch_surround);
        if (switchSurround != null) {
            switchSurround.setChecked(prefs.getBoolean("surround_on", false));
            switchSurround.setOnCheckedChangeListener((cb, isChecked) -> {
                prefs.edit().putBoolean("surround_on", isChecked).apply();
                startAudioService();
            });
        }

        // Switch Tube Amp Pro
        Switch switchTube = findViewById(R.id.switch_tube);
        if (switchTube != null) {
            switchTube.setChecked(prefs.getBoolean("tube_on", false));
            switchTube.setOnCheckedChangeListener((cb, isChecked) -> {
                prefs.edit().putBoolean("tube_on", isChecked).apply();
                startAudioService();
            });
        }

        // Switch Concert Hall
        Switch switchConcert = findViewById(R.id.switch_concert);
        if (switchConcert != null) {
            switchConcert.setChecked(prefs.getBoolean("concert_on", false));
            switchConcert.setOnCheckedChangeListener((cb, isChecked) -> {
                prefs.edit().putBoolean("concert_on", isChecked).apply();
                startAudioService();
            });
        }
    }
}
