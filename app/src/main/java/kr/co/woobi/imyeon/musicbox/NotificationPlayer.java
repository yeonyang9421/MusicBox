package kr.co.woobi.imyeon.musicbox;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

public class NotificationPlayer {
    public static final int NOTIFICATION_PLAYER_ID = 1000;
    private  AudioService mService;
    private NotificationManager mNotificationManager;
    private  NotificationManagerBuilder mNotificationManagerBuilder;
    private  boolean isForeground;

    public  NotificationPlayer(AudioService service){
        mService =service;
        mNotificationManager=(NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
      }

      public  void updateNotificationPlayer(){
        cancel();
        mNotificationManagerBuilder=new NotificationManagerBuilder();
        mNotificationManagerBuilder.execute();
      }
      public void  removeNotificationPlayer(){
        cancel();
        mService.stopForeground(true);
        isForeground=false;
      }

    private void cancel() {
        if(mNotificationManagerBuilder !=null){
            mNotificationManagerBuilder.cancel(true);
            mNotificationManagerBuilder=null;
        }
    }

    private class NotificationManagerBuilder extends AsyncTask<Void, Void, Notification> {
        private RemoteViews mRemoteViews;
        private NotificationCompat.Builder mNotificationBuilder;
        private PendingIntent mMainPendingIntent;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Intent mainActitity= new Intent(mService, MainActivity.class);
            mMainPendingIntent=PendingIntent.getActivity(mService, 0, mainActitity,0);
            mRemoteViews=createRemoteView(R.layout.notification_player);
            mNotificationBuilder=new NotificationCompat.Builder(mService);
            mNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setContentIntent(mMainPendingIntent)
                    .setContent(mRemoteViews);

            Notification notification=mNotificationBuilder.build();
            notification.priority=Notification.PRIORITY_MAX;
            notification.contentIntent=mMainPendingIntent;
            if(!isForeground){
                isForeground=true;
                mService.startForeground(NOTIFICATION_PLAYER_ID, notification);
            }
        }

        private RemoteViews createRemoteView(int layoutId) {
            RemoteViews remoteViews=new RemoteViews(mService.getPackageName(), layoutId);
            Intent actionTogglePlay= new Intent(CommandActions.TOGGLE_PLAY);
            Intent actionForward= new Intent(CommandActions.FORWARD);
            Intent actionRewind= new Intent(CommandActions.REWIND);
            Intent actionClose= new Intent(CommandActions.CLOSE);
            PendingIntent togglePlay= PendingIntent.getService(mService,0,actionTogglePlay,0);
            PendingIntent forward= PendingIntent.getService(mService,0,actionForward,0);
            PendingIntent rewind= PendingIntent.getService(mService,0,actionRewind,0);
            PendingIntent close= PendingIntent.getService(mService,0,actionClose,0);

            remoteViews.setOnClickPendingIntent(R.id.btn_play_pause, togglePlay);
            remoteViews.setOnClickPendingIntent(R.id.btn_forward, forward);
            remoteViews.setOnClickPendingIntent(R.id.btn_rewind, rewind);
            remoteViews.setOnClickPendingIntent(R.id.btn_close, close);
            return  remoteViews;
        }

        @Override
        protected Notification doInBackground(Void... voids) {
            mNotificationBuilder.setContent(mRemoteViews);
            mNotificationBuilder.setContentIntent(mMainPendingIntent);
            mNotificationBuilder.setPriority(Notification.PRIORITY_MAX);
            Notification notification=mNotificationBuilder.build();
            updateRemoteView(mRemoteViews,notification);
            return null;
        }
    }
    public  class  CommandActions{
        public final static String REWIND="REWIND";
        public final static String TOGGLE_PLAY = "TOGGLE_PLAY";
        public final static String FORWARD = "FORWARD";
        public final static String CLOSE = "CLOSE";
    }

    private  void updateRemoteView(RemoteViews remoteViews, Notification notification){
        if(mService.isPlaying()){
            remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.pause);
        }else{
            remoteViews.setImageViewResource(R.id.btn_play_pause, R.drawable.play);
        }
        String title = mService.getmAudioItem().mTitle;
        remoteViews.setTextViewText(R.id.txt_title, title);
        Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mService.getmAudioItem().mAlbumId);
        Picasso.with(mService).load(albumArtUri).error(R.drawable.empty_albumart).into(remoteViews, R.id.img_albumart, NOTIFICATION_PLAYER_ID, notification);
    }


}
