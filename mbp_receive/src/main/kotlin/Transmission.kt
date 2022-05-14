import java.io.File
import java.net.InetAddress

data class Transmission(
	val uid: UByte,
	val receiverAddress: InetAddress,
	val receiverPort: Int,
	val openedAtTimestamp: Long,
	private val maxOOSCache: Int,
	private val dropoffFolder: File
) {

	open class TransmissionException : Exception()
	object ProtocolException : TransmissionException()
	object SequencingException : TransmissionException()

	private val cachedPackets = mutableListOf<Packet>()
	private var fileWriter: PacketFileWriter? = null
	private var nextSeqNr = 0u

	var isDone = false
		private set

	/**
	 * returns true if done
	 */
	fun push(packet: Packet) {
		if (packet.seqNr == nextSeqNr) {
			continueInOrder(packet)
			nextSeqNr++

			continueWithCached()
		} else {
			if (cachedPackets.size < maxOOSCache){
				cachedPackets += packet
			} else {
				throw SequencingException
			}
		}

	}

	private fun continueWithCached(){
		if (cachedPackets.isNotEmpty()) {
			cachedPackets.sortBy { it.seqNr }

			while (cachedPackets.size > 0 && cachedPackets.first().seqNr == nextSeqNr) {
				continueInOrder(cachedPackets.removeFirst())
				nextSeqNr++
			}
		}
	}

	fun terminate() {
		fileWriter?.del()
	}

	private fun continueInOrder(packet: Packet) =
		when (packet.packetBody) {
			is InfoPacketBody -> {
				if (packet.seqNr != 0u) throw ProtocolException
				fileWriter = PacketFileWriter(dropoffFolder, packet.packetBody as InfoPacketBody)
			}
			is DataPacketBody -> {
				fileWriter!!.putData(packet.packetBody as DataPacketBody)
			}
			is FinalizePacketBody -> {
				fileWriter!!.putFin(packet.packetBody as FinalizePacketBody)
				isDone = true
			}

		}

}
