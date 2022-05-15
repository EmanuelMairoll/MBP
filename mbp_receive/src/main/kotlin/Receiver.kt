import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

class Receiver(
	private val dropoffFolder: File,
	private val port: Int,
	private val retransmissionDelay: Int
	) {

	private var socket: DatagramSocket? = null
	private var activeTransmissions = hashMapOf<Int, Transmission>()


	fun start() {
		socket = DatagramSocket(port)
		socket!!.broadcast = true

		val buffer = ByteArray(65527) // MAX UDP SIZE

		while (true) {
			val udpPacket = DatagramPacket(buffer, buffer.size)
			socket!!.receive(udpPacket)

			/*
			if (Random.nextInt(10) == 0){
				println("dropping packet")
				continue
			}
			 */

			try {
				val packet = Packet(udpPacket.data, udpPacket.length)
				if (packet.seqNr % 1u == 0u || packet.packetBody !is DataPacketBody) {
					println("Received Packet $packet")
				}

				when (packet.packetBody) {
					is InfoPacketBody -> log("rec inf at			" + System.currentTimeMillis())
					is FinalizePacketBody -> log("rec fin at			" + System.currentTimeMillis())
					else -> {}
				}

				receivePacket(packet, udpPacket)
			} catch (e: Exception) {
				println(
					"Discarded Packet with content [" + udpPacket.data.copyOfRange(0, udpPacket.length).toHex() + "]"
				)
			}
		}
	}

	private fun receivePacket(packet: Packet, udpPacket: DatagramPacket) {
		val transmissionId = transmissionId(packet.uid, udpPacket.address, udpPacket.port)
		val transmission = activeTransmissions.getOrPut(transmissionId) {
			Transmission(
				packet.uid,
				udpPacket.address,
				udpPacket.port,
				32,
				dropoffFolder,
				retransmissionDelay,
				this::answer,
			)
		}

		try {
			transmission.push(packet)

			if (transmission.isDone) {
				activeTransmissions -= transmissionId
			}
		} catch (e: Transmission.TransmissionException) {
			println("Terminated transmission due to $e")
			activeTransmissions -= transmissionId
		}
	}

	private fun answer(source: Transmission, packet: Packet) {
		val bytes = packet.serialize()
		val udpPacket = DatagramPacket(bytes, bytes.size, source.receiverAddress, source.receiverPort)
		socket!!.send(udpPacket)
	}

	private fun transmissionId(uid: UByte, receiverAddress: InetAddress, receiverPort: Int) =
		uid.hashCode() + receiverAddress.hashCode() + receiverPort.hashCode()

}
