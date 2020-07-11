package com.seagazer.sample.example

import android.content.res.Configuration
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
import com.seagazer.liteplayer.ListPlayer
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.helper.OrientationSensorHelper
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LitePlayerView
import com.seagazer.sample.R
import com.seagazer.sample.navigationTo
import com.seagazer.sample.widget.SimpleItemDecoration
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.activity_player_list.*
import kotlinx.android.synthetic.main.activity_player_list.sensor

class PlayerListActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var listPlayer: ListPlayer
    private var isAutoPlay = true
    private val urls = arrayListOf(
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4",
        "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4",
        "https://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4",
        "https://vfx.mtime.cn/Video/2019/03/17/mp4/190317150237409904.mp4",
        "https://vfx.mtime.cn/Video/2019/03/14/mp4/190314223540373995.mp4",
        "https://vfx.mtime.cn/Video/2019/03/14/mp4/190314102306987969.mp4",
        "https://vfx.mtime.cn/Video/2019/03/13/mp4/190313094901111138.mp4",
        "https://vfx.mtime.cn/Video/2019/03/12/mp4/190312143927981075.mp4",
        "https://vfx.mtime.cn/Video/2019/03/12/mp4/190312083533415853.mp4",
        "https://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4",
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319104618910544.mp4",
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319125415785691.mp4",
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4",
        "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4",
        "https://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4",
        "https://vfx.mtime.cn/Video/2019/03/17/mp4/190317150237409904.mp4",
        "https://vfx.mtime.cn/Video/2019/03/14/mp4/190314223540373995.mp4",
        "https://vfx.mtime.cn/Video/2019/03/14/mp4/190314102306987969.mp4",
        "https://vfx.mtime.cn/Video/2019/03/13/mp4/190313094901111138.mp4",
        "https://vfx.mtime.cn/Video/2019/03/12/mp4/190312143927981075.mp4",
        "https://vfx.mtime.cn/Video/2019/03/12/mp4/190312083533415853.mp4",
        "https://vfx.mtime.cn/Video/2019/03/18/mp4/190318214226685784.mp4",
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319104618910544.mp4",
        "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319125415785691.mp4"
    )
    private lateinit var orientationSensorHelper: OrientationSensorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_list)
        linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(SimpleItemDecoration(0, 10, 0, 10))
        val listAdapter = ListAdapter()
        recycler_view.adapter = listAdapter

        listPlayer = ListPlayer(LitePlayerView(this).apply {
            displayProgress(true)
            attachMediaController(LiteMediaController(this@PlayerListActivity))
        })
        val videoScrollListener = object : ListPlayer.VideoListScrollListener {

            override fun getVideoContainer(position: Int): ViewGroup? {
                val holder = recycler_view.findViewHolderForAdapterPosition(position)
                return if (holder != null && holder is ListAdapter.VideoHolder) {
                    holder.videoContainer
                } else {
                    null
                }
            }

            override fun getVideoDataSource(position: Int): DataSource? {
                return DataSource(listAdapter.getVideoUrl(position))
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

        orientationSensorHelper = OrientationSensorHelper(this)
        sensor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                orientationSensorHelper.startWatching(this)
            } else {
                orientationSensorHelper.stopWatching()
            }
        }
    }

    inner class ListAdapter : RecyclerView.Adapter<ListAdapter.VideoHolder>() {

        inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView = itemView.findViewById(R.id.video_index)
            val image: ImageView = itemView.findViewById(R.id.video_poster)
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

        override fun getItemCount() = urls.size

        fun getVideoUrl(position: Int) = urls[position]

        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            holder.run {
                image.setBackgroundResource(R.drawable.timg)
                text.text = position.toString()
            }

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        listPlayer.setFullScreenMode(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onDestroy() {
        orientationSensorHelper.stopWatching()
        super.onDestroy()
    }

    fun jumpToActivity(view: View) {
        navigationTo(EmptyActivity::class.java)
    }

    override fun onBackPressed() {
        if (player_view.isFullScreen()) {
            player_view.setFullScreenMode(false)
        } else {
            super.onBackPressed()
        }
    }

}
