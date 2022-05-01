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
	private val maxChunkSize: Int
) {
	private val socket = DatagramSocket()

	fun send() {
		val fileContent: ByteArray = fileToTransfer.inputStream().readAllBytes()

		val s = HashingSink128(0)
		s.putBytes(fileContent)
		val hash = s.finish().asBigEndianByteArray()

		val uid = ThreadLocalRandom.current().nextInt().toUByte()
		val infoPacket = Packet(
			0u, uid, InfoPacketBody(
				fileContent.size.toULong(),
				fileToTransfer.name
			)
		)
		val dataPackets = fileContent
			.asList()
			.chunked(maxChunkSize)
			.mapIndexed { i, c ->
				Packet(
					i.toUInt() + 1u,
					uid,
					DataPacketBody(c.toByteArray())
				)
			}
		val finalizePacket = Packet(
			dataPackets.size.toUInt() + 1u,
			uid,
			FinalizePacketBody(
				hash
			)
		)

		val packets = mutableListOf<Packet>()
		packets.add(infoPacket)
		packets.addAll(dataPackets)
		packets.add(finalizePacket)

		sendAllPackets(packets)
	}

	private fun sendAllPackets(packets: List<Packet>) = packets.forEachIndexed{ i, p ->
		println("Sending packet "  + (i + 1) + " of " + packets.size + " <" + p + ">")
		sendPacket(p)
		Thread.sleep(10)
	}

	private fun sendPacket(packet: Packet) {
		val bytes = packet.serialize()
		val udpPacket = DatagramPacket(bytes, bytes.size, receiver, port)
		socket.send(udpPacket)
	}
}
