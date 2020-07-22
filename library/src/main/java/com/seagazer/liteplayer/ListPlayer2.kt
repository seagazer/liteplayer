package com.seagazer.liteplayer

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.ListView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.widget.IPlayerView

/**
 * A helper to play media list for listView.
 *
 * Author: Seagazer
 * Date: 2020/7/16
 */
class ListPlayer2 constructor(val playerView: LitePlayerView) : IPlayerView by playerView, LifecycleObserver {
    companion object {
        private const val MSG_ATTACH_CONTAINER = 0x11
        private const val ATTACH_DELAY = 200L
    }

    private var playingPosition = 0
    private var listView: ListView? = null
    private lateinit var listener: VideoListScrollListener
    private var autoPlay = true
    private var playerType = PlayerType.TYPE_EXO_PLAYER
    private var renderType = RenderType.TYPE_SURFACE_VIEW

    fun attachToListView(listView: ListView, autoPlay: Boolean, listener: VideoListScrollListener) {
        this.autoPlay = autoPlay
        if (autoPlay) {
            attachToListViewAutoPlay(listView, listener)
        } else {
            attachToListViewClickPlay(listView, listener)
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
            if (listView == null) {
                throw IllegalStateException("You must call attachToListView first")
            }
            listView!!.run {
                setOnScrollListener(null)
                detachVideoContainer()
                if (autoPlay) {
                    attachToListViewAutoPlay(this, listener)
                } else {
                    attachToListViewClickPlay(this, listener)
                }
            }
        }
    }

    private fun attachToListViewAutoPlay(listView: ListView, listener: VideoListScrollListener) {
        // default config
        initConfig()
        this.listView = listView
        this.listView!!.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                detachVideoContainer()
                val container = listener.getVideoContainer(0, playingPosition)
                container?.addView(playerView)
                MediaLogger.d("attach container: $container")
                val dataSource = listener.getVideoDataSource(playingPosition)
                dataSource?.let {
                    MediaLogger.d("start play: $it")
                    playerView.setDataSource(it)
                    playerView.start()
                }
                this@ListPlayer2.listView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        this.listView!!.setOnScrollListener(autoPlayScrollListener)
        this.listener = listener
        unregisterLifecycle()
        registerLifecycle()
    }

    private fun attachToListViewClickPlay(listView: ListView, listener: VideoListScrollListener) {
        // default config
        initConfig()
        this.listView = listView
        this.listView!!.setOnScrollListener(clickPlayScrollListener)
        this.listener = listener
        unregisterLifecycle()
        registerLifecycle()
    }

    private fun initConfig() {
        playerView.setPlayerType(playerType)
        playerView.setRenderType(renderType)
    }

    /**
     * If set the autoPlay false, call this method to play when click the item of listView.
     *
     * @param position Click adapter position.
     */
    fun onItemClick(position: Int) {
        detachVideoContainer()
        val firstVisiblePosition = listView!!.firstVisiblePosition
        var childIndex = position - firstVisiblePosition
        if (childIndex < 0) {
            childIndex = 0
        }
        MediaLogger.d("aaa click=$position, first=$firstVisiblePosition")
        val container = listener.getVideoContainer(childIndex, position)
        container?.addView(playerView)
        MediaLogger.d("attach container: $container")
        val dataSource = listener.getVideoDataSource(position)
        dataSource?.let {
            playerView.setDataSource(it)
            playerView.start()
        }
        playingPosition = position
    }

    @SuppressLint("HandlerLeak")
    private val attachHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_ATTACH_CONTAINER) {
                val childIndex: Int = msg.arg1
                val completedVisibleFirst: Int = msg.arg2
                val container = listener.getVideoContainer(childIndex, completedVisibleFirst)
                detachVideoContainer()
                container?.addView(playerView)
                MediaLogger.d("attach container: $container, currentFirst: $completedVisibleFirst, childIndex: $childIndex")
                val dataSource = listener.getVideoDataSource(completedVisibleFirst)
                dataSource?.let {
                    MediaLogger.d("start play: $it")
                    playerView.setDataSource(it)
                    playerView.start()
                }
            }
        }
    }

    private val autoPlayScrollListener = object : AbsListView.OnScrollListener {
        override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            // find first completed visible
            val firstChild = view!!.getChildAt(0)
            var firstPosition = firstVisibleItem
            if (firstChild == null) {
                return
            }
            if (firstChild.top < 0) {
                firstPosition += 1
            }
            // 当前第一个不等于上次播放的index，先释放，滑动停止再开始播放
            if (playingPosition != firstPosition) {
                detachVideoContainer()
            }
        }

        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
            // listView has some bug that onScroll sometime may call always ,so we start play when scroll stop
            if (scrollState == SCROLL_STATE_IDLE) {
                val firstChild = view!!.getChildAt(0)
                val firstVisibleItem = view.firstVisiblePosition
                var firstPosition = firstVisibleItem
                MediaLogger.d("firstChild=>$firstChild}")
                if (firstChild == null) {
                    return
                }
                if (firstChild.top < 0) {
                    firstPosition += 1
                }
                if (playingPosition != firstPosition) {
                    MediaLogger.d("position: playing=$playingPosition, first=$firstPosition")
                    attachHandler.removeMessages(MSG_ATTACH_CONTAINER)
                    val message = attachHandler.obtainMessage(MSG_ATTACH_CONTAINER)
                    var childIndex = 0
                    if (firstPosition > firstVisibleItem) {
                        childIndex = 1
                    }
                    message.arg1 = childIndex
                    message.arg2 = firstPosition
                    attachHandler.sendMessageDelayed(message, ATTACH_DELAY)
                    playingPosition = firstPosition
                }
            }
        }
    }

    private val clickPlayScrollListener = object : AbsListView.OnScrollListener {
        override fun onScroll(view: AbsListView?, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            val lastPosition = firstVisibleItem + visibleItemCount - 1
            if (firstVisibleItem != lastPosition && playingPosition < firstVisibleItem || playingPosition > lastPosition) {
                detachVideoContainer()
            }
        }

        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
        }
    }

    private fun detachVideoContainer() {
        if (playerView.parent != null) {
            val parent: ViewGroup = playerView.parent as ViewGroup
            MediaLogger.d("detach container: $parent")
            playerView.stop()
            parent.removeView(playerView)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        detachListView()
    }

    private fun registerLifecycle() {
        if (listView!!.context is FragmentActivity) {
            MediaLogger.d("register lifecycle")
            (listView!!.context as FragmentActivity).lifecycle.addObserver(this)
        }
    }

    private fun unregisterLifecycle() {
        if (listView!!.context is FragmentActivity) {
            MediaLogger.d("unregister lifecycle")
            (listView?.context as FragmentActivity).lifecycle.removeObserver(this)
        }
    }

    /**
     * You should call this method if use in original Activity when the activity onDestroy or fragment when onDestroyView.
     * If use in sub class of FragmentActivity, it can call this method automatic when activity onDestroy.
     */
    fun detachListView() {
        attachHandler.removeCallbacksAndMessages(null)
        unregisterLifecycle()
        listView?.setOnScrollListener(null)
        playerView.stop()
        playerView.destroy()
    }

    interface VideoListScrollListener {
        /**
         * Return a container to hold the player view.
         * @param childIndex Current index of child of listView.
         * @param position Current adapter position of listView.
         */
        fun getVideoContainer(childIndex: Int, position: Int): ViewGroup?

        /**
         * Return a data source to play.
         * @param position Current adapter position.
         */
        fun getVideoDataSource(position: Int): DataSource?
    }

}