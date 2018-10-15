package com.example.m.youtu;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.io.IOException;
import java.net.MalformedURLException;

public class MainActivity extends AppCompatActivity implements YouTuDraggingView.Callback, MediaPlayer.OnPreparedListener, TextureView.SurfaceTextureListener {

    private TextureView mVideoView;
    private MediaPlayer mMediaPlayer;
    private YouTuDraggingView mYouTuDraggingView;
    private float mVideoWidth;
    private float mVideoHeight;
    String TAG = "Youtu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mVideoWidth = DensityUtil.getScreenW(this);
        mVideoHeight = (mVideoWidth * 9 / 16);
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(ArrayAdapter.createFromResource(this, R.array.program_list, android.R.layout.simple_list_item_1));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playVideo();
            }
        });
        mVideoView = findViewById(R.id.videoView);
        mVideoView.setSurfaceTextureListener(this);
        mYouTuDraggingView = findViewById(R.id.youtube_view);
        mYouTuDraggingView.setCallback(this);
    }

    private void playVideo() {
        mYouTuDraggingView.show();
    }


    @Override
    public void onIconClick(YouTuDraggingView.IconType iconType) {
        if (iconType == YouTuDraggingView.IconType.CLOSE) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
        } else if (iconType == YouTuDraggingView.IconType.PAUSE) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
        } else if (iconType == YouTuDraggingView.IconType.PLAY) {
            if (mMediaPlayer.isPlaying())
                return;
            mMediaPlayer.start();
        }
    }

    @Override
    public void onVideoViewHide() {
        mMediaPlayer.pause();
    }

    @Override
    public void videoSize(int width, int height) {
        updateTextureViewSize(width, height);
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.setLooping(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);

        try {
            try {
                mMediaPlayer = new MediaPlayer();
                String streamPath = "http://bmob-cdn-982.b0.upaiyun.com/2017/02/23/266454624066f2b680707492a0664a97.mp4";
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(streamPath);
                mMediaPlayer.setSurface(surface);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepareAsync();
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Wrong url for mediaplayer! " + ex);
            } catch (IllegalStateException ex) {
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException("Wrong url for mediaplayer! " + ex);
            }

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        finish();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);
        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX = viewWidth / 2;
        int pivotPointY = viewHeight / 2;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);

        mVideoView.setTransform(matrix);
        mVideoView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mYouTuDraggingView.getNowStateScale() == 1f) {
            mYouTuDraggingView.goMin();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
