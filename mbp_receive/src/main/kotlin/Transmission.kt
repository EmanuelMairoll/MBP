import java.io.File
import java.net.InetAddress
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

data class Transmission(
	val uid: UByte,
	val receiverAddress: InetAddress,
	val receiverPort: Int,
	private val maxOOSCache: Int,
	private val dropoffFolder: File,
	val retransmissionDelayMs: Int,
	val answer: (Transmission, Packet) -> Unit
) {

	open class TransmissionException : Exception()
	object ProtocolException : TransmissionException()
	object SequencingException : TransmissionException()

	private val cachedPackets = mutableListOf<Packet>()
	private var nextSeqNr = 0u

	private var fileWriter: PacketFileWriter? = null

	private val openedAt = System.currentTimeMillis()
	private var lastPacketAt = System.currentTimeMillis()

	private val retransmissionTimer = Timer()
	private var retransmissionTask: TimerTask? = null

	var isDone = false
		private set

	/**
	 * returns true if done
	 */
	fun push(packet: Packet) {
		if (packet.seqNr < nextSeqNr) return

		try {
			if (packet.seqNr == nextSeqNr) {
				if (cachedPackets.isEmpty()) {
					println("A")
					continueInOrder(packet)
					nextSeqNr++
				} else {
					println("B")
					cachedPackets += packet
					continueWithCached()
				}
			} else {
				if (cachedPackets.size < maxOOSCache) {
					println("C")
					cachedPackets += packet
				} else {
					println("D")
					throw SequencingException
				}
			}

		} catch (e: TransmissionException) {
			terminate(e.javaClass.simpleName)
			throw e
		}
	}

	private fun continueWithCached() {
		if (cachedPackets.isNotEmpty()) {
			cachedPackets.sortBy { it.seqNr }

			while (cachedPackets.size > 0 && cachedPackets.first().seqNr == nextSeqNr) {
				continueInOrder(cachedPackets.removeFirst(), cachedPackets.isNotEmpty())
				nextSeqNr++
			}
		}
	}

	private fun terminate(reason: String) {
		answer(
			this, Packet(
				nextSeqNr,
				uid,
				ErrorPacketBody(
					reason
				)
			)
		)
		fileWriter?.del()
	}

	private fun continueInOrder(packet: Packet, skipAck: Boolean = false) {
		retransmissionTask?.cancel()
		retransmissionTask = null

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
			else -> throw ProtocolException
		}

		if (!skipAck) {
			answer(this, Packet(packet.seqNr, uid, AckPacketBody))
			val self = this
			retransmissionTask =
				retransmissionTimer.schedule(retransmissionDelayMs.toLong(), retransmissionDelayMs.toLong()) {
					answer(self, Packet(packet.seqNr, uid, AckPacketBody))
				}
		}
	}
}
