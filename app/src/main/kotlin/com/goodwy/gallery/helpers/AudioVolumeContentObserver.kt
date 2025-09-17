package com.goodwy.gallery.helpers

import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import androidx.annotation.NonNull

class AudioVolumeContentObserver(
    handler: Handler,
    @NonNull private val mAudioManager: AudioManager,
    private val mAudioStreamType: Int,
    @NonNull private val mListener: OnAudioVolumeChangedListener
) : ContentObserver(handler) {

    private var mLastVolume: Float = mAudioManager.getStreamVolume(mAudioStreamType).toFloat()

    /**
     * Depending on the handler this method may be executed on the UI thread
     */
    override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (mAudioManager != null && mListener != null) {
            val maxVolume = mAudioManager.getStreamMaxVolume(mAudioStreamType)
            val currentVolume = mAudioManager.getStreamVolume(mAudioStreamType)
            if (currentVolume != mLastVolume.toInt()) {
                mLastVolume = currentVolume.toFloat()
                mListener.onAudioVolumeChanged(currentVolume, maxVolume)
            }
        }
    }

    override fun deliverSelfNotifications(): Boolean {
        return super.deliverSelfNotifications()
    }
}
