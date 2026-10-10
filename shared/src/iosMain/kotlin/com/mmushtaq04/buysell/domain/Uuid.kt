package com.mmushtaq04.buysell.domain

import platform.Foundation.NSUUID

actual fun generateUuid(): String = NSUUID().UUIDString()
