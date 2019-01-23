package com.shareder.ln_jan.wechatluckymoneygetter.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * Created by Ln_Jan on 2019/1/23.
 * 播放提示音类
 */

public class SoundPlayer implements SoundPool.OnLoadCompleteListener {
    private static final int DEFAULT_INVALID_STREAM_ID = -1;

    private SoundPool mSoundPool;
    private int mStreamId = DEFAULT_INVALID_STREAM_ID;
    private int mPreStreamId = DEFAULT_INVALID_STREAM_ID;

    public SoundPlayer() {
        init();
    }

    private void init() {
        AudioAttributes audioAttributes;
        audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(audioAttributes);
        mSoundPool = builder.build();
        mSoundPool.setOnLoadCompleteListener(this);
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        mStreamId = status == 0x00 ? mPreStreamId : DEFAULT_INVALID_STREAM_ID;
    }

    public void loadMusic(Context context, int resourceId) {
        mPreStreamId = mSoundPool.load(context, resourceId, 1);
    }

    public void playMusic() {
        if (mStreamId != DEFAULT_INVALID_STREAM_ID) {
            mSoundPool.play(mStreamId, 1, 1, 0, 0, 1);
        }
    }

    public void releaseMusic() {
        mSoundPool.autoPause();
        if (mStreamId != DEFAULT_INVALID_STREAM_ID) {
            mSoundPool.unload(mStreamId);
            mStreamId = DEFAULT_INVALID_STREAM_ID;
        }
        mSoundPool.release();
    }
}
