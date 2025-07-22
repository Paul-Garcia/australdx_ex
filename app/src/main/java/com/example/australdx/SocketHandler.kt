import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import android.util.Log


object SocketHandler {

    lateinit var mSocket: Socket

    @Synchronized
    fun setSocket() {
        try {
            mSocket = IO.socket("http://10.0.2.2:8080")
            Log.d("SocketIO", "Socket créé avec succès vers http://10.0.2.2:8080")
            setupListeners()
        } catch (e: URISyntaxException) {
            Log.e("SocketIO", "URISyntaxException: ${e.message}")
        }
    }

    @Synchronized
    fun getSocket(): Socket {
        return mSocket
    }

    @Synchronized
    fun establishConnection() {
        mSocket.connect()
    }

    @Synchronized
    fun closeConnection() {
        mSocket.disconnect()
    }

    fun setupListeners() {
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("SocketIO", "Connecté au serveur")
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketIO", "Erreur de connexion : ${args.joinToString()}")
        }

        mSocket.on(Socket.EVENT_DISCONNECT) {
            Log.d("SocketIO", "Déconnecté du serveur")
        }
    }
}