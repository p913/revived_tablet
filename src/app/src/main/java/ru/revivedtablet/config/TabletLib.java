package ru.revivedtablet.config;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import ru.revivedtablet.R;
import ru.revivedtablet.RevivedTabletApp;

public class TabletLib extends TwoArgFunction {
    private MediaPlayer mediaPlayer;
    private final static int MAX_VOLUME = 100;

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    };

    private void play(int resId, float volume) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.setOnCompletionListener(onCompletionListener);
        } else
            mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(RevivedTabletApp.getContext(),
                    Uri.parse("android.resource://ru.revivedtablet/" + resId));
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        library.set("beep", new BeepMethod(this));
        library.set("longbeep", new LongBeepMethod(this));
        env.set("tablet", library);
        return library;
    }

    static private float percentVolumeToLogScale(LuaValue volume) {
        int percents = MAX_VOLUME - 1;
        if (volume.isnumber()) {
            percents = volume.checkint();
            percents = Math.max(Math.min(percents, MAX_VOLUME - 1), 0);
        }
        return (float)(1 - Math.log(MAX_VOLUME - percents) / Math.log(MAX_VOLUME));
    }

    static class BeepMethod extends OneArgFunction {
        private final TabletLib lib;

        public BeepMethod(TabletLib lib) {
            this.lib = lib;
        }

        @Override
        public LuaValue call(LuaValue volume) {
            lib.play(R.raw.beep, percentVolumeToLogScale(volume));

            return NIL;
        }
    }

    static class LongBeepMethod extends OneArgFunction {
        private final TabletLib lib;

        public LongBeepMethod(TabletLib lib) {
            this.lib = lib;
        }

        @Override
        public LuaValue call(LuaValue volume) {
            lib.play(R.raw.longbeep, percentVolumeToLogScale(volume));

            return NIL;
        }
    }

}
