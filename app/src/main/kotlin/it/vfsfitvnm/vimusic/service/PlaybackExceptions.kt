@file:OptIn(UnstableApi::class)

package it.vfsfitvnm.vimusic.service

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi

class PlayableFormatNotFoundException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
class UnplayableException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
class LoginRequiredException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
class VideoIdMismatchException : PlaybackException(null, null, ERROR_CODE_REMOTE_ERROR)
