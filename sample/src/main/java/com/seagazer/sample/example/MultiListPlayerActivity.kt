package com.seagazer.sample.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seagazer.liteplayer.ListPlayer
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.sample.ConfigHolder
import com.seagazer.sample.R
import com.seagazer.sample.showConfigInfo
import com.seagazer.sample.widget.LoadingOverlay
import com.seagazer.sample.widget.SimpleItemDecoration
import kotlinx.android.synthetic.main.activity_multi_list_player.*

/**
 * Example for use in fragments with viewPager.
 */
class MultiListPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_list_player)
        showConfigInfo()

        tab_layout.setupWithViewPager(view_pager)
        view_pager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

            val fragments = mutableListOf<Pair<String, Fragment>>().apply {
                add(Pair("Tab1", ListFragment()))
                add(Pair("Tab2", ListFragment()))
                add(Pair("Tab3", ListFragment()))
                add(Pair("Tab4", ListFragment()))
                add(Pair("Tab5", ListFragment()))
            }

            override fun getItem(position: Int) = fragments[position].second

            override fun getPageTitle(position: Int) = fragments[position].first

            override fun getCount() = fragments.size
        }
        view_pager.offscreenPageLimit = 4
    }

    class ListFragment : Fragment() {
        private var urls = arrayListOf(
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

        private lateinit var listPlayer: ListPlayer
        private lateinit var recyclerView: RecyclerView
        private var rootView: View? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            if (rootView == null) {
                urls.shuffle()
                rootView = inflater.inflate(R.layout.fragment_video_list, container, false)
                recyclerView = rootView!!.findViewById(R.id.recycler_view)
                val linearLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                recyclerView.layoutManager = linearLayoutManager
                recyclerView.addItemDecoration(SimpleItemDecoration(0, 10, 0, 10))
                val listAdapter = ListAdapter()
                recyclerView.adapter = listAdapter

                listPlayer = ListPlayer(LitePlayerView(context!!)).apply {
                    displayProgress(true)
                    setRenderType(ConfigHolder.renderType)
                    setPlayerType(ConfigHolder.playerType)
                    attachOverlay(LoadingOverlay(context!!))
                }
                val videoScrollListener = object : ListPlayer.VideoListScrollListener {

                    override fun getVideoContainer(position: Int): ViewGroup? {
                        val holder = recyclerView.findViewHolderForAdapterPosition(position)
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
                listPlayer.attachToRecyclerView(recyclerView, false, videoScrollListener)
            }
            return rootView
        }

        override fun onResume() {
            super.onResume()
            listPlayer.resume()
        }

        override fun onStop() {
            super.onStop()
            listPlayer.pause(true)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            listPlayer.detachRecyclerView()
            rootView = null
        }

        inner class ListAdapter : RecyclerView.Adapter<ListAdapter.VideoHolder>() {

            inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val text: TextView = itemView.findViewById(R.id.video_index)
                val image: ImageView = itemView.findViewById(R.id.video_poster)
                val videoContainer: FrameLayout = itemView.findViewById(R.id.video_container)

                init {
                    itemView.setOnClickListener {
                        listPlayer.onItemClick(adapterPosition)
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

    }
}
