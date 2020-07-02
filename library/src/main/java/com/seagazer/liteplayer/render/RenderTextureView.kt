package com.seagazer.liteplayer.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.RenderState
import com.seagazer.liteplayer.event.RenderStateEvent
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

    init {
        MediaLogger.d("texture view init")
        surfaceTextureListener = this
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        MediaLogger.d("texture view do measure")
        renderMeasure?.let {
            it.doRenderMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(it.getMeasureWidth(), it.getMeasureHeight())
        }
    }

    override fun getRenderView() = this

    override fun updateVideoSize(videoWidth: Int, videoHeight: Int) {
        MediaLogger.d("texture view update size")
        renderMeasure?.let {
            it.setVideoSize(videoWidth, videoHeight)
            requestLayout()
        }
    }

    override fun updateAspectRatio(aspectRatio: AspectRatio) {
        MediaLogger.d("texture view update aspect ratio")
        renderMeasure?.let {
            it.setAspectRatio(aspectRatio)
            requestLayout()
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
        this.liveData = null
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