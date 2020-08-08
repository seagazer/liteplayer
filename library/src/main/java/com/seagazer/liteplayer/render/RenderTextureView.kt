package com.seagazer.liteplayer.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.RenderState
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.player.IPlayer
import java.lang.ref.WeakReference

/**
 *
 * Author: Seagazer
 * Date: 2020/6/28
 */
class RenderTextureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener, IRender {

    private var renderMeasure: RenderMeasure? = null
    private var surfaceReference: WeakReference<SurfaceTexture>? = null
    private var liveData: MutableLiveData<RenderStateEvent>? = null
    private var aspectRatio = AspectRatio.AUTO
    private var lastVideoWidth = -1
    private var lastVideoHeight = -1
    private var shouldReLayout = false
    private var lastWidthMeasureSpec = -1
    private var lastHeightMeasureSpec = -1

    init {
        surfaceTextureListener = this
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (shouldReLayout ||
            (lastWidthMeasureSpec != -1 && lastWidthMeasureSpec != widthMeasureSpec ||
                    lastHeightMeasureSpec != -1 && lastHeightMeasureSpec != heightMeasureSpec)
        ) {
            renderMeasure?.let {
                it.doRenderMeasure(widthMeasureSpec, heightMeasureSpec)
                setMeasuredDimension(it.getMeasureWidth(), it.getMeasureHeight())
                shouldReLayout = false
                lastWidthMeasureSpec = widthMeasureSpec
                lastHeightMeasureSpec = heightMeasureSpec
            }
        } else {
            // when different dataSource has the same video size, we should reset last measureSize again
            renderMeasure?.let {
                if (it.getMeasureWidth() != 0 && it.getMeasureHeight() != 0) {
                    setMeasuredDimension(it.getMeasureWidth(), it.getMeasureHeight())
                }
            }
        }
    }

    override fun getRenderView() = this

    override fun updateVideoSize(videoWidth: Int, videoHeight: Int) {
        if (!shouldReLayout && lastVideoWidth != -1 && lastVideoWidth == videoWidth && lastVideoHeight != -1 && lastVideoHeight == videoHeight) {
            // the same video size, not measure and layout again
            shouldReLayout = false
        } else {
            shouldReLayout = true
            lastVideoWidth = videoWidth
            lastVideoHeight = videoHeight
            renderMeasure?.let {
                it.setVideoSize(videoWidth, videoHeight)
                requestLayout()
            }
        }
    }

    override fun updateAspectRatio(aspectRatio: AspectRatio) {
        renderMeasure?.let {
            shouldReLayout = true
            if (this.aspectRatio != aspectRatio) {
                this.aspectRatio = aspectRatio
                it.setAspectRatio(aspectRatio)
                requestLayout()
            }
        }
    }

    override fun bindPlayer(iPlayer: IPlayer) {
        surfaceReference?.let {
            it.get()?.let { surfaceTexture ->
                iPlayer.setSurface(Surface(surfaceTexture))
            }
        }
    }

    override fun bindRenderMeasure(renderMeasure: RenderMeasure) {
        this.renderMeasure = renderMeasure
    }

    override fun registerStateObserver(liveData: MutableLiveData<RenderStateEvent>) {
        if (this.liveData != liveData) {
            this.liveData = liveData
        }
    }

    override fun release() {
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_DESTROYED)
        }
        this.liveData = null
        surfaceReference?.get()?.release()
        surfaceReference?.clear()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        MediaLogger.d("texture changed： $width * $height")
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_CHANGED, width, height)
        }
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        MediaLogger.d("texture destroy")
        shouldReLayout = true
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_DESTROYED)
        }
        return false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        MediaLogger.d("texture create")
        surfaceReference = WeakReference(surface!!)
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_CREATED)
        }
    }


}