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
import com.seagazer.liteplayer.ListPlayer
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LitePlayerView
import com.seagazer.sample.R
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.navigationTo
import com.seagazer.sample.widget.ListCoverOverlay
import com.seagazer.sample.widget.SimpleItemDecoration
import kotlinx.android.synthetic.main.activity_list_player.*

class ListPlayerActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var listPlayer: ListPlayer
    private var isAutoPlay = true

    private lateinit var overlay: ListCoverOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_player)
        linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(SimpleItemDecoration(0, 10, 0, 10))
        val listAdapter = ListAdapter()
        recycler_view.adapter = listAdapter
        overlay = ListCoverOverlay(this)
        listPlayer = ListPlayer(LitePlayerView(this)).apply {
            displayProgress(true)
            setProgressColor(resources.getColor(R.color.colorAccent), resources.getColor(R.color.colorPrimaryDark))
            attachMediaController(LiteMediaController(this@ListPlayerActivity))
            attachOverlay(this@ListPlayerActivity.overlay)
        }
        val videoScrollListener = object : ListPlayer.VideoListScrollListener {

            override fun getVideoContainer(position: Int): ViewGroup? {
                // add different cover here, test data for example
                if (position % 2 == 0) {
                    overlay.setCover(R.drawable.timg)
                } else {
                    overlay.setCover(R.drawable.ic_launcher_background)
                }
                overlay.show()
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

        override fun getItemCount() = DataProvider.urls.size

        fun getVideoUrl(position: Int) = DataProvider.urls[position]

        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            holder.run {
                image.setBackgroundResource(R.drawable.timg)
                text.text = position.toString()
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
