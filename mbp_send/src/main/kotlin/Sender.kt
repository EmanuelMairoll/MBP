import java.io.File
import java.net.DatagramSocket
import java.net.InetAddress

class Sender(
    private val fileToTransfer: File,
    private val receiver: InetAddress,
    private val port: Int,
    private val chunkSize: Int,
) {
    private val socket = run {
        val s = DatagramSocket()
        s.soTimeout = 1000
        s
    }

    fun send() {
        val sequence = SequenceBuilder(fileToTransfer, chunkSize).build()
        val delivery = SequenceDelivery(socket, sequence, receiver, port, 32)
        delivery.deliver()
    }


}
