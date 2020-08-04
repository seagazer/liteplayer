package com.seagazer.sample.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.ListPlayer2
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.sample.R
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.navigationTo
import com.seagazer.sample.widget.ListCoverOverlay
import kotlinx.android.synthetic.main.activity_list_player2.*

/**
 * Example for use in ListView.
 */
class ListPlayerActivity2 : AppCompatActivity() {
    private lateinit var listPlayer: ListPlayer2
    private var isAutoPlay = true
    private lateinit var overlay: ListCoverOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_player2)

        val listAdapter = ListAdapter()
        list_view.adapter = listAdapter
        list_view.setOnItemClickListener { _, _, position, _ ->
            if (!isAutoPlay) {
                listPlayer.onItemClick(position)
            }
        }

        overlay = ListCoverOverlay(this)
        listPlayer = ListPlayer2(LitePlayerView(this)).apply {
            displayProgress(true)
            setProgressColor(resources.getColor(R.color.colorAccent), resources.getColor(R.color.colorPrimaryDark))
            attachMediaController(LiteMediaController(this@ListPlayerActivity2))
            attachOverlay(this@ListPlayerActivity2.overlay)
        }
        val videoScrollListener = object : ListPlayer2.VideoListScrollListener {

            override fun getVideoContainer(childIndex: Int, position: Int): ViewGroup? {
                // add different cover here, test data for example
                if (position % 2 == 0) {
                    overlay.setCover(R.drawable.timg)
                } else {
                    overlay.setCover(R.drawable.ic_launcher_background)
                }
                overlay.show()
                val itemView = list_view.getChildAt(childIndex)
                return if (itemView != null && itemView.tag != null) {
                    (itemView.tag as ListAdapter.VideoHolder).videoContainer
                } else {
                    null
                }
            }

            override fun getVideoDataSource(position: Int): DataSource? {
                return DataSource(listAdapter.getItem(position))
            }
        }
        listPlayer.attachToListView(list_view, true, videoScrollListener)

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

    inner class ListAdapter : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val itemView: View
            val holder: VideoHolder
            if (convertView == null) {
                itemView = LayoutInflater.from(parent!!.context).inflate(R.layout.item_view_video_list, parent, false)
                holder = VideoHolder()
                holder.text = itemView.findViewById(R.id.video_index)
                holder.image = itemView.findViewById(R.id.video_poster)
                holder.videoContainer = itemView.findViewById(R.id.video_container)
                itemView.tag = holder
            } else {
                itemView = convertView
                holder = convertView.tag as VideoHolder
            }
            holder.run {
                image.setBackgroundResource(R.drawable.timg)
                text.text = position.toString()
            }
            return itemView
        }

        override fun getItem(position: Int) = DataProvider.urls[position % 2]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = DataProvider.urls.size * 10

        inner class VideoHolder {
            lateinit var text: TextView
            lateinit var image: ImageView
            lateinit var videoContainer: FrameLayout

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
