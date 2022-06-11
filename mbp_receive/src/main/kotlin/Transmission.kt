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
    private val fastRetransmissionThreshold: Int,
    private val dropoffFolder: File,
    val retransmissionDelayMs: Int,
    val answer: (Transmission, Packet) -> Unit
) {

    open class TransmissionException : Exception()
    object ProtocolException : TransmissionException()
    object SequencingException : TransmissionException()

    private val cachedPackets = mutableListOf<Packet>()
    private var nextSeqNr = 0u

    private var fileWriter: FileWriter? = null

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
                    continueInOrder(packet)
                    nextSeqNr++
                } else if (cachedPackets.none { it.seqNr == packet.seqNr }) {
                    cachedPackets += packet
                    continueWithCached()
                }
            } else {
                if (cachedPackets.size < maxOOSCache) {
                    cachedPackets += packet

                    if (cachedPackets.size == fastRetransmissionThreshold && nextSeqNr > 0u) {
                        //answer(this, Packet((nextSeqNr - 1u), uid, AckPacketBody))
                        //answer(this, Packet((nextSeqNr - 1u), uid, AckPacketBody))
                    }
                } else {
                    throw SequencingException
                }
            }

        } catch (e: TransmissionException) {
            terminate(e.javaClass.simpleName)
            throw e
        }
    }

    private fun continueWithCached() {
        println("continueWithCached")
        if (cachedPackets.isNotEmpty()) {
            cachedPackets.sortBy { it.seqNr }

            while (cachedPackets.size > 0 && cachedPackets.first().seqNr == nextSeqNr) {
                continueInOrder(cachedPackets.removeFirst(), cachedPackets.isEmpty())
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

    private fun continueInOrder(packet: Packet, ack: Boolean = true) {
        retransmissionTask?.cancel()
        retransmissionTask = null

        when (packet.packetBody) {
            is InfoPacketBody -> {
                if (packet.seqNr != 0u) throw ProtocolException
                fileWriter = FileWriter(dropoffFolder, packet.packetBody as InfoPacketBody)
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

        if (ack) {
            answer(this, Packet(packet.seqNr, uid, AckPacketBody))

            if (packet.packetBody !is FinalizePacketBody) {
                val self = this
                retransmissionTask =
                    retransmissionTimer.schedule(retransmissionDelayMs.toLong(), retransmissionDelayMs.toLong()) {
                        answer(self, Packet(packet.seqNr, uid, AckPacketBody))
                    }
            }
        }
    }
}
