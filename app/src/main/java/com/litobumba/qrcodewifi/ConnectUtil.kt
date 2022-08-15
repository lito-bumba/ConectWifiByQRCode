package com.litobumba.qrcodewifi

import android.os.Build


inline fun<T> is29AndAbove(
    func: () -> T
) :T?{
    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        func.invoke()
    else
        null
}