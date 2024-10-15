package com.goodwy.gallery.helpers

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.provider.Settings
import com.goodwy.gallery.extensions.audioManager

class AudioVolumeObserver(private val context: Context) {
  private var contentObserver: AudioVolumeContentObserver? = null

  fun register(audioStreamType: Int, listener: OnAudioVolumeChangedListener) {
    val handler = Handler()
    // with this handler AudioVolumeContentObserver#onChange()
    //   will be executed in the main thread
    // To execute in another thread you can use a Looper
    // +info: https://stackoverflow.com/a/35261443/904907
    contentObserver = AudioVolumeContentObserver(
      handler,
      context.audioManager,
      audioStreamType,
      listener
    )
    context.contentResolver.registerContentObserver(
      Settings.System.CONTENT_URI,
      true,
      contentObserver!!
    )
  }

  fun unregister() {
    if (contentObserver != null) {
      context.contentResolver.unregisterContentObserver(contentObserver!!)
      contentObserver = null
    }
  }
}
