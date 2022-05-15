import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

class Sender(
	private val fileToTransfer: File,
	private val receiver: InetAddress,
	private val port: Int,
	private val chunkSize: Int,
	retransmissionDelayMs: Int
) {
	private val rand = Random(45)
	private val socket = run {
		val s = DatagramSocket()
		s.soTimeout = retransmissionDelayMs
		s
	}
	private var seqNr = 0u
	private val buffer = ByteArray(65527) // MAX UDP SIZE

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
		fileToTransfer.chunkedSequence(chunkSize).forEach { chunk ->
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

		val bytes = packet.serialize()
		val udpPacket = DatagramPacket(bytes, bytes.size, receiver, port)

		// Control flow here is ugly as hell, but necessary IMO
		retransmission@ for (i in 1..5) {
			println("Sending Packet $packet")
			socket.send(udpPacket)

			do {
				var ackPacket: Packet?
				try {
					val udpResponse = DatagramPacket(buffer, buffer.size)
					socket.receive(udpResponse)

					if (rand.nextInt(10) == 0) {
						println("dropping ACK")
						continue@retransmission
					}

					val response = Packet(udpResponse.data, udpResponse.length)
					when (response.packetBody) {
						is AckPacketBody -> {
							if (response.seqNr > packet.seqNr) {
								throw Exception("Receiver ACKed SeqNr not even sent")
							} else {
								ackPacket = response
							}
						}
						is ErrorPacketBody -> throw Exception((response.packetBody as ErrorPacketBody).reason)
						else -> throw Exception("Receiver responded with unknown Packet")
					}
				} catch (e: SocketTimeoutException) {
					continue@retransmission
				}
			} while ((ackPacket == null) || (ackPacket.seqNr != packet.seqNr))
			return
		}

		throw Exception("Receiver did not respond in time after multiple retransmissions")
	}
}
