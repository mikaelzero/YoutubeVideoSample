package com.example.m.youtu;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.salient.artplayer.AbsControlPanel;
import org.salient.artplayer.MediaPlayerManager;
import org.salient.artplayer.Utils;
import org.salient.artplayer.VideoView;
import org.salient.artplayer.ui.VideoGestureListener;

import java.util.List;

public class YoutubeControlPanel extends AbsControlPanel implements SeekBar.OnSeekBarChangeListener {

    private final String TAG = YoutubeControlPanel.class.getSimpleName();

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
    private ImageView downIv;
    private ImageView video_cover;
    private ImageView fullScreenIv;
    private LinearLayout errorLayout;
    private TextView tvAlert;
    private TextView tvConfirm;
    private TextView tvTitle;
    private LinearLayout llOperation;
    private LinearLayout llProgressTime;
    View minErrorIv;
    FrameLayout backgroundLayout;
    YouTuDraggingView youTuDraggingView;

    public ImageView getFullScreenIv() {
        return fullScreenIv;
    }

    public ImageView getDownIv() {
        return downIv;
    }

    public YoutubeControlPanel(Context context) {
        super(context);
    }

    public YoutubeControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public YoutubeControlPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setYouTuDraggingView(YouTuDraggingView youTuDraggingView) {
        this.youTuDraggingView = youTuDraggingView;
    }

    @Override
    protected int getResourceId() {
        return R.layout.layout_youtube_control_panel;
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        minErrorIv = findViewById(R.id.minErrorIv);
        backgroundLayout = findViewById(R.id.backgroundLayout);
        startCenterIv = findViewById(R.id.startCenterIv);
        bottom_seek_progress = findViewById(R.id.bottom_seek_progress);
        layout_bottom = findViewById(R.id.layout_bottom);
        layout_top = findViewById(R.id.layout_top);
        current = findViewById(R.id.current);
        total = findViewById(R.id.total);
        loading = findViewById(R.id.loading);
        downIv = findViewById(R.id.downIv);
        video_cover = findViewById(R.id.video_cover);
        errorLayout = findViewById(R.id.errorLayout);
        tvAlert = findViewById(R.id.tvAlert);
        tvConfirm = findViewById(R.id.tvConfirm);
        fullScreenIv = findViewById(R.id.fullScreenIv);
        tvTitle = findViewById(R.id.tvTitle);
        llOperation = findViewById(R.id.llOperation);
        llProgressTime = findViewById(R.id.llProgressTime);

        downIv.setOnClickListener(this);
        bottom_seek_progress.setOnSeekBarChangeListener(this);
        startCenterIv.setOnClickListener(this);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTarget == null) return;
                if (youTuDraggingView.isMin()) {
                    youTuDraggingView.goMax();
                    return;
                }
                if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.ERROR) {
                    return;
                }
                if (layout_bottom.getVisibility() != VISIBLE) {
                    showUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress);
                    bgAlphaToBlack();
                } else {
                    hideUI(layout_top, layout_bottom, startCenterIv, bottom_seek_progress);
                    bgAlphaToTrans();
                }
                if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.PREPARING) {
                    hideUI(bottom_seek_progress, startCenterIv);
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
        bgAlphaToBlack();
    }

    /**
     * @param status 1 max 0 min
     */
    void showOrHide(int status) {
        if (status == YouTuDraggingView.STATUS_MAX) {
            setVisibility(View.VISIBLE);
            //加载状态
            if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.PREPARING) {
                //正在缓冲
                showUI(loading);
                bgAlphaToTrans();
                hideUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress, errorLayout, minErrorIv);
            } else if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.ERROR) {
                showUI(errorLayout);
                hideUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress, minErrorIv, loading);
            } else {
                showUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress);
                hideUI(errorLayout, loading, minErrorIv);
                bgAlphaToBlack();
            }
        } else if (status == YouTuDraggingView.STATUS_MIN) {
            setVisibility(View.VISIBLE);
            //加载状态
            if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.PREPARING) {
                //正在缓冲
                showUI(loading);
                bgAlphaToTrans();
            } else if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.ERROR) {
                showUI(minErrorIv);
                hideUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress, errorLayout, loading);
            } else {
                setVisibility(View.GONE);
                bgAlphaToTrans();
            }
        } else if (status == YouTuDraggingView.STATUS_DRAG) {
            if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.PREPARING) {
                setVisibility(View.VISIBLE);
                showUI(loading);
                bgAlphaToTrans();
            } else if (MediaPlayerManager.instance().getPlayerState() == MediaPlayerManager.PlayerState.ERROR) {
                setVisibility(View.VISIBLE);
                showUI(minErrorIv);
                hideUI(startCenterIv, layout_bottom, layout_top, bottom_seek_progress, errorLayout, loading);
            } else {
                setVisibility(View.GONE);
            }
        }
    }

    void bgAlphaToBlack() {
        ObjectAnimator.ofFloat(backgroundLayout, "alpha", backgroundLayout.getAlpha(), 0.8f)
                .setDuration(300).start();
    }

    void bgAlphaToTrans() {
        ObjectAnimator.ofFloat(backgroundLayout, "alpha", backgroundLayout.getAlpha(), 0f)
                .setDuration(300).start();
    }

    @Override
    public void onStateError() {
        tvAlert.setText("播放出现错误");
        tvConfirm.setText("重试");
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (youTuDraggingView.isMin()) {
                    youTuDraggingView.goMax();
                    return;
                }
                if (mTarget != null) {
                    hideUI(errorLayout);
                    mTarget.start();
                }
            }
        });
        showOrHide(youTuDraggingView.isMax() ? 1 : 0);
    }

    @Override
    public void onStateIdle() {
        hideUI(startCenterIv, layout_bottom, layout_top, loading, errorLayout, bottom_seek_progress);
        bgAlphaToTrans();
        showUI(video_cover);
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        youTuDraggingView.changeStatus(YouTuDraggingView.IconType.PLAY);
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
        youTuDraggingView.changeStatus(YouTuDraggingView.IconType.PAUSE);
        showOrHide(youTuDraggingView.isMax() ? 1 : 0);
    }

    @Override
    public void onStatePaused() {
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        youTuDraggingView.changeStatus(YouTuDraggingView.IconType.PLAY);
        showOrHide(youTuDraggingView.isMax() ? 1 : 0);
    }

    @Override
    public void onStatePlaybackCompleted() {
        startCenterIv.setBackgroundResource(R.drawable.play_white);
        youTuDraggingView.changeStatus(YouTuDraggingView.IconType.PLAY);
        showOrHide(youTuDraggingView.isMax() ? 1 : 0);
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
        showUI(downIv);
        SynchronizeViewState();
    }

    @Override
    public void onExitSecondScreen() {
        if (mTarget != null && mTarget.getWindowType() != VideoView.WindowType.TINY) {
            downIv.setVisibility(GONE);
        }
        showUI(fullScreenIv);
        SynchronizeViewState();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        MediaPlayerManager.instance().cancelProgressTimer();
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
        showUI(errorLayout);
        tvAlert.setText("Is in non-WIFI");
        tvConfirm.setText("continue");
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTarget != null) {
                    hideUI(errorLayout);
                    mTarget.start();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.startCenterIv) {
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


}
