package com.seagazer.sample.example

import android.graphics.Color
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
import com.seagazer.liteplayer.list.ListPlayer
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.multitype.MultiTypeAdapter
import com.seagazer.multitype.ViewTypeCreator
import com.seagazer.sample.ConfigHolder
import com.seagazer.sample.R
import com.seagazer.sample.cache.VideoCacheHelper
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.widget.ListCoverOverlay
import com.seagazer.sample.widget.SimpleItemDecoration
import kotlinx.android.synthetic.main.activity_list_player3.*

class ListPlayerActivity3 : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var listPlayer: ListPlayer
    private lateinit var coverOverlay: ListCoverOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_player3)
        // multi type
        val adapter = MyAdapter()
        adapter.registerCreator(ImageCreator())
        adapter.registerCreator(VideoCreator())
        adapter.registerCreator(TitleCreator())
        fillData(adapter)
        linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recycler_view.layoutManager = linearLayoutManager
        recycler_view.addItemDecoration(SimpleItemDecoration(0, 10, 0, 10))
        recycler_view.adapter = adapter
        // list player
        coverOverlay = ListCoverOverlay(this)
        listPlayer = ListPlayer(LitePlayerView(this)).apply {
            displayProgress(true)
            setProgressColor(resources.getColor(R.color.colorAccent), Color.YELLOW)
            attachMediaController(LiteMediaController(this@ListPlayerActivity3))
            attachGestureController(LiteGestureController(this@ListPlayerActivity3).apply {
                supportVolume = false
                supportBrightness = false
            })
            attachOverlay(this@ListPlayerActivity3.coverOverlay)
            setRenderType(ConfigHolder.renderType)
            setPlayerType(ConfigHolder.playerType)
            // support cache player history progress
            supportHistory = true
        }
        val videoScrollListener = object : ListPlayer.VideoListScrollListener {

            override fun getVideoContainer(position: Int): ViewGroup? {
                // add different cover here, test data for example
                if (position % 2 == 0) {
                    coverOverlay.setCover(R.drawable.timg)
                } else {
                    coverOverlay.setCover(R.drawable.ic_launcher_background)
                }
                coverOverlay.show()
                val holder = recycler_view.findViewHolderForAdapterPosition(position)
                return if (holder != null && holder is VideoCreator.Holder) {
                    holder.videoContainer
                } else {
                    null
                }
            }

            override fun getVideoDataSource(position: Int): DataSource? {
                val data = adapter.getData(position)
                return if (data is DataSource) {
                    DataSource(VideoCacheHelper.url(data.mediaUrl))
                } else {
                    null
                }
            }
        }
        listPlayer.attachToRecyclerView(recycler_view, true, videoScrollListener)
    }

    private fun fillData(adapter: MyAdapter) {
        adapter.dataList.add(R.drawable.test)
        adapter.dataList.add(DataSource(DataProvider.url1))
        adapter.dataList.add("I am Title")
        adapter.dataList.add(DataSource(DataProvider.url2))
        adapter.dataList.add(DataSource(DataProvider.url1))
        adapter.dataList.add(DataSource(DataProvider.url2))
        adapter.dataList.add("I am Music")
        adapter.dataList.add("I am Album")
        adapter.dataList.add(R.drawable.test)
        for (i in 0..3) {
            adapter.dataList.add(DataSource(DataProvider.url1))
            adapter.dataList.add(DataSource(DataProvider.url2))
        }
        adapter.dataList.add(R.drawable.test)
        adapter.dataList.add("I am End")
    }


    class MyAdapter : MultiTypeAdapter() {
        val dataList = mutableListOf<Any>()

        override fun getData(position: Int): Any = dataList[position]

        override fun getItemCount(): Int = dataList.size

    }

    class ImageCreator : ViewTypeCreator<Int, ImageCreator.Holder>() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val image: ImageView = itemView.findViewById(R.id.image)
        }

        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): Holder {
            return Holder(inflater.inflate(R.layout.item_view_image, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, data: Int) {
            holder.image.setImageResource(data)
        }

        override fun match(data: Int): Boolean = false
    }

    class TitleCreator : ViewTypeCreator<String, TitleCreator.Holder>() {

        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.main_title)
        }

        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): Holder {
            return Holder(inflater.inflate(R.layout.view_type_main_title, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, data: String) {
            holder.title.text = data
        }

        override fun match(data: String): Boolean = false
    }

    class VideoCreator : ViewTypeCreator<DataSource, VideoCreator.Holder>() {
        class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView = itemView.findViewById(R.id.video_index)
            val image: ImageView = itemView.findViewById(R.id.video_poster)
            val videoContainer: FrameLayout = itemView.findViewById(R.id.video_container)
        }

        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): Holder {
            return Holder(inflater.inflate(R.layout.item_view_video_list, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, data: DataSource) {
            holder.run {
                image.setBackgroundResource(R.drawable.timg)
                text.text = adapterPosition.toString()
            }
        }

        override fun match(data: DataSource): Boolean = false
    }

}