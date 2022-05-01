import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ThreadLocalRandom

class Sender(
	private val fileToTransfer: File,
	private val receiver: InetAddress,
	private val port: Int,
	private val chunkSize: Int,
	private val packetDelayUs: Long,
) {
	private val socket = DatagramSocket()
	private var seqNr = 0u

	fun send() {
		val uid = ThreadLocalRandom.current().nextInt().toUByte()
		val infoPacket = Packet(
			seqNr++, uid, InfoPacketBody(
				fileToTransfer.length().toULong(),
				fileToTransfer.name
			)
		)
		sendPacket(infoPacket)

		val s = HashingSink128(0)
		fileToTransfer.chunkedSequence(chunkSize).forEach {chunk ->
			s.putBytes(chunk)
			val dataPacket = Packet(
				seqNr++,
				uid,
				DataPacketBody(chunk)
			)
			sendPacket(dataPacket)
		}


		val hash = s.finish().asBigEndianByteArray()
		val finalizePacket = Packet(
			seqNr++,
			uid,
			FinalizePacketBody(
				hash
			)
		)
		sendPacket(finalizePacket)
	}

	private fun File.chunkedSequence(chunk: Int): Sequence<ByteArray> {
		val input = this.inputStream().buffered()
		val buffer = ByteArray(chunk)
		return generateSequence {
			val red = input.read(buffer)
			if (red >= 0) buffer.copyOf(red)
			else {
				input.close()
				null
			}
		}
	}

	private fun sendPacket(packet: Packet) {
		when (packet.packetBody) {
			is InfoPacketBody -> log("snd inf at			" + System.currentTimeMillis())
			is FinalizePacketBody -> log("snd fin at			" + System.currentTimeMillis())
			else -> {}
		}

		//println("Sending Packet $packet")
		val bytes = packet.serialize()
		val udpPacket = DatagramPacket(bytes, bytes.size, receiver, port)
		socket.send(udpPacket)

		Thread.sleep(packetDelayUs / 1000, ((packetDelayUs % 1000) * 1000).toInt())
	}
}
