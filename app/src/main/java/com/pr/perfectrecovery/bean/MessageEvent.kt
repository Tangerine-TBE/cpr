package com.pr.perfectrecovery.bean

data class MessageEventData(
    val code: Int,
    val cycleCount: String,
    val data: BaseDataDTO?,
    var isCheck: Boolean = false
) {
    var power: Int = 0
}