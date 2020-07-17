# LitePlayer
<font color=#4A90CE size=5>A lite player for android by Kotlin language</font>
<font color=#4A90CE size=5>一个完全使用Kotlin开发的简洁高效，轻量级的播放器框架</font>

#### 目前v1版已支持功能：
* <font color=#4A90CE>支持ExoPlayer解码</font>
* <font color=#4A90CE>支持MediaPlayer解码</font>
* <font color=#4A90CE>支持SurfaceView和TextureView渲染</font>
* <font color=#4A90CE>支持AspectRatio画面比例模式</font>
* <font color=#4A90CE>支持FullScreen模式</font>
* <font color=#4A90CE>支持RecyclerView列表播放</font>
* <font color=#4A90CE>支持手势seek, setBrightness, setVolume操作</font>
* <font color=#4A90CE>支持预置Controller控制面板</font>
* <font color=#4A90CE>支持预置Topbar信息面板</font>
* <font color=#4A90CE>支持自定义Overlay面板</font>

#### TODO计划：
* 支持IjkPlayer解码
* 支持Cache本地缓存
* 支持适应各种类型数据流
* 支持Window悬浮窗模式
* 支持系统PictureInPicture模式
* 支持ListView列表播放
* 提供预置清晰度设置面板
* 提供预置播放速率设置面板
* 提供预置弹幕控制面板



#### v1版本模块设计：
<img src="https://upload-images.jianshu.io/upload_images/4420407-43ebcad07f04fe94.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="820" height="500"/>


#### 基本使用方法：
1. `LitePlayerView`为基础视频播放控件，继承至`FrameLayout`，下面列举一些基础`API`：
```kotlin
<LitePlayerView.kt>

    val litePlayerView = findViewById(R.id.lite_player_view)
    // 设置内置progress颜色
    litePlayerView.setProgressColor(progressColor, secondProgressColor)
    // 设置是否显示内置progress
    litePlayerView.displayProgress(true)
    // 设置是否开启重力感应自动全屏
    litePlayerView.setAutoSensorEnable(true)
    // 设置播放器内核：模式定义在PlayerType中
    litePlayerView.setPlayerType(PlayerType.TYPE_EXO_PLAYER)
    // 设置渲染方式：模式定义在RenderType中
    litePlayerView.setRenderType(RenderType.TYPE_SURFACE_VIEW)
    // 设置是否全屏
    litePlayerView.setFullScreenMode(true)
    // 判断当前是否全屏
    litePlayerView.isFullScreen()
    // 设置比例模式：模式定义在AspectRatio中
    litePlayerView.setAspectRatio(AspectRatio.W_21_9)
    // 添加内置媒体控制面板
    litePlayerView.attachMediaController(LiteMediaController(context))
    // 添加内置媒体顶部面板
    litePlayerView.attachMediaTopbar(LiteMediaTopbar(context))
    // 添加内置手势控制面板
    litePlayerView.attachGestureController(LiteGestureController(context))
    // 添加自定义面板(Loading等)
    litePlayerView.attachOverlay(LoadingOverlay(context))
    // 设置是否自动隐藏ITopbar和IController面板(IGesture面板会在操作后自动隐藏，自定义overlay需自己控制显示隐藏时机)
    litePlayerView.setAutoHideOverlay(false)
    // 设置媒体播放资源
    litePlayerView.setDataSource(DataSource(url))
    // 设置播放速率
    litePlayerView.setPlaySpeed(1.5f)
    // 设置监听播放器状态
    litePlayerView.setPlayerStateChangedListener(PlayerStateChangedListener())
    // 开始播放
    litePlayerView.start()
    // 暂停(是否用户主动暂停)[例如Activity移入后台，框架内部会标记为非用户暂停，页面状态恢复时会自动开启播放]
    litePlayerView.pause(true)
    // 恢复播放
    litePlayerView.resume()
    // 停止播放：在支持Lifecycle环境下无需主动调用，框架内部会自动调用
    litePlayerView.stop()
    // 销毁释放播放器：在支持Lifecycle环境下无需主动调用，框架内部会自动调用
    litePlayerView.destroy()
```

2. `ListPlayer`支持以极简的方式接入`RecyclerView`列表播放，框架设计使用代理模式，因此上述`LitePlayerView`的所有`API`都适用于`ListPlayer`：
```kotlin
<ListPlayer.kt>

    // 默认构造需传入一个LitePlayerView实例，LitePlayerView可根据上面api进行定制
    val listPlayer = ListPlayer(LitePlayerView(context))
    // 定义列表滑动回调
    val videoScrollListener = object : ListPlayer.VideoListScrollListener {
            override fun getVideoContainer(position: Int): ViewGroup? {
                // 返回当前索引itemView中装载LitePlayerView的容器，建议itemView布局中定义一个空的FrameLayout作为载体
            }

            override fun getVideoDataSource(position: Int): DataSource? {
                // 返回当前索引的媒体资源
            }
        }
    // 关联RecyclerView，并且设置是否滑动过程中自动播放
    listPlayer.attachToRecyclerView(recyclerView, true, videoScrollListener)
    // 设置是否滑动过程中自动播放，建议一般在attach中设置后无特殊需求不进行模式变更(动态切换会造成数据重置)
    listPlayer.setAutoPlayMode(true)
    // 非自动播放时，点击item播放调用方法
    listPlayer.onItemClick(adapterPosition)
```
##### 更多具体使用方式和场景可参考项目中的`sample`工程

#### Demo展示：
<table>
<tr>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-a27b968e174058bd.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-c1eb65b099d8cc30.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-fc89b62de8f8aa38.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
</tr>
</table>
<table>
<tr>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-500a2aaa6c1cd16f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-ec115d0291f51114.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-24c3a4ce486587fa.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
</tr>
</table>
<table>
<tr>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-b02251c1eee82c6e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-08cf02a96152ed76.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>
<td><center><img src="https://upload-images.jianshu.io/upload_images/4420407-267f81cf903729e8.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" width="240" height="450"/></center></td>

</tr>
</table>

### <font color=#ff0000>注意：Demo展示中的视频链接均来源于网络，仅供学习交流使用，严禁用于商业用途，若有侵权请联系作者删除(seagazer@qq.com) </color>