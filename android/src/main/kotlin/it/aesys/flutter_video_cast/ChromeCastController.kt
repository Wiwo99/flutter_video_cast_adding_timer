package it.aesys.flutter_video_cast

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.ArrayAdapter
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.images.WebImage
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


class ChromeCastController(
    messenger: BinaryMessenger,
    viewId: Int,
    context: Context?
) : PlatformView, MethodChannel.MethodCallHandler, SessionManagerListener<Session>,
    PendingResult.StatusListener {
    private val channel = MethodChannel(messenger, "flutter_video_cast/chromeCast_$viewId")
    private val chromeCastButton =
        MediaRouteButton(ContextThemeWrapper(context, R.style.Theme_AppCompat_NoActionBar))
    private val sessionManager = CastContext.getSharedInstance()?.sessionManager
    lateinit var subtitle: String

    init {
        CastButtonFactory.setUpMediaRouteButton(context, chromeCastButton)
        channel.setMethodCallHandler(this)
    }

    private fun loadMedia(args: Any?) {
        if (args is Map<*, *>) {
            val url = args["url"] as? String
            subtitle = args["subTitle"] as String
            val meta = MediaMetadata(MediaMetadata.MEDIA_TYPE_GENERIC)
            meta.putString(MediaMetadata.KEY_TITLE, args["title"] as? String)
            (args["image"] as? String).let { imageUrl ->
                meta.addImage(WebImage(Uri.parse(imageUrl)))
            }
            

            val media =
                MediaInfo.Builder(url).setStreamType(args["streamType"] as Int)
                    .setMetadata(meta)
                    .build()
            val options = MediaLoadOptions.Builder().build()
            val request =
                sessionManager?.currentCastSession?.remoteMediaClient?.load(media, options)

            request?.addStatusListener(this)
        }
    }

    private fun loadMediaTvShow(args: Any?){
        
        if (args is Map<*, *>) {
            val url = args["url"] as? String
            val image = args["image"] as? String
            val seriesTitle = args["seriesTitle"] as? String
            val season = args["season"] as Int
            val episode = args["episode"] as Int
            var currentTime = args["currentTime"] as Int
            
            val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_TV_SHOW)
            movieMetadata.putString(MediaMetadata.KEY_SERIES_TITLE, seriesTitle)
            movieMetadata.putInt(MediaMetadata.KEY_SEASON_NUMBER, season)
            movieMetadata.putInt(MediaMetadata.KEY_EPISODE_NUMBER, episode)
            movieMetadata.addImage(WebImage(Uri.parse(image)))

            val media =
                MediaInfo.Builder(url).setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(movieMetadata)
                    .build()
            val options = MediaLoadOptions.Builder().setPlayPosition(currentTime?.toLong()).build()
            val request =
                sessionManager?.currentCastSession?.remoteMediaClient?.load(media, options)

            request?.addStatusListener(this)
        }
    }
    

    private fun play() {
        val request = sessionManager?.currentCastSession?.remoteMediaClient?.play()
        request?.addStatusListener(this)
    }


    private fun activeTracks() {
        val array = LongArray(1,init = {1})

        val request = sessionManager?.currentCastSession?.remoteMediaClient?.setActiveMediaTracks(
            array
        )
        Log.e("activeTracks", "tracks activated ")
        request?.addStatusListener(this)
    }

    private fun pause() {
        val request = sessionManager?.currentCastSession?.remoteMediaClient?.pause()
        request?.addStatusListener(this)
    }

    private fun seek(args: Any?) {
        if (args is Map<*, *>) {
            val relative = (args["relative"] as? Boolean) ?: false
            var interval = args["interval"] as? Double
            interval = interval?.times(1000)
            if (relative) {
                interval = interval?.plus(
                    sessionManager?.currentCastSession?.remoteMediaClient?.mediaStatus?.streamPosition
                        ?: 0
                )
            }
            val request =
                sessionManager?.currentCastSession?.remoteMediaClient?.seek(interval?.toLong() ?: 0)
            request?.addStatusListener(this)
        }
    }

    private fun setVolume(args: Any?) {
        if (args is Map<*, *>) {
            val volume = args["volume"] as? Double
            val request = sessionManager?.currentCastSession?.remoteMediaClient?.setStreamVolume(
                volume ?: 0.0
            )
            request?.addStatusListener(this)
        }
    }

    private fun getVolume() = sessionManager?.currentCastSession?.volume ?: 0.0

    private fun stop() {
        val request = sessionManager?.currentCastSession?.remoteMediaClient?.stop()
        request?.addStatusListener(this)
    }

    private fun isPlaying() =
        sessionManager?.currentCastSession?.remoteMediaClient?.isPlaying ?: false

    private fun isConnected() = sessionManager?.currentCastSession?.isConnected ?: false

    private fun endSession() = sessionManager?.endCurrentSession(true)

    private fun position() =
        sessionManager?.currentCastSession?.remoteMediaClient?.approximateStreamPosition ?: 0

    private fun duration() =
        sessionManager?.currentCastSession?.remoteMediaClient?.mediaInfo?.streamDuration ?: 0

    private fun addSessionListener() {
        sessionManager?.addSessionManagerListener(this)
    }

    private fun removeSessionListener() {
        sessionManager?.removeSessionManagerListener(this)
    }

    override fun getView() = chromeCastButton

    override fun dispose() {

    }

    // Flutter methods handling

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "chromeCast#wait" -> result.success(null)
            "chromeCast#loadMedia" -> {
                loadMedia(call.arguments)
                result.success(null)
            }
            "chromeCast#loadMediaTvShow" -> {
                loadMediaTvShow(call.arguments)
                result.success(null)
            }
            "chromeCast#play" -> {
                play()
                result.success(null)
            }
            "chromeCast#activeTracks" -> {
                activeTracks()
                result.success(null)
            }
            "chromeCast#pause" -> {
                pause()
                result.success(null)
            }
            "chromeCast#seek" -> {
                seek(call.arguments)
                result.success(null)
            }
            "chromeCast#setVolume" -> {
                setVolume(call.arguments)
                result.success(null)
            }
            "chromeCast#getVolume" -> result.success(getVolume())
            "chromeCast#stop" -> {
                stop()
                result.success(null)
            }
            "chromeCast#isPlaying" -> result.success(isPlaying())
            "chromeCast#isConnected" -> result.success(isConnected())
            "chromeCast#endSession" -> {
                endSession()
                result.success(null)
            }
            "chromeCast#position" -> result.success(position())
            "chromeCast#duration" -> result.success(duration())
            "chromeCast#addSessionListener" -> {
                addSessionListener()
                result.success(null)
            }
            "chromeCast#removeSessionListener" -> {
                removeSessionListener()
                result.success(null)
            }
        }
    }

    // SessionManagerListener

    override fun onSessionStarted(p0: Session?, p1: String?) {
        channel.invokeMethod("chromeCast#didStartSession", null)
    }

    override fun onSessionEnded(p0: Session?, p1: Int) {
        channel.invokeMethod("chromeCast#didEndSession", null)
    }

    override fun onSessionResuming(p0: Session?, p1: String?) {

    }

    override fun onSessionResumed(p0: Session?, p1: Boolean) {

    }

    override fun onSessionResumeFailed(p0: Session?, p1: Int) {

    }

    override fun onSessionSuspended(p0: Session?, p1: Int) {

    }

    override fun onSessionStarting(p0: Session?) {
    }

    override fun onSessionEnding(p0: Session?) {

    }

    override fun onSessionStartFailed(p0: Session?, p1: Int) {

    }

    // PendingResult.StatusListener

    override fun onComplete(status: Status?) {
        if (status?.isSuccess == true) {
            channel.invokeMethod("chromeCast#requestDidComplete", null)
        }
    }
}
