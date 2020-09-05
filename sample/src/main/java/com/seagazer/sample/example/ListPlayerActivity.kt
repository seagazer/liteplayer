package com.seagazer.sample.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.list.ListItemChangedListener
import com.seagazer.liteplayer.list.ListPlayer
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.sample.ConfigHolder
import com.seagazer.sample.R
import com.seagazer.sample.cache.VideoCacheHelper
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.navigationTo
import com.seagazer.sample.showConfigInfo
import com.seagazer.sample.widget.LoadingOverlay
import com.seagazer.sample.widget.SimpleItemDecoration
import kotlinx.android.synthetic.main.activity_list_player.*

/**
 * Example for use in RecyclerView.
 */
class ListPlayerActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var listPlayer: ListPlayer
    private var isAutoPlay = true
    private var lastPlayerHolder: ListAdapter.VideoHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_player)
        showConfigInfo()

        linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(SimpleItemDecoration(0, 10, 0, 10))
        val listAdapter = ListAdapter()
        recycler_view.adapter = listAdapter
        listPlayer = ListPlayer(LitePlayerView(this)).apply {
            displayProgress(true)
            setProgressColor(resources.getColor(R.color.colorAccent), resources.getColor(R.color.colorPrimaryDark))
            attachOverlay(LoadingOverlay(this@ListPlayerActivity))
            attachMediaController(LiteMediaController(this@ListPlayerActivity))
            attachGestureController(LiteGestureController(this@ListPlayerActivity).apply {
                supportVolume = false
                supportBrightness = false
            })
            setRenderType(ConfigHolder.renderType)
            setPlayerType(ConfigHolder.playerType)
            // support cache player history progress
            supportHistory = true
            // sample to show and hide video cover
            // onDetachItemView always call before onAttachItemView
            listItemChangedListener = object : ListItemChangedListener {
                override fun onDetachItemView(oldPosition: Int) {
                    MediaLogger.e("detach item: $oldPosition")
                    lastPlayerHolder?.let {
                        it.videoPoster.visibility = View.VISIBLE
                    }
                }

                override fun onAttachItemView(newPosition: Int) {
                    MediaLogger.e("attach item: $newPosition")
                    lastPlayerHolder?.let {
                        it.videoPoster.visibility = View.INVISIBLE
                    }
                }
            }
        }
        val videoScrollListener = object : ListPlayer.VideoListScrollListener {

            override fun getVideoContainer(position: Int): ViewGroup? {
                recycler_view.findViewHolderForAdapterPosition(position)?.let {
                    if (it is ListAdapter.VideoHolder) {
                        lastPlayerHolder = it
                        return it.videoContainer
                    }
                }
                return null
            }

            override fun getVideoDataSource(position: Int): DataSource? {
                return DataSource(VideoCacheHelper.url(listAdapter.getVideoUrl(position)))
            }
        }
        listPlayer.attachToRecyclerView(recycler_view, true, videoScrollListener)

        play_mode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.auto_play -> {
                    listPlayer.setAutoPlayMode(true)
                    isAutoPlay = true
                }
                R.id.click_play -> {
                    listPlayer.setAutoPlayMode(false)
                    isAutoPlay = false
                }
            }
        }

        sensor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                listPlayer.setAutoSensorEnable(true)
            } else {
                listPlayer.setAutoSensorEnable(false)
            }
        }
    }

    inner class ListAdapter : RecyclerView.Adapter<ListAdapter.VideoHolder>() {

        inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val videoTitle: TextView = itemView.findViewById(R.id.video_index)
            val videoPoster: ImageView = itemView.findViewById(R.id.video_poster)
            val videoContainer: FrameLayout = itemView.findViewById(R.id.video_container)

            init {
                itemView.setOnClickListener {
                    if (!isAutoPlay) {
                        listPlayer.onItemClick(adapterPosition)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            return VideoHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_view_video_list,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount() = DataProvider.urls.size * 10

        fun getVideoUrl(position: Int) = DataProvider.urls[position % 2]

        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            holder.run {
                videoPoster.setBackgroundResource(R.drawable.timg)
                videoTitle.text = position.toString()
            }

        }
    }

    fun jumpToActivity(view: View) {
        navigationTo(EmptyActivity::class.java)
    }

    override fun onBackPressed() {
        if (listPlayer.isFullScreen()) {
            listPlayer.setFullScreenMode(false)
        } else {
            super.onBackPressed()
        }
    }

}
