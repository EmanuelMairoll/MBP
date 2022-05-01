import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Receiver(dropoffFolder: File, private val port: Int) {

	private var socket: DatagramSocket? = null
	private var digest = PacketDigest(dropoffFolder)
	private var sequencer = PacketSequencer(128, 1024, digest::continueSequence, digest::cancelSequence)

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
					//println("Received Packet $packet")
				}

				when (packet.packetBody) {
					is InfoPacketBody -> log("rec inf at			" + System.currentTimeMillis())
					is FinalizePacketBody -> log("rec fin at			" + System.currentTimeMillis())
					else -> {}
				}


				val transmissionId = transmissionId(packet.uid, udpPacket.address, udpPacket.port)
				sequencer.push(packet, transmissionId)
			} catch (e: Exception) {
				println("Discarded Packet with content [" + udpPacket.data.copyOfRange(0, udpPacket.length).toHex() + "]")
			}
		}
	}

	private fun transmissionId(uid: UByte, receiverAddress: InetAddress, receiverPort: Int) =
		uid.hashCode() + receiverAddress.hashCode() + receiverPort.hashCode()
}
