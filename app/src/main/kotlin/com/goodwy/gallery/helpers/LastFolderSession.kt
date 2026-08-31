package com.goodwy.gallery.helpers

// Remembers the last folder + scroll position the user was browsing, in memory only
// (never persisted to disk), so reopening the app while its process is still warm -
// most noticeably when another app launches this one as an image picker - can resume
// where the user left off instead of resetting back to the folder list every time.
object LastFolderSession {
    var path: String? = null
    var scrollPosition: Int = 0
}
