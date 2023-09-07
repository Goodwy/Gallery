package com.goodwy.gallery.extensions

import com.goodwy.commons.helpers.SORT_DESCENDING

fun Int.isSortingAscending() = this and SORT_DESCENDING == 0
