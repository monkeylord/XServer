package monkeylord.XServer.api

import android.app.AndroidAppHelper
import cn.vove7.rhino.RhinoHelper
import cn.vove7.rhino.api.RhinoApi
import monkeylord.XServer.XServer.wsOperation
import monkeylord.XServer.utils.NanoHTTPD.IHTTPSession
import monkeylord.XServer.utils.NanoWSD.WebSocket
import monkeylord.XServer.utils.NanoWSD.WebSocketFrame
import monkeylord.XServer.utils.NanoWSD.WebSocketFrame.CloseCode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * # WsScript
 *
 * @author liben
 * 2020/11/22
 */
class WsScript : wsOperation {
    companion object {
        @JvmStatic
        val PATH = "/wsScript"
    }

    private var ws: WebSocketHandler? = null
    private val sf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())

    val onPrint: RhinoApi.OnPrint = RhinoApi.OnPrint { l, s ->
        ws?.trySend(sf.format(Date()) + "[$l]: $s")
    }

    private val engine by lazy {
        RhinoHelper(
                AndroidAppHelper.currentApplication(),
                mapOf("app" to AndroidAppHelper.currentApplication())
        ).also {
            RhinoApi.regPrint(onPrint)
        }
    }

    override fun handle(handshake: IHTTPSession?): WebSocket {
        ws = WebSocketHandler(handshake);
        return ws!!
    }

    fun handleMessage(data: String?) {
        engine.evalString(data ?: "", null as Array<*>?)
    }


    private inner class WebSocketHandler(
            handshakeRequest: IHTTPSession?
    ) : WebSocket(handshakeRequest) {
        fun trySend(payload: String?) {
            try {
                send(payload)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun onMessage(message: WebSocketFrame) {
            handleMessage(message.textPayload)
        }

        override fun onOpen() {
            trySend("Connect Success...")
        }

        override fun onClose(code: CloseCode, reason: String, initiatedByRemote: Boolean) {
            RhinoApi.unregPrint(onPrint)
        }

        override fun onPong(pong: WebSocketFrame) {

        }

        override fun onException(exception: IOException) {
            exception.printStackTrace()
        }
    }
}