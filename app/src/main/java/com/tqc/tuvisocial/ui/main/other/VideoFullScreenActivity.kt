package com.tqc.tuvisocial.ui.main.other

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.tqc.tuvisocial.R
import com.tqc.tuvisocial.base.BaseActivity
import com.tqc.tuvisocial.databinding.ActivityVideoFullScreenBinding
import com.tqc.tuvisocial.sharedPref.Extensions.setOnClick

class VideoFullScreenActivity : BaseActivity() {
    override fun getContainerId() = 0

    private lateinit var mPlayer: ExoPlayer

    private lateinit var binding: ActivityVideoFullScreenBinding

    //Minimum Video you want to buffer while Playing
    private val MIN_BUFFER_DURATION = 2000

    //Max Video you want to buffer during PlayBack
    private val MAX_BUFFER_DURATION = 5000

    //Min Video you want to buffer before start Playing it
    private val MIN_PLAYBACK_START_BUFFER = 1500

    //Min video You want to buffer when user resumes video
    private val MIN_PLAYBACK_RESUME_BUFFER = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoFile = intent.extras?.getString("Video")
        val name = intent.extras?.getString("Name") ?: ""

        mPlayer = ExoPlayer.Builder(this).setLoadControl(
            DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, 16))
                .setBufferDurationsMs(MIN_BUFFER_DURATION,
                MAX_BUFFER_DURATION,
                MIN_PLAYBACK_START_BUFFER,
                MIN_PLAYBACK_RESUME_BUFFER)
                .setTargetBufferBytes(-1)
                .setPrioritizeTimeOverSizeThresholds((true)).build()
        ).setTrackSelector(DefaultTrackSelector(this)).build()

        binding.viewVideo.apply {
            player = mPlayer
            mPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoFile)))
            mPlayer.prepare()
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowFastForwardButton(false)
            setShowRewindButton(false)
            setShowMultiWindowTimeBar(false)
            mPlayer.play()
//                //ẩn icon play
            val controller = findViewById<PlayerControlView>(R.id.exo_controller)
            controller.findViewById<AppCompatImageButton>(R.id.exo_play).setImageResource(0)
            controller.findViewById<AppCompatImageButton>(R.id.exo_pause).setImageResource(0)
            //Xử lý play - pause
            setOnClick {
                if (mPlayer.isPlaying) {
                    mPlayer.pause()
                    binding.playLayout.visibility = View.VISIBLE
                } else {
                    mPlayer.play()
                    binding.playLayout.visibility = View.GONE
                }
            }
        }

        binding.nameTV.text = name

        binding.backImg.setOnClick {
            finish()
        }
        binding.volumeImg.apply {
            setOnClick {
                if (mPlayer.volume == 0f) {
                    mPlayer.volume = mPlayer.deviceVolume.toFloat()
                    setImageResource(R.drawable.volume)
                } else {
                    mPlayer.volume = 0f
                    setImageResource(R.drawable.ic_mute)
                }
            }
        }
        binding.fullScreenImg.setOnClick {
            requestedOrientation =
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onPause() {
        super.onPause()
        mPlayer.pause()
    }
}