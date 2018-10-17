package com.example.m.youtu;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.salient.artplayer.AbsControlPanel;
import org.salient.artplayer.MediaPlayerManager;
import org.salient.artplayer.Utils;
import org.salient.artplayer.VideoView;
import org.salient.artplayer.ui.ControlPanel;
import org.salient.artplayer.ui.VideoGestureListener;

public class YoutubeControlPanel extends AbsControlPanel implements SeekBar.OnSeekBarChangeListener {

    private final String TAG = YoutubeControlPanel.class.getSimpleName();

    private final long autoDismissTime = 3000;
    private int mWhat;
    private int mExtra;
    protected GestureDetector mGestureDetector;

    private ImageView startCenterIv;
    private SeekBar bottom_seek_progress;
    private View layout_bottom;
    private View layout_top;
    private TextView current;
    private TextView total;
    private ProgressBar loading;
    private ImageView ivLeft;
    private ImageView video_cover;
    private ImageView fullScreenIv;
    private LinearLayout llAlert;
    private TextView tvAlert;
    private TextView tvConfirm;
    private TextView tvTitle;
    private LinearLayout llOperation;
    private LinearLayout llProgressTime;

    private Runnable mDismissTask = new Runnable() {
        @Override
        public void run() {
            if (MediaPlayerManager.instance().getCurrentVideoView() == mTarget && MediaPlayerManager.instance().isPlaying()) {
                hideUI(layout_bottom, layout_top, startCenterIv);
            }
        }
    };

    public YoutubeControlPanel(Context context) {
        super(context);
    }

    public YoutubeControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YoutubeControlPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getResourceId() {
        return R.layout.layout_youtube_control_panel;
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        startCenterIv = findViewById(R.id.startCenterIv);
        bottom_seek_progress = findViewById(R.id.bottom_seek_progress);
        layout_bottom = findViewById(R.id.layout_bottom);
        layout_top = findViewById(R.id.layout_top);
        current = findViewById(R.id.current);
        total = findViewById(R.id.total);
        loading = findViewById(R.id.loading);
        ivLeft = findViewById(R.id.ivLeft);
        video_cover = findViewById(R.id.video_cover);
        llAlert = findViewById(R.id.llAlert);
        tvAlert = findViewById(R.id.tvAlert);
        tvConfirm = findViewById(R.id.tvConfirm);
        fullScreenIv = findViewById(R.id.ivRight);
        tvTitle = findViewById(R.id.tvTitle);
        llOperation = findViewById(R.id.llOperation);
        llProgressTime = findViewById(R.id.llProgressTime);


        fullScreenIv.setOnClickListener(this);
        ivLeft.setOnClickListener(this);
        bottom_seek_progress.setOnSeekBarChangeListener(this);
        startCenterIv.setOnClickListener(this);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTarget == null) return;
                if (!mTarget.isCurrentPlaying()) {
                    return;
                }
                if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.PLAYING) {
                    cancelDismissTask();
                    if (layout_bottom.getVisibility() != VISIBLE) {
                        showUI(startCenterIv,layout_bottom, layout_top);
                    } else {
                        hideUI(layout_top, layout_bottom,startCenterIv);
                    }
                    startDismissTask();
                }
            }
        });
        final VideoGestureListener videoGestureListener = new VideoGestureListener(this);
        mGestureDetector = new GestureDetector(getContext(), videoGestureListener);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureDetector.onTouchEvent(event)) return true;
                return videoGestureListener.onTouch(v, event);
            }
        });
    }

    @Override
    public void onStateError() {
        hideUI(startCenterIv, layout_top, layout_bottom, loading);
        showUI(llAlert);
        //MediaPlayerManager.instance().releaseMediaPlayer();
        tvAlert.setText("oops~~ unknown error");
        tvConfirm.setText("retry");
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTarget != null) {
                    hideUI(llAlert);
                    mTarget.start();
                }
            }
        });
    }

    @Override
    public void onStateIdle() {
        hideUI(startCenterIv,layout_bottom, layout_top, loading, llAlert);
        showUI(video_cover);
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        if (mTarget != null && mTarget.getParentVideoView() != null && mTarget.getParentVideoView().getControlPanel() != null) {
            TextView title = mTarget.getParentVideoView().getControlPanel().findViewById(R.id.tvTitle);
            tvTitle.setText(title.getText() == null ? "" : title.getText());
        }
    }

    @Override
    public void onStatePreparing() {
        showUI(loading);
    }

    @Override
    public void onStatePrepared() {
        hideUI(loading);
    }

    @Override
    public void onStatePlaying() {
        startCenterIv.setBackgroundResource(R.drawable.pause_white);
        showUI(startCenterIv,layout_bottom, layout_top);
        hideUI(video_cover, loading, llOperation, llProgressTime);
        startDismissTask();
    }

    @Override
    public void onStatePaused() {
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        showUI(startCenterIv,layout_bottom);
        hideUI(video_cover, loading, llOperation, llProgressTime);
    }

    @Override
    public void onStatePlaybackCompleted() {
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        showUI(startCenterIv,layout_bottom);
        hideUI(loading);
        if (mTarget.getWindowType() == VideoView.WindowType.FULLSCREEN || mTarget.getWindowType() == VideoView.WindowType.TINY) {
            showUI(layout_top);
        }
    }

    @Override
    public void onSeekComplete() {

    }

    @Override
    public void onBufferingUpdate(int progress) {
        if (progress != 0) bottom_seek_progress.setSecondaryProgress(progress);
    }

    @Override
    public void onInfo(int what, int extra) {
        mWhat = what;
        mExtra = extra;
    }

    @Override
    public void onProgressUpdate(final int progress, final long position, final long duration) {
//        Log.e(TAG, "progress:" + progress + "     position:" + position + "    duration:" + duration);
        post(new Runnable() {
            @Override
            public void run() {
                bottom_seek_progress.setProgress(progress);
                current.setText(Utils.stringForTime(position));
                total.setText(Utils.stringForTime(duration));
            }
        });
    }

    @Override
    public void onEnterSecondScreen() {
        if (mTarget != null && mTarget.getWindowType() == VideoView.WindowType.FULLSCREEN) {
            hideUI(fullScreenIv);
        }
        showUI(ivLeft);
        SynchronizeViewState();
    }

    @Override
    public void onExitSecondScreen() {
        if (mTarget != null && mTarget.getWindowType() != VideoView.WindowType.TINY) {
            ivLeft.setVisibility(GONE);
        }
        showUI(fullScreenIv);
        SynchronizeViewState();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        MediaPlayerManager.instance().cancelProgressTimer();
        cancelDismissTask();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        MediaPlayerManager.instance().startProgressTimer();
        startDismissTask();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (MediaPlayerManager.instance().getPlayerState() != MediaPlayerManager.PlayerState.PLAYING &&
                MediaPlayerManager.instance().getPlayerState() != MediaPlayerManager.PlayerState.PAUSED)
            return;
        long time = (long) (seekBar.getProgress() * 1.00 / 100 * MediaPlayerManager.instance().getDuration());
        MediaPlayerManager.instance().seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            long duration = MediaPlayerManager.instance().getDuration();
            current.setText(Utils.stringForTime(progress / 100 * duration));
        }
    }

    //显示WiFi状态提醒
    public void showWifiAlert() {
        hideUI(startCenterIv, layout_bottom, layout_top, loading);
        showUI(llAlert);
        tvAlert.setText("Is in non-WIFI");
        tvConfirm.setText("continue");
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTarget != null) {
                    hideUI(llAlert);
                    mTarget.start();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        cancelDismissTask();
        int id = v.getId();
        if (id == R.id.ivLeft) {
            if (mTarget == null) return;
            if (mTarget.getWindowType() == VideoView.WindowType.FULLSCREEN) {
                mTarget.exitFullscreen();
            } else if (mTarget.getWindowType() == VideoView.WindowType.TINY) {
                mTarget.exitTinyWindow();
            }
        } else if (id == R.id.ivRight) {
            if (mTarget == null) return;
            if (mTarget.getWindowType() != VideoView.WindowType.FULLSCREEN) {
                //new VideoView
                VideoView videoView = new VideoView(getContext());
                //set parent
                videoView.setParentVideoView(mTarget);
                videoView.setUp(mTarget.getDataSourceObject(), VideoView.WindowType.FULLSCREEN, mTarget.getData());
                videoView.setControlPanel(new ControlPanel(getContext()));
                //startCenterIv fullscreen
                videoView.startFullscreen(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                //MediaPlayerManager.instance().startFullscreen(videoView, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }

        }  else if (id == R.id.startCenterIv) {
            if (mTarget == null) {
                return;
            }
            if (mTarget.isCurrentPlaying() && MediaPlayerManager.instance().isPlaying()) {
                mTarget.pause();
            } else {
                if (!Utils.isNetConnected(getContext())) {
                    onStateError();
                    return;
                }
                if (!Utils.isWifiConnected(getContext())) {
                    showWifiAlert();
                    return;
                }
                mTarget.start();
            }

        }
        startDismissTask();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }

    //同步跟MediaPlayer状态无关的视图
    public void SynchronizeViewState() {
        if (MediaPlayerManager.instance().getPlayerState() != MediaPlayerManager.PlayerState.PLAYING
                && MediaPlayerManager.instance().getPlayerState() != MediaPlayerManager.PlayerState.PAUSED) {
            showUI(startCenterIv);
        } else {
            hideUI(startCenterIv);
        }
        if (mTarget != null && mTarget.getParentVideoView() != null && mTarget.getParentVideoView().getControlPanel() != null) {
            TextView title = mTarget.getParentVideoView().getControlPanel().findViewById(R.id.tvTitle);
            tvTitle.setText(title.getText() == null ? "" : title.getText());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelDismissTask();
    }

    private void startDismissTask() {
        cancelDismissTask();
        postDelayed(mDismissTask, autoDismissTime);
    }

    private void cancelDismissTask() {
        Handler handler = getHandler();
        if (handler != null && mDismissTask != null) {
            handler.removeCallbacks(mDismissTask);
        }
    }
}
