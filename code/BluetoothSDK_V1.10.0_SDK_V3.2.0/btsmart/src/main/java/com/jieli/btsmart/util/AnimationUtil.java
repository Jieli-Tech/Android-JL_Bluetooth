package com.jieli.btsmart.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.jieli.bluetooth.utils.JL_Log;


/**
 * @ClassName: AnimationUtil
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2020/9/23 14:56
 */
public class AnimationUtil {
    private static final String TAG = AnimationUtil.class.getSimpleName();

    public static void parabolaAnimation(ViewGroup parentParentView, View srcView, View targetView, ImageView animView, DynamicAnimation.OnAnimationEndListener endListener) {
        parabolaAnimation(parentParentView, srcView, targetView, animView, null, null, null, endListener);
    }

    /**
     * View的抛物线动画
     *
     * @param parentParentView 必须包含起始view和目标view的ViewGroup
     * @param srcView          起始动画的view
     * @param targetView       目标view
     * @param animView         负责动画的View，完全复制srcView
     * @param frictionF        抛物线动画的摩擦力而不是伸缩动画的摩擦力(DeFault:04f)
     * @param maxScaleF        弹性动画的最大值Scale（注意MaxScale要大于最终的停下来的Scale，也要大于MinScale，而且要大于StartScale）
     * @param startFlingScaleF 抛物线动画的起始Scale（注意变化是从srcView的当前scale-》MaxSCale-》targetView的Scale），当Scale变化到该值就会开始抛物线动画
     * @param endListener      抛物线动画结束回调
     * @return 无
     * @description 把一个view从当前位置抛到目标view位置，其中使用了一个伸缩动画，当伸缩动画变化到一个scale才会进行一个X轴的和一个y轴的Fling动画。
     * 视srcView的大小为scale 等于1
     * 注：目前只有 由大变小： 有弹簧动画，有抛物动画
     *              由小变大： 无弹簧动画，有抛物动画
     *              scale没有改变： 无弹簧动画，有抛物动画
     */
    public static void parabolaAnimation(ViewGroup parentParentView, View srcView, View targetView, ImageView animView, Float frictionF, Float maxScaleF, Float startFlingScaleF, DynamicAnimation.OnAnimationEndListener endListener) {
        float friction = 0.4f;
        float startFlingScale = 1f;
        if (null != frictionF) {
            friction = frictionF;
        }
        if (null != startFlingScaleF) {
            startFlingScale = startFlingScaleF;
        }
        float scaleWidth = (float) targetView.getWidth() / (float) srcView.getWidth();//最终View和起始view的宽高大小倍数
        float scaleHeight = (float) targetView.getHeight() / (float) srcView.getHeight();
        float scale = Math.min(scaleHeight, scaleWidth);
        float maxScale;
        if (scale > 1) {//尺寸变大
            maxScale = scale + 0.2f;
        } else if (scale == 1) {//尺寸不变
            maxScale = 1.2f;
        } else {//尺寸变小
            maxScale = 1.2f;
        }
        if (null != maxScaleF) {
            maxScale = maxScaleF;
        }
        int scaleWidthDValue = srcView.getWidth() - targetView.getWidth();//起始view和最终View的宽高差值
        int scaleHeightDValue = srcView.getHeight() - targetView.getHeight();
        int[] srcViewLocation = new int[2];//起始view的坐标
        final boolean[] isCallbackOnce = {true};//只回调一次
        final boolean[] isShowOnce = {true};//抛物线动画只运行一次
        int[] targetViewLocation = new int[2];//目标view的坐标
        targetView.getLocationInWindow(targetViewLocation);
        srcView.getLocationInWindow(srcViewLocation);
        int startXPosition = srcViewLocation[0];
        int startYPosition = srcViewLocation[1];
        int endXPosition = targetViewLocation[0];
        int endYPosition = targetViewLocation[1];
        int[] parentViewLocation = new int[2];//容器ViewGroup的坐标
        int parentParentViewPaddingStart = parentParentView.getPaddingStart();
        int parentParentViewPaddingTop = parentParentView.getPaddingTop();
        parentParentView.getLocationInWindow(parentViewLocation);
        if ((startYPosition - parentViewLocation[1]) < 0) {//如果起始view的位置超过容器的位置（Y），就只能从容器的位置开始
            startYPosition = parentViewLocation[1];
            JL_Log.d(TAG, "startYPosition - parentViewLocation[1]) < 0");
        }
        if ((startXPosition - parentViewLocation[0]) < 0) {
            startXPosition = parentViewLocation[0];
            JL_Log.d(TAG, "(startXPosition - parentViewLocation[0]) < 0");
        }
//        ImageView imageView = new ImageView(context);
//        Glide.with(this.getContext()).load(url).transform(new CenterInside(), new GlideRoundTransform(7)).error(R.drawable.ic_radio_placeholder).into(imageView);
        if (parentParentView instanceof ConstraintLayout) {
            JL_Log.d(TAG, "parentParentView instanceof ConstraintLayout");
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(srcView.getWidth(), srcView.getHeight());
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            layoutParams.setMargins(startXPosition - parentViewLocation[0] - parentParentViewPaddingStart, startYPosition - parentViewLocation[1] - parentParentViewPaddingTop, 0, 0);
            parentParentView.addView(animView, layoutParams);
        } else if (parentParentView instanceof RelativeLayout) {
            JL_Log.d(TAG, "parentParentView instanceof RelativeLayout");
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(srcView.getWidth(), srcView.getHeight());
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            layoutParams.setMargins(startXPosition - parentViewLocation[0] - parentParentViewPaddingStart, startYPosition - parentViewLocation[1] - parentParentViewPaddingTop, 0, 0);
            parentParentView.addView(animView, layoutParams);
        } else {
            JL_Log.d(TAG, "parentParentView instanceof OtherLayout");
            ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(srcView.getWidth(), srcView.getHeight());
            layoutParams.setMargins(startXPosition - parentViewLocation[0] - parentParentViewPaddingStart, startYPosition - parentViewLocation[1] - parentParentViewPaddingTop, 0, 0);
            parentParentView.addView(animView, layoutParams);
        }
        //缩放动画;
        FloatPropertyCompat<View> scaleProperty = new FloatPropertyCompat<View>("scale") {
            @Override
            public float getValue(View object) {
                return object.getScaleX();
            }

            @Override
            public void setValue(View object, float value) {
                object.setScaleX(value);
                object.setScaleY(value);
            }
        };
        SpringAnimation stretchAnimation =
                new SpringAnimation(animView, scaleProperty);
        stretchAnimation.setMaxValue(/*1.1f*//*1 / scale*/maxScale);//弹簧动画设置最大尺寸，
        stretchAnimation.setMinimumVisibleChange(
                DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        SpringForce force = new SpringForce(scale);
        force.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)//阻尼比
                .setStiffness(SpringForce.STIFFNESS_VERY_LOW);//刚度

        stretchAnimation.setSpring(force)//将弹簧设置为动画效果
                .setStartVelocity(10);//起始速度(越小速度越快)
        int finalStartXPosition = startXPosition;
        int finalStartYPosition = startYPosition;
        float finalFriction = friction;
        float finalStartFlingScale = startFlingScale;
        stretchAnimation.addUpdateListener((animation, value, velocity) -> {
            boolean isStartFlingAnim;
            if (scale > 1) {//尺寸变大
                isStartFlingAnim = value > finalStartFlingScale;
            } else if (scale == 1) {//尺寸不变
                isStartFlingAnim = true;
            } else {//尺寸变小
                isStartFlingAnim = value < finalStartFlingScale;
            }

            if (isStartFlingAnim/*value < 1f*/ && isShowOnce[0]) {
                animation.setStartVelocity(1);
                isShowOnce[0] = false;
                //抛物线的X轴动画
                //抛物线的Y轴动画
                final boolean[] animationXEnd = {false};
                final boolean[] animationYEnd = {false};
                int startVelocityX;
                int minValueX = 0;
                int maxValueX = 0;
                int startVelocityY;
                int minValueY = 0;
                int maxValueY = 0;
                int dValueX = endXPosition - (finalStartXPosition + scaleWidthDValue / 2);
                int baseVelocityX = 0;
                int baseVelocityY = 0;
                if (dValueX > 0) {
                    maxValueX = dValueX;
                    startVelocityX = 3 * dValueX + baseVelocityX;
                } else {
                    minValueX = dValueX;
                    startVelocityX = 3 * dValueX - baseVelocityX;
                }
                int dValueY = endYPosition - (finalStartYPosition + scaleHeightDValue / 2);
                if (dValueY > 0) {
                    maxValueY = dValueY;
                    startVelocityY = 3 * dValueY + baseVelocityY;
                } else {
                    minValueY = dValueY;
                    startVelocityY = 3 * dValueY - baseVelocityY;
                }
                FlingAnimation flingAnimationX = new FlingAnimation(animView, DynamicAnimation.TRANSLATION_X);
                flingAnimationX.setStartVelocity(startVelocityX)
                        .setMinValue(minValueX)
                        .setMaxValue(maxValueX)
                        .setFriction(finalFriction)
                        .addEndListener((animation12, canceled, value12, velocity12) -> {
                            if (isCallbackOnce[0]) {
                                isCallbackOnce[0] = false;
                                endListener.onAnimationEnd(animation12, canceled, value12, velocity12);
                            }
                            if (dValueX == dValueY || animationYEnd[0]) {
                                parentParentView.removeView(animView);
                            }
                            animationXEnd[0] = true;
                        })
                        .start();
                FlingAnimation flingAnimationY = new FlingAnimation(animView, DynamicAnimation.TRANSLATION_Y);
                flingAnimationY.setStartVelocity(startVelocityY)
                        .setMinValue(minValueY)
                        .setMaxValue(maxValueY)
                        .setFriction(finalFriction)
                        .addEndListener((animation1, canceled, value1, velocity1) -> {
                            if (isCallbackOnce[0]) {
                                isCallbackOnce[0] = false;
                                endListener.onAnimationEnd(animation1, canceled, value1, velocity1);
                            }
                            if (dValueX == dValueY || animationXEnd[0]) {
                                parentParentView.removeView(animView);
                            }
                            animationYEnd[0] = true;
                        })
                        .start();
            }
        });
        stretchAnimation.start();//开始动画
    }

}
