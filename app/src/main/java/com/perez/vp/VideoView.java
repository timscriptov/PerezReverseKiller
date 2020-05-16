package com.perez.vp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import java.io.IOException;

public class VideoView extends SurfaceView implements MediaPlayerControl {

    public int getAudioSessionId() {

        return 0;
    }

    private String TAG = "VideoView";

    private Context mContext;

    private Uri mUri;
    private int mDuration;

    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private boolean mIsPrepared;

    private int mVideoWidth;
    private int mVideoHeight;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private MediaController mMediaController;

    private OnCompletionListener mOnCompletionListener;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private int mCurrentBufferPercentage;

    private OnErrorListener mOnErrorListener;
    private boolean mStartWhenPrepared;
    private int mSeekWhenPrepared;

    private MySizeChangeLinstener mMyChangeLinstener;

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public void setVideoScale(int width, int height) {
        LayoutParams lp = getLayoutParams();
        lp.height = height;
        lp.width = width;
        setLayoutParams(lp);
    }

    public VideoView(Context context) {
        super(context);
        mContext = context;

        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;

        initVideoView();
    }

    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        initVideoView();
    }

    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);

        requestFocus();
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                                   int h) {

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            if(mMediaPlayer != null && mIsPrepared && mVideoWidth == w
                    && mVideoHeight == h) {
                if(mSeekWhenPrepared != 0) {
                    mMediaPlayer.seekTo(mSeekWhenPrepared);
                    mSeekWhenPrepared = 0;
                }

                mMediaPlayer.start();

                if(mMediaController != null)
                    mMediaController.show();
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceHolder = holder;
            openVideo();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {

            mSurfaceHolder = null;
            if(mMediaController != null)
                mMediaController.hide();
            if(mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    };

    private void openVideo() {
        if(mUri == null || mSurfaceHolder == null) {

            return;
        }

        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        if(mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        try {

            mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setOnPreparedListener(mPreparedListener);

            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mIsPrepared = false;

            mDuration = -1;

            mMediaPlayer.setOnCompletionListener(mCompletionListener);

            mMediaPlayer.setOnErrorListener(mErrorListener);

            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;

            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mMediaPlayer.setScreenOnWhilePlaying(true);

            mMediaPlayer.prepareAsync();

            attachMediaController();

        } catch(IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            return;
        } catch(IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            return;
        }
    }

    public void setMediaController(MediaController controller) {
        if(mMediaController != null)
            mMediaController.hide();
        mMediaController = controller;

        attachMediaController();
    }

    private void attachMediaController() {
        if(mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            View anchorView = this.getParent() instanceof View ? (View) this
                              .getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(mIsPrepared);
        }
    }

    public interface MySizeChangeLinstener {
        public void doMyThings();
    }

    public void setMySizeChangeLinstener(MySizeChangeLinstener l) {
        mMyChangeLinstener = l;
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if(mMyChangeLinstener != null)
                mMyChangeLinstener.doMyThings();

            if(mVideoWidth != 0 && mVideoHeight != 0)
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            if(mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(mMediaPlayer);

            if(mMediaController != null)
                mMediaController.setEnabled(true);

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if(mVideoWidth != 0 && mVideoHeight != 0) {

                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if(mSurfaceWidth == mVideoWidth
                        && mSurfaceHeight == mVideoHeight) {

                    if(mSeekWhenPrepared != 0) {
                        mMediaPlayer.seekTo(mSeekWhenPrepared);
                        mSeekWhenPrepared = 0;
                    }

                    if(mStartWhenPrepared) {
                        mMediaPlayer.start();
                        mStartWhenPrepared = false;

                        if(mMediaController != null)
                            mMediaController.show();
                    } else if(!isPlaying()
                              && (mSeekWhenPrepared != 0 || getCurrentPosition() > 0)) {
                        if(mMediaController != null) {

                            mMediaController.show(0);
                        }
                    }
                }
            } else {

                if(mSeekWhenPrepared != 0) {
                    mMediaPlayer.seekTo(mSeekWhenPrepared);
                    mSeekWhenPrepared = 0;
                }

                if(mStartWhenPrepared) {
                    mMediaPlayer.start();
                    mStartWhenPrepared = false;
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {

            if(mMediaController != null)
                mMediaController.hide();
            if(mOnCompletionListener != null)
                mOnCompletionListener.onCompletion(mMediaPlayer);
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {

            if(mMediaController != null)
                mMediaController.hide();
            if(mOnErrorListener != null) {
                if(mOnErrorListener.onError(mMediaPlayer, framework_err,
                                            impl_err))
                    return true;
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {

            mCurrentBufferPercentage = percent;
        }
    };

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch(specMode) {

        case MeasureSpec.UNSPECIFIED:
            result = desiredSize;
            break;

        case MeasureSpec.AT_MOST:
            result = Math.min(desiredSize, specSize);
            break;

        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result;
    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        mUri = uri;
        mStartWhenPrepared = false;
        mSeekWhenPrepared = 0;

        openVideo();
        requestLayout();

        invalidate();
    }

    public void stopPlayback() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mIsPrepared && mMediaPlayer != null && mMediaController != null)
            toggleMediaControlsVisiblity();
        return false;
    }

    private void toggleMediaControlsVisiblity() {
        if(mMediaController.isShowing())
            mMediaController.hide();
        else
            mMediaController.show();
    }

    public void start() {
        if(mMediaPlayer != null && mIsPrepared) {
            mMediaPlayer.start();
            mStartWhenPrepared = false;
        } else
            mStartWhenPrepared = true;
    }

    public void pause() {
        if(mMediaPlayer != null && mIsPrepared) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
        }
        mStartWhenPrepared = false;
    }

    public int getDuration() {
        if(mMediaPlayer != null && mIsPrepared) {
            if(mDuration > 0)
                return mDuration;

            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public int getCurrentPosition() {
        if(mMediaPlayer != null && mIsPrepared)
            return mMediaPlayer.getCurrentPosition();
        return 0;
    }

    public void seekTo(int msec) {
        if(mMediaPlayer != null && mIsPrepared)
            mMediaPlayer.seekTo(msec);
        else
            mSeekWhenPrepared = msec;
    }

    public boolean isPlaying() {
        if(mMediaPlayer != null && mIsPrepared)
            return mMediaPlayer.isPlaying();
        return false;
    }

    public int getBufferPercentage() {
        if(mMediaPlayer != null)
            return mCurrentBufferPercentage;
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }
}
