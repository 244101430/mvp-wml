package com.lantu.andorid.mvp_wml.ui.video;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;
import android.widget.ImageView;

import com.lantu.andorid.mvp_wml.R;
import com.lantu.andorid.mvp_wml.ui.base.IBaseView;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

/**
 * 视频页面逻辑
 * Created by wml on 2017/12/11.
 */

public class VideoActivityPresenter implements IVideoActivityPresenter {

    private final IBaseView mView;
    private OrientationUtils orientationUtils;
    private boolean isPlay = false;
    private boolean isPause;
    String url = "https://1251983981.vod2.myqcloud.com/a003942evodgzp1251983981/2f6955769031868223059449897/LN7g2USC7CYA.mp4";

    public VideoActivityPresenter(IBaseView mView) {
        this.mView = mView;
    }


    @Override
    public void getData(boolean isRefresh) {

    }

    @Override
    public void getMoreData() {

    }

    @Override
    public void initVideo(final Activity activity, final StandardGSYVideoPlayer mVideoPlayer) {
        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(activity, mVideoPlayer);

        mVideoPlayer.setUp(url, false, null, "测试视频");
        // 设置封面
        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setImageResource(R.drawable.ic_github);
        mVideoPlayer.setThumbImageView(imageView);

        //增加title
        mVideoPlayer.getTitleTextView().setVisibility(View.GONE);
        mVideoPlayer.getTitleTextView().setText("测试视频");
        mVideoPlayer.getBackButton().setVisibility(View.GONE);

        orientationUtils.setEnable(false);//初始化不打开外部的旋转

        mVideoPlayer.setIsTouchWiget(true);//是否可以滑动界面改变进度，声音等
        mVideoPlayer.setRotateViewAuto(false);//关闭自动旋转
        mVideoPlayer.setNeedLockFull(true);//是否需要全屏锁定屏幕功能

        /**
         * 全屏按钮点击事件
         */
        mVideoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orientationUtils.resolveByClick();//直接横屏
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                mVideoPlayer.startWindowFullscreen(activity, true, true);
            }
        });

        /**
         * 标准播放器的回调
         */
        mVideoPlayer.setStandardVideoAllCallBack(new SampleListener() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                //开始播放了才能旋转和全屏
                isPlay = true;
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
                onQuitFullscreen(url, objects);//播放完成退出全屏
            }

            @Override
            public void onClickStartError(String url, Object... objects) {
                super.onClickStartError(url, objects);
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Activity activity, Configuration newConfig, StandardGSYVideoPlayer mVideoPlayer) {
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
                if (!mVideoPlayer.isIfCurrentIsFullscreen()) {
                    mVideoPlayer.startWindowFullscreen(activity, true, true);
                }
            } else {
                //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
                if (mVideoPlayer.isIfCurrentIsFullscreen()) {
                    StandardGSYVideoPlayer.backFromWindowFull(activity);
                }
            }
        }
    }

    @Override
    public void onBackPressed(Activity activity) {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }

        if (StandardGSYVideoPlayer.backFromWindowFull(activity)) {
            return;
        }
        activity.finish();
    }

    @Override
    public void onPause() {
        isPause = true;
        GSYVideoManager.onPause();
    }

    @Override
    public void onResume() {
        isPause = false;
        GSYVideoManager.onResume();
    }

    @Override
    public void onDestory() {
        GSYVideoPlayer.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }
}
