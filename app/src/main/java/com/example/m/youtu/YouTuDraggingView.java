package com.example.m.youtu;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by moyokoo on 2018/10/11.
 * 1. Activity must set android:configChanges="orientation|screenSize"
 * 2. DispatchLayout must set background
 * 3. YouTuDraggingView must set match_parent
 */

public class YouTuDraggingView extends RelativeLayout implements View.OnClickListener {


    interface Callback {
        void onVideoViewHide();

        void videoSize(int width, int height);

        void onIconClick(IconType iconType);
    }

    enum IconType {
        PAUSE,
        PLAY,
        CLOSE
    }

    Callback mCallback;
    IconType statusType = IconType.PLAY;

    // 可拖动的view 和下方的详情View
    DispatchLayout mBackgroundView;
    View mDetailView;
    View titleLayout;
    View pauseIv;
    View closeIv;
    View pauseLayout;
    View closeLayout;
    ImageView statusIv;
    ImageView fullScreenIv;
    View topToolsView;
    Activity mActivity;

    MarginViewWrapper mBackgroundViewWrapper;
    MarginViewWrapper mTopViewWrapper;
    MarginViewWrapper titleWrapper;
    MarginViewWrapper pauseIvWrapper;
    MarginViewWrapper closeIvWrapper;
    MarginViewWrapper topToolsViewWrapper;

    //滑动区间,取值为是topView最小化时距离屏幕顶端的高度
    float mRangeScrollY;
    float mRangeNodeScrollY;

    //当前的比例
    float nowStateScale;

    //节点最小的缩放比例
    float MIN_RATIO_HEIGHT_NODE = 0.45f;
    //最小的缩放比例
    float MIN_RATIO_HEIGHT = 0.35f;
    float MIN_RATIO_WIDTH = 0.9f;
    //播放器比例
    static final float VIDEO_RATIO = 16f / 9f;
    int finalVideoLeftRightOffset;

    //video布局的原始宽度
    float mTopViewOriginalWidth;
    //video布局的原始高度
    float mTopOriginalHeight;
    float mBackgroundOriginalHeight;

    //底部的距离
    float bottomHeight = DensityUtil.dip2px(getContext(), 60);
    long rangeDuration = 350;
    long dismissDuration = 100;
    //View所在的activity是否全屏
    boolean activityFullscreen = true;


    public YouTuDraggingView(Context context) {
        this(context, null);
    }

    public YouTuDraggingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YouTuDraggingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mActivity = getActivityFromView(this);
        addView(LayoutInflater.from(getContext()).inflate(R.layout.youtu_dispatch, null), 0);
        mBackgroundView = findViewById(R.id.backgroundView);
        View mTopView = findViewById(R.id.topView);
        topToolsView = findViewById(R.id.topToolsView);
        ImageView downIv = findViewById(R.id.downIv);
        fullScreenIv = findViewById(R.id.fullScreenIv);
        mDetailView = findViewById(R.id.detailView);
        titleLayout = findViewById(R.id.titleLayout);
        pauseLayout = findViewById(R.id.pauseLayout);
        closeLayout = findViewById(R.id.closeLayout);
        pauseIv = findViewById(R.id.pauseIv);
        closeIv = findViewById(R.id.closeIv);
        statusIv = findViewById(R.id.statusIv);
        pauseIv.setBackgroundResource(R.drawable.pause);
        statusIv.setBackgroundResource(R.drawable.pause_white);
        mBackgroundView.setParentView(this);
        setBackgroundColor(Color.BLACK);
        downIv.setOnClickListener(this);
        fullScreenIv.setOnClickListener(this);
        mTopView.setOnClickListener(this);
        pauseLayout.setOnClickListener(this);
        closeLayout.setOnClickListener(this);
        titleLayout.setOnClickListener(this);
        statusIv.setOnClickListener(this);

        //初始化包装类
        topToolsViewWrapper = new MarginViewWrapper(topToolsView);
        mBackgroundViewWrapper = new MarginViewWrapper(mBackgroundView);
        mTopViewWrapper = new MarginViewWrapper(mTopView);
        titleWrapper = new MarginViewWrapper(titleLayout);
        pauseIvWrapper = new MarginViewWrapper(pauseLayout);
        closeIvWrapper = new MarginViewWrapper(closeLayout);

        //当前缩放比例
        nowStateScale = 1f;

        if (isLandscape()) {
            mTopViewOriginalWidth = DensityUtil.getScreenW(getContext());
            if (activityFullscreen) {
                mTopOriginalHeight = DensityUtil.getScreenH(getContext());
            } else {
                mTopOriginalHeight = DensityUtil.getScreenH(getContext()) - DensityUtil.getStatusBarH(getContext());
            }
            MIN_RATIO_HEIGHT_NODE = 0.35f;
            MIN_RATIO_HEIGHT = 0.25f;
            mDetailView.setVisibility(View.GONE);
        } else {
            mTopViewOriginalWidth = mBackgroundView.getContext().getResources().getDisplayMetrics().widthPixels;
            mTopOriginalHeight = (mTopViewOriginalWidth / VIDEO_RATIO);
            mDetailView.setVisibility(View.VISIBLE);
        }


        mTopViewWrapper.setHeight(mTopOriginalHeight);
        mTopViewWrapper.setWidth(mTopViewOriginalWidth);

        topToolsViewWrapper.setHeight(mTopOriginalHeight);
        topToolsViewWrapper.setWidth(mTopViewOriginalWidth);

        if (MIN_RATIO_HEIGHT_NODE < MIN_RATIO_HEIGHT) {
            throw new RuntimeException("MIN_RATIO_HEIGHT_NODE can't smaller than MIN_RATIO_HEIGHT_NODE");
        }

    }

    public Boolean isLandscape() {
        return getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        resetRangeAndSize(false, false);
    }

    public void resetRangeAndSize() {
        resetRangeAndSize(true, true);
    }

    /**
     * @param exchange        是否需要对换宽高
     * @param deleteStatusBar 是否需要删除状态栏高度 全屏时切换后需要删除高度
     */
    public void resetRangeAndSize(boolean exchange, boolean deleteStatusBar) {

        if (exchange) {
            mTopViewOriginalWidth = this.getMeasuredHeight();
            resetTopOriginHeight(true);
            mRangeScrollY = this.getMeasuredWidth() - MIN_RATIO_HEIGHT * mTopOriginalHeight - bottomHeight;
            mRangeNodeScrollY = this.getMeasuredWidth() - MIN_RATIO_HEIGHT_NODE * mTopOriginalHeight - bottomHeight;
            mBackgroundOriginalHeight = this.getMeasuredWidth();
        } else {
            mTopViewOriginalWidth = this.getMeasuredWidth();
            resetTopOriginHeight(false);
            mRangeScrollY = this.getMeasuredHeight() - MIN_RATIO_HEIGHT * mTopOriginalHeight - bottomHeight;
            mRangeNodeScrollY = this.getMeasuredHeight() - MIN_RATIO_HEIGHT_NODE * mTopOriginalHeight - bottomHeight;
            mBackgroundOriginalHeight = this.getMeasuredHeight();
        }
        finalVideoLeftRightOffset = (int) ((mTopViewOriginalWidth - mTopViewOriginalWidth * MIN_RATIO_WIDTH) / 2);
        if (deleteStatusBar) {
            if (!activityFullscreen) {
                mRangeScrollY = mRangeScrollY - DensityUtil.getStatusBarH(getContext());
                mRangeNodeScrollY = mRangeNodeScrollY - DensityUtil.getStatusBarH(getContext());
            }
        }

    }

    void resetTopOriginHeight(boolean exchange) {
        if (isLandscape()) {
            mTopOriginalHeight = exchange ? this.getMeasuredWidth() : this.getMeasuredHeight();
            MIN_RATIO_HEIGHT_NODE = 0.35f;
            MIN_RATIO_HEIGHT = 0.25f;
            mDetailView.setVisibility(View.GONE);
        } else {
            mTopOriginalHeight = (mTopViewOriginalWidth / VIDEO_RATIO);
            MIN_RATIO_HEIGHT_NODE = 0.45f;
            MIN_RATIO_HEIGHT = 0.35f;
        }
    }

    void notifyStatus() {
        if (statusType == IconType.PLAY) {
            statusType = IconType.PAUSE;
            pauseIv.setBackgroundResource(R.drawable.play);
            statusIv.setBackgroundResource(R.drawable.play_white);
            mCallback.onIconClick(IconType.PAUSE);
        } else {
            statusType = IconType.PLAY;
            pauseIv.setBackgroundResource(R.drawable.pause);
            statusIv.setBackgroundResource(R.drawable.pause_white);
            mCallback.onIconClick(IconType.PLAY);
        }
    }

    void updateDismissView(int m) {
        ViewGroup.LayoutParams params = getLayoutParams();
        params.width = -1;
        params.height = -1;
        setLayoutParams(params);
        float fullY = mRangeScrollY + mTopOriginalHeight * MIN_RATIO_HEIGHT;
        float offset = mRangeScrollY;
        if (m < fullY && m > mRangeScrollY) {
            offset = mRangeScrollY + (m - mRangeScrollY);
        }
        if (m >= fullY) {
            offset = fullY;
        }
        if (m < mRangeScrollY) {
            offset = mRangeScrollY;
        }
        float alphaPercent = (m - mRangeScrollY) / (fullY - mRangeScrollY);
        mBackgroundViewWrapper.setMarginTop(Math.round(offset));
        mBackgroundViewWrapper.setMarginBottom(Math.round(bottomHeight - (m - mRangeScrollY)));
        mBackgroundView.setAlpha((1 - alphaPercent));
    }

    void updateVideoView(int m) {
        //如果当前状态是最小化，先把我们的的布局宽高设置为MATCH_PARENT
        if (nowStateScale == MIN_RATIO_HEIGHT) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = -1;
            params.height = -1;
            setLayoutParams(params);
        }

        //marginTop的值最大为allScrollY，最小为0
        if (m > mRangeScrollY)
            m = (int) mRangeScrollY;
        if (m < 0)
            m = 0;


        //视频View高度的百分比100% - 0%
        float marginPercent = (mRangeScrollY - m) / mRangeScrollY;
        float nodeMarginPercent = (mRangeNodeScrollY - m) / mRangeNodeScrollY;

        float videoNodeWidthPercent = MIN_RATIO_WIDTH + (1f - MIN_RATIO_WIDTH) * nodeMarginPercent;
        float videoNodeHeightPercent = MIN_RATIO_HEIGHT_NODE + (1f - MIN_RATIO_HEIGHT_NODE) * nodeMarginPercent;
        float detailPercent = m / mRangeNodeScrollY;


        e("mTopViewOriginalWidth:" + mTopViewOriginalWidth);
        int videoLeftRightOffset = (int) ((mTopViewOriginalWidth - mTopViewOriginalWidth * videoNodeWidthPercent) / 2);
        int detailBottomOffset = Math.round(bottomHeight * detailPercent);
        //不能超过底部间距
        if (detailBottomOffset >= bottomHeight) {
            detailBottomOffset = Math.round(bottomHeight);
        }

        if (m >= mRangeNodeScrollY) {
            mDetailView.setVisibility(View.GONE);
            float alphaPercent = (m - mRangeNodeScrollY) / (mRangeScrollY - mRangeNodeScrollY);
            //背景的移动高度
            float offHeight = Math.round(mBackgroundOriginalHeight * MIN_RATIO_HEIGHT_NODE - (m - mRangeNodeScrollY));
            //视频View的移动高度
            float offHeight2 = Math.round(mTopOriginalHeight * MIN_RATIO_HEIGHT_NODE - (m - mRangeNodeScrollY));
            mBackgroundViewWrapper.setMarginTop(m);
            mBackgroundViewWrapper.setHeight(offHeight);
            float videoRightOffset = (m - mRangeNodeScrollY) / (mRangeScrollY - mRangeNodeScrollY) * mTopViewOriginalWidth * MIN_RATIO_WIDTH * 2 / 3;
            float topViewWidth = Math.round(mTopViewOriginalWidth * MIN_RATIO_WIDTH - videoRightOffset);

            mTopViewWrapper.setHeight(offHeight2);
            mTopViewWrapper.setWidth(topViewWidth);
            topToolsViewWrapper.setHeight(offHeight2);
            topToolsViewWrapper.setWidth(topViewWidth);

            //头部达到最小宽度时,滑动的宽度
            float pieceWidth = mTopViewOriginalWidth * MIN_RATIO_WIDTH / 3;
            float minWidthOffset = topViewWidth - pieceWidth;
            float imageLayoutWidth = pieceWidth / 2;

            //最小布局时的控件位置
            float titleWidth = pieceWidth - minWidthOffset;
            float pauseWidth = imageLayoutWidth;
            float closeWidth = imageLayoutWidth;
            float pauseLeftOffset = pieceWidth - minWidthOffset;
            float closeLeftOffset = pieceWidth + pieceWidth / 2 - minWidthOffset;
            if (minWidthOffset >= pieceWidth) {
                titleWidth = 0;
                pauseLeftOffset = 0;
                pauseWidth = imageLayoutWidth - minWidthOffset + pieceWidth;
            }
            if (minWidthOffset >= pieceWidth + imageLayoutWidth) {
                pauseWidth = 0;
                closeLeftOffset = 0;
                closeWidth = imageLayoutWidth - minWidthOffset + pieceWidth + imageLayoutWidth;
            }
            if (minWidthOffset >= pieceWidth * 2) {
                closeWidth = 0;
            }

            titleWrapper.setWidth(Math.round(titleWidth));
            titleWrapper.setHeight(offHeight2);

            pauseIvWrapper.setWidth(Math.round(pauseWidth));
            pauseIvWrapper.setHeight(offHeight2);
            pauseIvWrapper.setMarginLeft(Math.round(pauseLeftOffset));


            closeIvWrapper.setWidth(closeWidth);
            closeIvWrapper.setHeight(offHeight2);
            closeIvWrapper.setMarginLeft(Math.round(closeLeftOffset));

            pauseIv.setAlpha(alphaPercent);
            closeIv.setAlpha(alphaPercent);
            titleLayout.setAlpha(alphaPercent);
        } else {
            mDetailView.setVisibility(View.VISIBLE);
            mBackgroundViewWrapper.setHeight(Math.round(mBackgroundOriginalHeight * videoNodeHeightPercent));
            mTopViewWrapper.setWidth(Math.round(mTopViewOriginalWidth * videoNodeWidthPercent));
            mTopViewWrapper.setHeight(Math.round(mTopOriginalHeight * videoNodeHeightPercent));
            topToolsViewWrapper.setHeight(Math.round(mTopOriginalHeight * videoNodeHeightPercent));
            topToolsViewWrapper.setWidth(Math.round(mTopViewOriginalWidth * videoNodeWidthPercent));

            mBackgroundViewWrapper.setMarginTop(m);
        }
        mBackgroundViewWrapper.setWidth(Math.round(mTopViewOriginalWidth * videoNodeWidthPercent));
        mBackgroundViewWrapper.setMarginRight(videoLeftRightOffset);
        mBackgroundViewWrapper.setMarginLeft(videoLeftRightOffset);
        mBackgroundViewWrapper.setMarginBottom(detailBottomOffset);
        mDetailView.setAlpha(marginPercent);
        this.getBackground().setAlpha((int) (marginPercent * 255 * 0.6f));
        mCallback.videoSize(mTopViewWrapper.getWidth(), mTopViewWrapper.getHeight());
        mBackgroundView.setAlpha(1);
    }

    public int getScreenHeight() {
        return DensityUtil.getScreenH(getContext()) - DensityUtil.getStatusBarH(getContext());
    }

    void dismissView() {
        float fullY = mRangeScrollY + mTopOriginalHeight * MIN_RATIO_HEIGHT;
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ObjectAnimator.ofFloat(mBackgroundView, "alpha", 1f, 0),
                ObjectAnimator.ofInt(mBackgroundViewWrapper, "marginTop",
                        mBackgroundViewWrapper.getMarginTop(), Math.round(fullY)));
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(INVISIBLE);
                mBackgroundView.setAlpha(1f);
            }
        });
        set.setDuration((long) (dismissDuration * mBackgroundViewWrapper.getMarginTop() / fullY)).start();

        if (mCallback != null)
            mCallback.onVideoViewHide();
    }

    void confirmState(float v, int dy) {
        if (mBackgroundViewWrapper.getMarginTop() >= mRangeScrollY) {
            if (v > 15) {
                dismissView();
            } else {
                goMin(true);
            }

        } else {
            //dy用于判断是否反方向滑动了
            //如果手指抬起时宽度达到一定值 或者 速度达到一定值 则改变状态
            if (nowStateScale == 1f) {
                if (mTopViewOriginalWidth - mBackgroundView.getWidth() >= mTopViewOriginalWidth * (1 - MIN_RATIO_WIDTH) / 3 || (v > 5 && dy > 0)) {
                    goMin();
                } else {
                    goMax();
                }
            } else {
                if (mTopViewOriginalWidth - mBackgroundView.getWidth() <= 2 * mTopViewOriginalWidth * (1 - MIN_RATIO_WIDTH) / 3 || (v > 5 && dy < 0)) {
                    goMax();
                } else {
                    goMin();
                }
            }
        }
    }

    public void fullScreenGoMin() {
        if (isLandscape()) {
            nowStateScale = MIN_RATIO_HEIGHT;
            topToolsView.setVisibility(View.GONE);
            if (!activityFullscreen) {
                mActivity.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            resetRangeAndSize(false, true);
            mBackgroundViewWrapper.setMarginTop(Math.round(mRangeScrollY));
            updateVideoView(Math.round(mRangeScrollY));
        } else {
            goMin();
        }
    }

    /**
     * 由于是在屏幕大小确定之前调用  所以需要将宽高对换并提前设置
     */
    public void goLandscapeMin() {
        nowStateScale = MIN_RATIO_HEIGHT;
        topToolsView.setVisibility(View.GONE);

        resetRangeAndSize();
        //获取的this.getMeasuredHeight()是减去状态栏高度的高度 所以变为原状态时需要将高度加回
        mTopViewOriginalWidth = mTopViewOriginalWidth + (activityFullscreen ? 0 : DensityUtil.getStatusBarH(getContext()));
        mBackgroundViewWrapper.setMarginTop(Math.round(mRangeScrollY));
        updateVideoView(Math.round(mRangeScrollY));
    }

    /**
     * 原理同上
     */
    public void goPortraitMin() {
        nowStateScale = MIN_RATIO_HEIGHT;
        topToolsView.setVisibility(View.GONE);

        resetRangeAndSize();
        mTopViewOriginalWidth = mTopViewOriginalWidth + (activityFullscreen ? 0 : DensityUtil.getStatusBarH(getContext()));
        mBackgroundViewWrapper.setMarginTop(Math.round(mRangeScrollY));
        updateVideoView(Math.round(mRangeScrollY));
    }

    public void goPortraitMax() {
        if (!activityFullscreen) {
            mActivity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        nowStateScale = 1f;
        topToolsView.setVisibility(View.VISIBLE);
        resetRangeAndSize();
        mBackgroundViewWrapper.setMarginTop(0);
        updateVideoView(0);
//        goMax();
    }

    public void goFullScreen() {
        topToolsView.setVisibility(View.VISIBLE);
        if (!activityFullscreen) {
            mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mBackgroundViewWrapper.setWidth(DensityUtil.getScreenW(getContext()));
        mBackgroundViewWrapper.setHeight(DensityUtil.getScreenH(getContext()));
        mTopViewWrapper.setWidth(DensityUtil.getScreenW(getContext()));
        mTopViewWrapper.setHeight(DensityUtil.getScreenH(getContext()));
        topToolsViewWrapper.setWidth(DensityUtil.getScreenW(getContext()));
        topToolsViewWrapper.setHeight(DensityUtil.getScreenH(getContext()));
        mCallback.videoSize(mTopViewWrapper.getWidth(), mTopViewWrapper.getHeight());
    }

    public void goMax() {
        if (nowStateScale == MIN_RATIO_HEIGHT) {
            ViewGroup.LayoutParams params = getLayoutParams();
            params.width = -1;
            params.height = -1;
            setLayoutParams(params);
        }


        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mBackgroundViewWrapper.getMarginTop(), 0);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();

                updateVideoView((int) value);
                if (value == 0) {
                    topToolsView.setVisibility(View.VISIBLE);
                    if (isLandscape()) {
                        goFullScreen();
                    }
                }
            }
        });
        valueAnimator.setDuration((long) (mBackgroundViewWrapper.getMarginTop() / mRangeScrollY * rangeDuration)).start();

        nowStateScale = 1.0f;
    }


    public void goMin() {
        topToolsView.setVisibility(View.GONE);
        goMin(false);
    }

    public void goMin(final boolean isDismissToMin) {
        final float fullTop = Math.abs(mBackgroundViewWrapper.getMarginTop() - mRangeScrollY);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mBackgroundViewWrapper.getMarginTop(), mRangeScrollY);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mBackgroundView.setAlpha(isDismissToMin ? (value / fullTop) : 1);

                updateVideoView((int) value);
                if (value == mRangeScrollY) {
                    ViewGroup.LayoutParams p = getLayoutParams();
                    p.width = -2;
                    p.height = -2;
                    setLayoutParams(p);
                    nowStateScale = MIN_RATIO_HEIGHT;
                }
            }
        });
        valueAnimator.setDuration((long) (Math.abs((1 - mBackgroundViewWrapper.getMarginTop() / mRangeScrollY)) * rangeDuration)).start();
    }


    //获取当前状态
    public float getNowStateScale() {
        return nowStateScale;
    }

    public void show() {
        setVisibility(VISIBLE);
        statusType = IconType.PLAY;
        pauseIv.setBackgroundResource(R.drawable.pause);
        //默认从最底部开始变换到顶部
        mBackgroundViewWrapper.setMarginTop((int) mRangeScrollY);
        goMax();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    class MarginViewWrapper {
        private ViewGroup.MarginLayoutParams params;
        private View viewWrapper;

        MarginViewWrapper(View view) {
            this.viewWrapper = view;
            params = (ViewGroup.MarginLayoutParams) viewWrapper.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = Gravity.START;
            }
        }


        int getWidth() {
            return params.width < 0 ? (int) mTopViewOriginalWidth : params.width;
        }

        int getHeight() {
            return params.height < 0 ? (int) mTopOriginalHeight : params.height;
        }

        void setWidth(float width) {
            if (width == mTopViewOriginalWidth) {
                params.width = -1;
                params.setMargins(0, 0, 0, 0);
            } else
                params.width = (int) width;

            viewWrapper.setLayoutParams(params);
        }

        void setHeight(float height) {
            params.height = (int) height;
            viewWrapper.setLayoutParams(params);
        }

        void setMarginTop(int m) {
            params.topMargin = m;
            viewWrapper.setLayoutParams(params);
        }

        void setMarginBottom(int m) {
            params.bottomMargin = m;
            viewWrapper.setLayoutParams(params);
        }

        int getMarginTop() {
            return params.topMargin;
        }

        void setMarginRight(int mr) {
            params.rightMargin = mr;
            viewWrapper.setLayoutParams(params);
        }

        void setMarginLeft(int mr) {
            params.leftMargin = mr;
            viewWrapper.setLayoutParams(params);
        }

        int getMarginRight() {
            return params.rightMargin;
        }

        int getMarginLeft() {
            return params.leftMargin;
        }

        int getMarginBottom() {
            return params.bottomMargin;
        }
    }

    public static Activity getActivityFromView(View view) {
        if (null != view) {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    return (Activity) context;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.downIv:
                fullScreenGoMin();
                break;
            case R.id.fullScreenIv:
                //0 传感器关闭 1 开启
                int screenChange = 1;
                try {
                    screenChange = Settings.System.getInt(getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                if (isLandscape()) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                if (screenChange == 1) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                }
                break;
            case R.id.topView:
                if (getNowStateScale() == MIN_RATIO_HEIGHT) {
                    goMax();
                } else if (getNowStateScale() == 1f) {
                    topToolsView.setVisibility(topToolsView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
                break;
            case R.id.pauseLayout:
                notifyStatus();
                break;
            case R.id.closeLayout:
                dismissView();
                mCallback.onIconClick(YouTuDraggingView.IconType.CLOSE);
                break;
            case R.id.titleLayout:
                goMax();
                break;
            case R.id.statusIv:
                notifyStatus();
                break;
        }
    }


    public void e(String msg) {
        Log.e("Youtu", msg);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //当前屏幕为横屏
            if (getNowStateScale() == 1f) {
                goFullScreen();
            } else if (getNowStateScale() == MIN_RATIO_HEIGHT) {
                goLandscapeMin();
            }

        } else {
            //当前屏幕为竖屏
            if (getNowStateScale() == 1f) {
                goPortraitMax();
            } else if (getNowStateScale() == MIN_RATIO_HEIGHT) {
                goPortraitMin();
            }

        }
    }
}