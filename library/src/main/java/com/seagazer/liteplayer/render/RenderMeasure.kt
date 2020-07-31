package com.seagazer.liteplayer.render

import android.view.View.MeasureSpec
import android.view.View.getDefaultSize
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.helper.MediaLogger

/**
 * Helper to make measure for render view when aspectRatio changed.
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
class RenderMeasure {

    private var aspectRatio = AspectRatio.AUTO
    private var videoWidth = 0
    private var videoHeight = 0
    private var measureWidth = 0
    private var measureHeight = 0

    fun doRenderMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultWidth = getDefaultSize(videoWidth, widthMeasureSpec)
        val defaultHeight = getDefaultSize(videoHeight, heightMeasureSpec)

        MediaLogger.d("video size： $videoWidth $videoHeight")
        MediaLogger.d("render view default size： $defaultWidth $defaultHeight")

        if (videoWidth > 0 && videoHeight > 0) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
            MediaLogger.d("render view measure size： $widthSpecSize $heightSpecSize")

            val viewRatio = heightSpecSize * 1f / widthSpecSize// 视图 高/宽比
            val videoRatio = videoHeight * 1f / videoWidth// 视频 高/宽比
            MediaLogger.d("render aspectRatio： $viewRatio")
            MediaLogger.d("video aspectRatio： $videoRatio")

            if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                if (aspectRatio == AspectRatio.ORIGIN || aspectRatio == AspectRatio.AUTO) {
                    // 自适应
                    if (videoRatio > viewRatio) {
                        measureHeight = heightSpecSize
                        //heightSize / videoHeight = measureWidth / videoWidth
                        measureWidth = (heightSpecSize / videoRatio).toInt()
                    } else {
                        measureWidth = widthSpecSize
                        //widthSize / videoWidth = measureHeight / videoHeight
                        measureHeight = (widthSpecSize * videoRatio).toInt()
                    }
                    // 原始尺寸，最大为视图边界
                    if (aspectRatio == AspectRatio.ORIGIN) {
                        val widthSpecRatio = videoWidth * 1f / measureWidth
                        val heightSpecRatio = videoHeight * 1f / measureHeight
                        val specRatio = widthSpecRatio.coerceAtMost(heightSpecRatio)
                        if (specRatio < 1) {
                            measureWidth = (measureWidth * widthSpecRatio).toInt()
                            measureHeight = (measureHeight * heightSpecRatio).toInt()
                        }
                    }
                } else if (aspectRatio == AspectRatio.STRETCH) {// 拉伸模式
                    measureWidth = widthSpecSize
                    measureHeight = heightSpecSize
                } else if (aspectRatio == AspectRatio.FILL) {// 填充模式
                    if (videoRatio < viewRatio) {
                        // fill h
                        measureHeight = heightSpecSize
                        measureWidth = (heightSpecSize / videoRatio).toInt()
                    } else {
                        // fill w
                        measureWidth = widthSpecSize
                        measureHeight = (widthSpecSize * videoRatio).toInt()
                    }
                } else if (aspectRatio == AspectRatio.W_16_9) {// 16:9宽屏比例
                    measureWidth = widthSpecSize
                    measureHeight = (widthSpecSize * 1f / 16 * 9).toInt()
                } else if (aspectRatio == AspectRatio.W_21_9) {// 21:9电影比例
                    measureWidth = widthSpecSize
                    measureHeight = (widthSpecSize * 1f / 21 * 9).toInt()
                } else if (aspectRatio == AspectRatio.W_4_3) {// 4:3电视比例
                    measureWidth = widthSpecSize
                    measureHeight = (widthSpecSize * 1f / 4 * 3).toInt()
                }
            } else if (widthMode == MeasureSpec.EXACTLY) {// 宽确定，保持比例
                measureWidth = widthSpecSize
                measureHeight = (widthSpecSize * videoRatio).toInt()
                if (heightMode == MeasureSpec.AT_MOST && measureHeight > heightSpecSize) {// 视图高不确定，且比例换算的高 > 视图测量高度
                    measureHeight = heightSpecSize// 取小
                }
            } else if (heightMode == MeasureSpec.EXACTLY) {// 高确定，保持比例
                measureHeight = heightSpecSize
                measureWidth = (heightSpecSize / videoRatio).toInt()
                if (widthMode == MeasureSpec.AT_MOST && measureWidth > widthSpecSize) {// 视图宽不确定，且比例换算的宽 > 视图测量宽度
                    measureWidth = widthSpecSize// 取小
                }
            } else {// 宽高都不确定，按视频比例换算，上限为视图测量宽高
                measureWidth = videoWidth
                measureHeight = videoHeight
                if (heightMode == MeasureSpec.AT_MOST && measureHeight > heightSpecSize) {
                    measureHeight = heightSpecSize
                    measureWidth = (heightSpecSize / videoRatio).toInt()
                }
                if (widthMode == MeasureSpec.AT_MOST && measureWidth > widthSpecSize) {
                    measureWidth = widthSpecSize
                    measureHeight = (widthSpecSize * viewRatio).toInt()
                }
            }
        } else {
            measureWidth = defaultWidth
            measureHeight = defaultHeight
        }
        MediaLogger.d("measure result: $measureWidth $measureHeight")
    }

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        this.aspectRatio = aspectRatio
    }

    fun getMeasureWidth() = measureWidth

    fun getMeasureHeight() = measureHeight


}