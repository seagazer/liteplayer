package com.seagazer.liteplayer.list

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.widget.IPlayerView

/**
 * A helper to play media list for recyclerView.
 *
 * Author: Seagazer
 * Date: 2020/6/29
 */
class ListPlayer constructor(val playerView: LitePlayerView) : IPlayerView by playerView, LifecycleObserver {
    companion object {
        private const val MSG_ATTACH_CONTAINER = 0x11
        private const val ATTACH_DELAY = 500L
    }

    private var playingPosition = 0
    private var recyclerView: RecyclerView? = null
    private var layoutManager: LinearLayoutManager? = null
    private lateinit var listener: VideoListScrollListener
    private var autoPlay = true
    private var playerType = PlayerType.TYPE_EXO_PLAYER
    private var renderType = RenderType.TYPE_SURFACE_VIEW
    private var aspectRatio = AspectRatio.AUTO
    private val playerHistoryCache by lazy {
        hashMapOf<Int, Long>()
    }
    var listItemChangedListener: ListItemChangedListener? = null
    var isPlayableWhenScrollIdle = true
    private var isScrollChanged = false

    /**
     * Support auto cache last play position or not.
     */
    var supportHistory = false

    /**
     * Attach to recyclerView.
     *
     * @param recyclerView The recyclerView to attached.
     * @param autoPlay True auto play video when scroll changed, false you should click item to start play.
     * @param listener The video list scroll listener.
     */
    fun attachToRecyclerView(recyclerView: RecyclerView, autoPlay: Boolean, listener: VideoListScrollListener) {
        this.autoPlay = autoPlay
        if (autoPlay) {
            attachToRecyclerViewAutoPlay(recyclerView, listener)
        } else {
            attachToRecyclerViewClickPlay(recyclerView, listener)
        }
    }

    /**
     * Change the play logic.
     *
     * @param autoPlay True auto play video when scroll changed, false you should click item to start play.
     */
    fun setAutoPlayMode(autoPlay: Boolean) {
        if (this.autoPlay != autoPlay) {
            this.autoPlay = autoPlay
            if (recyclerView == null) {
                throw IllegalStateException("You must call attachToRecyclerView first")
            }
            recyclerView!!.run {
                removeOnScrollListener(autoPlayScrollListener)
                removeOnScrollListener(clickPlayScrollListener)
                detachVideoContainer()
                if (autoPlay) {
                    attachToRecyclerViewAutoPlay(this, listener)
                } else {
                    attachToRecyclerViewClickPlay(this, listener)
                }
            }
        }
    }

    private fun attachToRecyclerViewAutoPlay(recyclerView: RecyclerView, listener: VideoListScrollListener) {
        if (recyclerView.layoutManager !is LinearLayoutManager) {
            throw RuntimeException("Only support LinearLayoutManager because always single item video is playing")
        }
        // default config
        initConfig()
        this.layoutManager = recyclerView.layoutManager as LinearLayoutManager
        this.recyclerView = recyclerView
        this.recyclerView!!.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                detachVideoContainer()
                val container = listener.getVideoContainer(playingPosition)
                val dataSource = listener.getVideoDataSource(playingPosition)
                if (container == null || dataSource == null) {
                    MediaLogger.w("Attach container is null or current dataSource is null!")
                    return
                }
                container.addView(playerView)
                listItemChangedListener?.onAttachItemView(playingPosition)
                dataSource.let {
                    playerView.setDataSource(it)
                    playerView.start()
                }
                this@ListPlayer.recyclerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        this.recyclerView!!.addOnScrollListener(autoPlayScrollListener)
        this.listener = listener
        unregisterLifecycle()
        registerLifecycle()
    }

    private fun attachToRecyclerViewClickPlay(recyclerView: RecyclerView, listener: VideoListScrollListener) {
        if (recyclerView.layoutManager !is LinearLayoutManager) {
            throw RuntimeException("Only support LinearLayoutManager because always single item video is playing")
        }
        // default config
        initConfig()
        this.layoutManager = recyclerView.layoutManager as LinearLayoutManager
        this.recyclerView = recyclerView
        this.recyclerView!!.addOnScrollListener(clickPlayScrollListener)
        this.listener = listener
        unregisterLifecycle()
        registerLifecycle()
    }

    private fun initConfig() {
        playerView.setPlayerType(playerType)
        playerView.setRenderType(renderType)
        playerView.setAspectRatio(aspectRatio)
    }

    override fun setPlayerType(playerType: PlayerType) {
        this.playerType = playerType
    }

    override fun setRenderType(renderType: RenderType) {
        this.renderType = renderType
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        this.aspectRatio = aspectRatio
    }

    /**
     * If set the autoPlay false, call this method to play when click the item of recyclerView.
     *
     * @param position Click adapter position.
     */
    fun onItemClick(position: Int) {
        detachVideoContainer()
        val container = listener.getVideoContainer(position)
        val dataSource = listener.getVideoDataSource(position)
        if (container == null || dataSource == null) {
            MediaLogger.w("Attach container is null or current dataSource is null!")
            return
        }
        container.addView(playerView)
        listItemChangedListener?.onAttachItemView(position)
        dataSource.let {
            playerView.setDataSource(it)
            if (supportHistory && playerHistoryCache[position] != null) {
                MediaLogger.i("resume progress: [$position - ${playerHistoryCache[position]}]")
                playerView.start(playerHistoryCache[position]!!)
            } else {
                playerView.start()
            }
        }
        if (position != RecyclerView.NO_POSITION) {
            playingPosition = position
        }
    }

    @SuppressLint("HandlerLeak")
    private val attachHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_ATTACH_CONTAINER) {
                val currentFirst: Int = msg.obj as Int
                val container = listener.getVideoContainer(currentFirst)
                val dataSource = listener.getVideoDataSource(currentFirst)
                if (container == null || dataSource == null) {
                    MediaLogger.w("Attach container is null or current dataSource is null!")
                    return
                }
                // try detach again
                detachVideoContainer()
                container.addView(playerView)
                listItemChangedListener?.onAttachItemView(currentFirst)
                dataSource.let {
                    playerView.setDataSource(it)
                    val history = playerHistoryCache[currentFirst]
                    if (supportHistory && history != null) {
                        MediaLogger.i("resume progress: [$currentFirst - $history]")
                        playerView.start(history)
                    } else {
                        playerView.start()
                    }
                    isScrollChanged = false
                }
            }
        }
    }

    private val autoPlayScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            layoutManager?.let { lm ->
                val currentFirst = lm.findFirstCompletelyVisibleItemPosition()
                if (currentFirst != RecyclerView.NO_POSITION && playingPosition != currentFirst) {
                    isScrollChanged = true
                    detachVideoContainer()
                    // 当前第一个不等于上次播放的index，播放当前第一个
                    if (playingPosition != currentFirst && !isPlayableWhenScrollIdle) {
                        attachHandler.removeMessages(MSG_ATTACH_CONTAINER)
                        attachHandler.sendMessageDelayed(
                            attachHandler.obtainMessage(MSG_ATTACH_CONTAINER, currentFirst),
                            ATTACH_DELAY
                        )
                        if (currentFirst != RecyclerView.NO_POSITION) {
                            playingPosition = currentFirst
                        }
                    }
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (isPlayableWhenScrollIdle && newState == RecyclerView.SCROLL_STATE_IDLE) {
                layoutManager?.let { lm ->
                    val currentFirst = lm.findFirstCompletelyVisibleItemPosition()
                    // 当前第一个不等于上次播放的index，播放当前第一个
                    if (currentFirst != RecyclerView.NO_POSITION && (playingPosition != currentFirst || isScrollChanged)) {
                        attachHandler.removeMessages(MSG_ATTACH_CONTAINER)
                        attachHandler.sendMessageDelayed(
                            attachHandler.obtainMessage(MSG_ATTACH_CONTAINER, currentFirst),
                            ATTACH_DELAY
                        )
                    }
                    if (currentFirst != RecyclerView.NO_POSITION) {
                        playingPosition = currentFirst
                    }
                }
            }
        }
    }

    private val clickPlayScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            layoutManager?.let { lm ->
                val currentFirst = lm.findFirstVisibleItemPosition()
                val currentLast = lm.findLastVisibleItemPosition()
                if (currentFirst != RecyclerView.NO_POSITION && currentLast != RecyclerView.NO_POSITION &&
                    currentFirst != currentLast &&
                    (playingPosition < currentFirst || playingPosition > currentLast)
                ) {
                    detachVideoContainer()
                }
            }
        }
    }

    private fun detachVideoContainer() {
        if (playerView.parent != null) {
            val parent: ViewGroup = playerView.parent as ViewGroup
            if (supportHistory) {
                val history = playerView.getCurrentPosition()
                playerHistoryCache[playingPosition] = history
                MediaLogger.i("cache progress: [$playingPosition - $history]")
            }
            playerView.stop()
            parent.removeView(playerView)
            listItemChangedListener?.onDetachItemView(playingPosition)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        detachRecyclerView()
    }

    private fun registerLifecycle() {
        if (recyclerView!!.context is FragmentActivity) {
            MediaLogger.d("register lifecycle")
            (recyclerView!!.context as FragmentActivity).lifecycle.addObserver(this)
        }
    }

    private fun unregisterLifecycle() {
        if (recyclerView!!.context is FragmentActivity) {
            MediaLogger.d("unregister lifecycle")
            (recyclerView?.context as FragmentActivity).lifecycle.removeObserver(this)
        }
    }

    /**
     * You should call this method if use in original Activity when the activity onDestroy or fragment when onDestroyView.
     * If use in sub class of FragmentActivity, it can call this method automatic when activity onDestroy.
     */
    fun detachRecyclerView() {
        attachHandler.removeCallbacksAndMessages(null)
        unregisterLifecycle()
        recyclerView?.removeOnScrollListener(autoPlayScrollListener)
        recyclerView?.removeOnScrollListener(clickPlayScrollListener)
        playerView.stop()
        playerView.destroy()
    }

    interface VideoListScrollListener {
        /**
         * Return a container to hold the player view.
         * @param position Current adapter position of recyclerView.
         */
        fun getVideoContainer(position: Int): ViewGroup?

        /**
         * Return a data source to play.
         * @param position Current adapter position.
         */
        fun getVideoDataSource(position: Int): DataSource?
    }

}