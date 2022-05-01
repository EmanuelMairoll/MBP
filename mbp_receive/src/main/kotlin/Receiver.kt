import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket

class Receiver(private val dropoffFolder: File, private val port: Int) {

	private var socket: DatagramSocket? = null
	private var collector = PacketSequenceCollector(128, this::processPacketSequence)

	fun start() {
		socket = DatagramSocket(port)
		socket!!.broadcast = true

		val buffer = ByteArray(65527) // MAX UDP SIZE

		while (true) {
			val udpPacket = DatagramPacket(buffer, buffer.size)
			socket!!.receive(udpPacket)

			try {
				val packet = Packet(udpPacket.data, udpPacket.length)
				if (packet.seqNr % 1u == 0u || packet.packetBody !is DataPacketBody) {
					println("Received Packet $packet")
				}

				collector.push(packet, udpPacket.address, udpPacket.port)
			} catch (e: Exception) {
				println("discarded packet [" + udpPacket.data.copyOfRange(0, udpPacket.length).toHex() + "]")
			}
		}

	}

	private fun processPacketSequence(packets: List<Packet>) {
		val info = packets.first().packetBody as InfoPacketBody
		val data = packets.subList(1, packets.size - 1).map { it.packetBody as DataPacketBody }
		val fin = packets.last().packetBody as FinalizePacketBody

		val fileContent = data.map { it.data }.fold(byteArrayOf()) { l, r -> l + r }

		val hashShould = fin.murmurHash3
		val s = HashingSink128(0)
		s.putBytes(fileContent)
		val hashActual = s.finish().asBigEndianByteArray()

		if (hashShould contentEquals hashActual) {
			val out = File(dropoffFolder, info.fileName)
			out.delete()
			val os = out.outputStream()
			os.write(fileContent)
			os.close()
		} else {
			println("Hashes do not match! Abort")
			println("Should: ${hashShould[0]} ${hashShould[1]}")
			println("Actual: ${hashActual[0]} ${hashActual[1]}")

			val out = File(dropoffFolder, info.fileName)
			out.delete()
			val os = out.outputStream()
			os.write(fileContent)
			os.close()
		}
	}

}
