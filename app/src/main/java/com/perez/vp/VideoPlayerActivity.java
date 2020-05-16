package com.perez.vp;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.perez.revkiller.R;
import com.perez.vp.SoundView.OnVolumeChangedListener;
import com.perez.vp.VideoView.MySizeChangeLinstener;
import android.support.v7.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    public static LinkedList<MovieInfo> playList = new LinkedList<MovieInfo>();

    public class MovieInfo {

        String displayName;

        String path;
    }

    private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

    private static int position;

    private int playedTime;

    private VideoView vv = null;

    private SeekBar seekBar = null;

    private TextView durationTextView = null;

    private TextView playedTextView = null;

    private GestureDetector mGestureDetector = null;

    private AudioManager mAudioManager = null;

    private int maxVolume = 0;
    private int currentVolume = 0;

    private ImageButton bn3 = null;

    private ImageButton bn5 = null;

    private View controlView = null;

    private PopupWindow controler = null;

    private SoundView mSoundView = null;
    private PopupWindow mSoundWindow = null;

    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int controlHeight = 0;

    private final static int TIME = 6868;

    private boolean isControllerShow = true;
    private boolean isPaused = false;
    private boolean isFullScreen = false;
    private boolean isSilent = false;
    private boolean isSoundShow = false;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        Looper.myQueue().addIdleHandler(new IdleHandler() {

            @Override
            public boolean queueIdle() {

                if(controler != null && vv.isShown()) {
                    controler.showAtLocation(vv, Gravity.BOTTOM, 0, 0);

                    controler.update(0, 0, screenWidth, controlHeight);
                }

                return false;
            }
        });

        controlView = getLayoutInflater().inflate(R.layout.controler, null);
        controler = new PopupWindow(controlView);

        durationTextView = (TextView) controlView.findViewById(R.id.duration);

        playedTextView = (TextView) controlView.findViewById(R.id.has_played);

        mSoundView = new SoundView(this);
        mSoundView.setOnVolumeChangeListener(new OnVolumeChangedListener() {

            @Override
            public void setYourVolume(int index) {

                cancelDelayHide();

                updateVolume(index);

                hideControllerDelay();
            }
        });

        mSoundWindow = new PopupWindow(mSoundView);
        position = -1;

        bn3 = (ImageButton) controlView.findViewById(R.id.button3);
        bn5 = (ImageButton) controlView.findViewById(R.id.button5);

        vv = (VideoView) findViewById(R.id.vv);

        Uri uri = getIntent().getData();
        if(uri != null) {
            if(vv.getVideoHeight() == 0)
                vv.setVideoURI(uri);
            bn3.setImageResource(R.drawable.pause);
        } else
            bn3.setImageResource(R.drawable.play);

        getVideoFile(playList, new File("/sdcard/"));

        Cursor cursor = getContentResolver().query(videoListUri,
                        new String[] { "_display_name", "_data" }, null, null, null);
        int n = cursor.getCount();
        cursor.moveToFirst();
        LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();

        for(int i = 0; i != n; ++i) {
            MovieInfo mInfo = new MovieInfo();
            mInfo.displayName = cursor.getString(cursor
                                                 .getColumnIndex("_display_name"));
            mInfo.path = cursor.getString(cursor.getColumnIndex("_data"));
            playList2.add(mInfo);
            cursor.moveToNext();
        }

        if(playList2.size() > playList.size())
            playList = playList2;

        vv.setMySizeChangeLinstener(new MySizeChangeLinstener() {
            @Override
            public void doMyThings() {

                setVideoScale(SCREEN_DEFAULT);
            }
        });

        bn3.setAlpha(0xBB);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager
                        .getStreamVolume(AudioManager.STREAM_MUSIC);

        bn5.setAlpha(findAlphaFromSound());

        bn3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                cancelDelayHide();

                if(isPaused) {
                    vv.start();
                    bn3.setImageResource(R.drawable.pause);
                    hideControllerDelay();
                } else {
                    vv.pause();
                    bn3.setImageResource(R.drawable.play);
                }

                isPaused = !isPaused;
            }
        });

        bn5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                cancelDelayHide();

                if(isSoundShow)
                    mSoundWindow.dismiss();
                else {

                    if(mSoundWindow.isShowing()) {
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH,
                                            SoundView.MY_HEIGHT);
                    } else {
                        mSoundWindow.showAtLocation(vv, Gravity.RIGHT
                                                    | Gravity.CENTER_VERTICAL, 15, 0);
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH,
                                            SoundView.MY_HEIGHT);
                    }
                }
                isSoundShow = !isSoundShow;
                hideControllerDelay();
            }
        });

        bn5.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {

                if(isSilent)
                    bn5.setImageResource(R.drawable.soundenable);
                else
                    bn5.setImageResource(R.drawable.sounddisable);
                isSilent = !isSilent;

                updateVolume(currentVolume);

                cancelDelayHide();

                hideControllerDelay();
                return true;
            }
        });

        seekBar = (SeekBar) controlView.findViewById(R.id.seekbar);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                                          boolean fromUser) {

                if(fromUser)
                    vv.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                myHandler.removeMessages(HIDE_CONTROLER);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
            }
        });

        getScreenSize();

        mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(final MotionEvent e) {

                if(isFullScreen)
                    setVideoScale(SCREEN_DEFAULT);
                else
                    setVideoScale(SCREEN_FULL);

                isFullScreen = !isFullScreen;

                if(isControllerShow)
                    showController();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {

                if(!isControllerShow) {
                    showController();
                    hideControllerDelay();
                } else {

                    cancelDelayHide();
                    hideController();
                }
                return true;
            }

            @Override
            public void onLongPress(final MotionEvent e) {

                if(isPaused) {
                    vv.start();
                    bn3.setImageResource(R.drawable.pause);
                    cancelDelayHide();
                    hideControllerDelay();
                } else {
                    vv.pause();
                    bn3.setImageResource(R.drawable.play);
                    cancelDelayHide();
                    showController();
                }

                isPaused = !isPaused;
            }
        });

        vv.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer arg0) {

                setVideoScale(SCREEN_DEFAULT);

                isFullScreen = false;
                if(isControllerShow)
                    showController();

                int i = vv.getDuration();
                seekBar.setMax(i);
                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;

                durationTextView.setText(String.format("%02d:%02d:%02d", hour,
                                                       minute, second));
                vv.start();
                bn3.setImageResource(R.drawable.pause);
                hideControllerDelay();
                myHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });

        vv.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer arg0) {

                int n = playList.size();

                if(++position < n)
                    vv.setVideoPath(playList.get(position).path);
                else
                    VideoPlayerActivity.this.finish();
            }
        });
    }

    private final static int PROGRESS_CHANGED = 0;

    private final static int HIDE_CONTROLER = 1;

    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {

            case PROGRESS_CHANGED:

                int i = vv.getCurrentPosition();
                seekBar.setProgress(i);

                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                playedTextView.setText(String.format("%02d:%02d:%02d", hour,
                                                     minute, second));
                sendEmptyMessage(PROGRESS_CHANGED);
                break;
            case HIDE_CONTROLER:

                hideController();
                break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean result = mGestureDetector.onTouchEvent(event);
        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        getScreenSize();
        if(isControllerShow) {

            cancelDelayHide();

            hideController();

            showController();

            hideControllerDelay();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {

        playedTime = vv.getCurrentPosition();

        vv.pause();

        bn3.setImageResource(R.drawable.play);
        super.onPause();
    }

    @Override
    protected void onResume() {

        vv.seekTo(playedTime);
        vv.start();
        if(vv.getVideoHeight() != 0) {
            bn3.setImageResource(R.drawable.pause);
            hideControllerDelay();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        if(controler.isShowing())
            controler.dismiss();

        if(mSoundWindow.isShowing())
            mSoundWindow.dismiss();

        myHandler.removeMessages(PROGRESS_CHANGED);
        myHandler.removeMessages(HIDE_CONTROLER);

        playList.clear();
        super.onDestroy();
    }

    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        controlHeight = screenHeight / 4;
    }

    private void hideController() {

        if(controler.isShowing()) {
            controler.update(0, 0, 0, 0);
            isControllerShow = false;
        }

        if(mSoundWindow.isShowing()) {
            mSoundWindow.dismiss();
            isSoundShow = false;
        }
    }

    private void hideControllerDelay() {
        myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
    }

    private void showController() {
        controler.update(0, 0, screenWidth, controlHeight);
        isControllerShow = true;
    }

    private void cancelDelayHide() {
        myHandler.removeMessages(HIDE_CONTROLER);
    }

    private final static int SCREEN_FULL = 0;
    private final static int SCREEN_DEFAULT = 1;

    private void setVideoScale(int flag) {
        vv.getLayoutParams();
        switch(flag) {

        case SCREEN_FULL:
            vv.setVideoScale(screenWidth, screenHeight);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            break;

        case SCREEN_DEFAULT:

            int videoWidth = vv.getVideoWidth();
            int videoHeight = vv.getVideoHeight();

            int mWidth = screenWidth;
            int mHeight = screenHeight - 25;

            if(videoWidth > 0 && videoHeight > 0) {
                if(videoWidth * mHeight > mWidth * videoHeight)
                    mHeight = mWidth * videoHeight / videoWidth;
                else if(videoWidth * mHeight < mWidth * videoHeight)
                    mWidth = mHeight * videoWidth / videoHeight;
            }

            vv.setVideoScale(mWidth, mHeight);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            break;
        }
    }

    private int findAlphaFromSound() {
        if(mAudioManager != null) {
            int alpha = currentVolume * (0xCC - 0x55) / maxVolume + 0x55;
            return alpha;
        } else
            return 0xCC;
    }

    private void updateVolume(int index) {
        if(mAudioManager != null) {
            if(isSilent) {

                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index,
                                              0);
            }
            currentVolume = index;

            bn5.setAlpha(findAlphaFromSound());
        }
    }

    private void getVideoFile(final LinkedList<MovieInfo> list, File file) {

        file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {

                String name = file.getName();
                int i = name.indexOf('.');
                if(i != -1) {
                    name = name.substring(i);

                    if(name.equalsIgnoreCase(".mp4")
                            || name.equalsIgnoreCase(".3gp")) {

                        MovieInfo mi = new MovieInfo();

                        mi.displayName = file.getName();

                        mi.path = file.getAbsolutePath();
                        list.add(mi);
                        return true;
                    }
                } else if(file.isDirectory())
                    getVideoFile(list, file);
                return false;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            stopService(new Intent(VideoPlayerActivity.this,
                                   VideoPlayerActivity.class));
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}