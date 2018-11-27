package kr.co.woobi.imyeon.musicbox;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;

import java.io.IOException;
import java.util.ArrayList;

public class AudioService extends Service {
    private final IBinder mBinder = new AudioServiceBinder();
    private ArrayList<Long> mAudioIds = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private boolean isPrepared;
    private int mCurrentPosition;
    private AudioAdapter.AudioItem mAudioItem;
    private  NotificationPlayer mNotificationPlayer;

    public class AudioServiceBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationPlayer=new NotificationPlayer(this);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPrepared = true;
                mp.start();
                sendBroadcast(new Intent(BroadcastActions.PREPARED));// prepared 전송
                updateNotificationPlayer();
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPrepared = false;
                sendBroadcast(new Intent(BroadcastActions.PLAY_STATE_CHANGED));// 재생상태 변경 전송
                updateNotificationPlayer();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                isPrepared = false;
                sendBroadcast(new Intent(BroadcastActions.PLAY_STATE_CHANGED));// 재생상태 변경 전송
                updateNotificationPlayer();
                return false;
            }
        });
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void queryAudioItem(int position) {
        mCurrentPosition = position;
        long audioId = mAudioIds.get(position);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media._ID + " = ?";
        String[] selectionArs = {String.valueOf(audioId)};
        Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArs, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                mAudioItem = AudioAdapter.AudioItem.bindCursor(cursor);
            }
            cursor.close();
        }
    }

    private void prepare() {
        try {
            mMediaPlayer.setDataSource(mAudioItem.mDataPath);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
    }

    public void setPlayList(ArrayList<Long> audioIds) {
        if (!mAudioIds.equals(audioIds)) {
            mAudioIds.clear();
            mAudioIds.addAll(audioIds);
        }
    }

    public void play(int position) {
        queryAudioItem(position);
        stop();
        prepare();
    }

    public void play() {
        if (isPrepared) {
            mMediaPlayer.start();
            sendBroadcast(new Intent(BroadcastActions.PLAY_STATE_CHANGED)); // 재생상태 변경 전송
            updateNotificationPlayer();
        }
    }

    public void pause() {
        if (isPrepared) {
            mMediaPlayer.pause();
            sendBroadcast(new Intent(BroadcastActions.PLAY_STATE_CHANGED)); // 재생상태 변경 전송
            updateNotificationPlayer();
        }
    }

    public void forward() {
        if (mAudioIds.size() - 1 > mCurrentPosition) {
            mCurrentPosition++;

        } else {
            mCurrentPosition = 0;
        }
        play(mCurrentPosition);
    }

    public void rewind() {
        if (mCurrentPosition > 0) {
            mCurrentPosition--;

        } else {
            mCurrentPosition = mAudioIds.size() - 1;
        }
        play(mCurrentPosition);
    }

    public AudioAdapter.AudioItem getmAudioItem(){
        return  mAudioItem;
    }

    public  boolean isPlaying(){
        return mMediaPlayer.isPlaying();
    }

    private void updateNotificationPlayer(){
        if(mNotificationPlayer !=null){
            mNotificationPlayer.updateNotificationPlayer();
        }
    }
    private void removeNotificationPlayer(){
        if(mNotificationPlayer !=null){
            mNotificationPlayer.removeNotificationPlayer();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent !=null){
            String action = intent.getAction();
            if(NotificationPlayer.CommandActions.TOGGLE_PLAY.equals(action)){
                if(isPlaying()){
                    pause();
                }else {
                    play();
                }
            }else if(NotificationPlayer.CommandActions.REWIND.equals(action)){
                rewind();
            }else if(NotificationPlayer.CommandActions.FORWARD.equals(action)){
                forward();
            }else if(NotificationPlayer.CommandActions.CLOSE.equals(action)){

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
