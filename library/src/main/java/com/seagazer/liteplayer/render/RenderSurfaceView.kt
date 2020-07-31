package com.seagazer.liteplayer.render

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
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
 * Date: 2020/6/19
 */
class RenderSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, IRender {

    private var renderMeasure: RenderMeasure? = null
    private var holderReference: WeakReference<SurfaceHolder>? = null
    private var liveData: MutableLiveData<RenderStateEvent>? = null
    private var firstAttach = true
    private var aspectRatio = AspectRatio.AUTO

    init {
        MediaLogger.d("surface view init")
        holder.addCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        MediaLogger.d("surface view do measure")
        renderMeasure?.let {
            it.doRenderMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(it.getMeasureWidth(), it.getMeasureHeight())
        }
    }

    override fun getRenderView() = this

    override fun updateVideoSize(videoWidth: Int, videoHeight: Int) {
        MediaLogger.d("surface view  update size")
        renderMeasure?.let {
            it.setVideoSize(videoWidth, videoHeight)
            // fix the surfaceView is flash when first attach view system at a short time to requestLayout again.
            if (firstAttach) {
                postDelayed({
                    MediaLogger.d("first attach, delay 500ms to update layout")
                    requestLayout()
                }, 500)
            } else {
                MediaLogger.d("normal attach")
                requestLayout()
            }
            firstAttach = false
        }
    }

    override fun updateAspectRatio(aspectRatio: AspectRatio) {
        MediaLogger.d("surface view update aspect ratio")
        renderMeasure?.let {
            if (this.aspectRatio != aspectRatio) {
                it.setAspectRatio(aspectRatio)
                requestLayout()
            }
        }
    }

    override fun bindPlayer(iPlayer: IPlayer) {
        holderReference?.let {
            it.get()?.let { holder ->
                iPlayer.setSurfaceHolder(holder)
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
        holderReference?.clear()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        MediaLogger.d("surface create")
        holderReference = WeakReference(holder!!)
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_CREATED)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        MediaLogger.d("surface changed： $width * $height")
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_CHANGED, width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        MediaLogger.d("surface destroy")
        liveData?.run {
            value = RenderStateEvent(RenderState.STATE_SURFACE_DESTROYED)
        }
    }

}