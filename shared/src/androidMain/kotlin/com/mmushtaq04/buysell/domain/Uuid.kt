package com.mmushtaq04.buysell.domain

import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()
