package monkeylord.XServer.api

import android.os.Handler
import android.os.HandlerThread
import cn.vove7.rhino.RhinoHelper
import cn.vove7.rhino.api.RhinoApi
import monkeylord.XServer.XServer
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

        val engineThread by lazy {
            HandlerThread("WsScript").also {
                it.start()
            }
        }
        val engineHandler by lazy {
            Handler(engineThread.looper)
        }

        private val engine by lazy {
            RhinoHelper(
                    XServer.getCurrentApplication(),
                    mapOf("app" to XServer.getCurrentApplication())
            )
        }
    }

    private var ws: WebSocketHandler? = null
    private val sf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())

    val onPrint: RhinoApi.OnPrint = RhinoApi.OnPrint { l, s ->
        ws?.trySend("[$l] $s")
    }


    override fun handle(handshake: IHTTPSession?): WebSocket {
        ws = WebSocketHandler(handshake);
        return ws!!
    }

    fun handleMessage(data: String?) = engineHandler.post {
        kotlin.runCatching {
            engine.evalString(data ?: "", null as Array<*>?)
        }.onFailure {
            it.printStackTrace()
            ws?.trySend(it.toString())
        }
    }

    private inner class WebSocketHandler(
            handshakeRequest: IHTTPSession?
    ) : WebSocket(handshakeRequest) {
        fun trySend(payload: String?) {
            try {
                send(sf.format(Date()) + ": " + payload)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun onMessage(message: WebSocketFrame) {
            handleMessage(message.textPayload)
        }

        override fun onOpen() {
            RhinoApi.regPrint(onPrint)
            trySend("Connect Success...")
        }

        override fun onClose(code: CloseCode, reason: String, initiatedByRemote: Boolean) {
            RhinoApi.unregPrint(onPrint)
            engine.release()
        }

        override fun onPong(pong: WebSocketFrame) {

        }

        override fun onException(exception: IOException) {
            exception.printStackTrace()
        }
    }
}