package com.mtchship.chameleon.kyc

sealed interface ChameleonTransport {
    data class Cname(
        val endpoint: String,
        val keyToken: String,
        val headers: Map<String, String> = emptyMap()
    ) : ChameleonTransport

    fun interface Custom : ChameleonTransport {
        suspend fun onFrameReady(payload: FramePayload)
    }
}
