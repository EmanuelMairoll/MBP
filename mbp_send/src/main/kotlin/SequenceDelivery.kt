import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.system.exitProcess

class SequenceDelivery(
    private val socket: DatagramSocket,
    packetSource: Sequence<Packet>,
    private val receiver: InetAddress,
    private val port: Int,
    private val maxUnackedPackets: Int
) {
    private val unackedPackets = LinkedList<Packet>()
    private var unackedPacketsTailOffset = 0u
    private var lastReceivedAck: Packet? = null

    private val bufferLock = ReentrantLock()
    private val sendingBlocked = bufferLock.newCondition()

    private val packetIterator = packetSource.iterator()
    private val udpBuffer = ByteArray(65527) // MAX UDP SIZE

    private val rttCalculator = RTTCalculator(socket)

    fun deliver() {
        val receiver = startAckReceiver()

        bufferLock.lock()
        while (packetIterator.hasNext() || unackedPackets.isNotEmpty()) {
            deliveryStep()
        }

        receiver.stop()
    }

    private fun deliveryStep() {
        if (unackedPacketsTailOffset > 0u) {
            val packetToSend = unackedPackets[unackedPackets.size - unackedPacketsTailOffset.toInt()]
            unackedPacketsTailOffset--

            bufferLock.unlock()

            sendSinglePacket(packetToSend)

            bufferLock.lock()
        } else {
            if (packetIterator.hasNext()) {
                if (unackedPackets.size >= maxUnackedPackets) {
                    sendingBlocked.await()
                } else {
                    unackedPackets.addLast(packetIterator.next())
                    unackedPacketsTailOffset++
                }
            } else {
                sendingBlocked.await()
            }
        }
    }

    private fun sendSinglePacket(packet: Packet) {
        val bytes = packet.serialize()
        val udpPacket = DatagramPacket(bytes, bytes.size, receiver, port)

        //println("Sending Packet $packet")
        socket.send(udpPacket)
        rttCalculator.startRoundtrip(packet.seqNr)
    }

    private fun startAckReceiver() = thread(start = true) {
        while (true) {
            val udpResponse = DatagramPacket(udpBuffer, udpBuffer.size)
            try {
                socket.receive(udpResponse)

                val packet = Packet(udpResponse.data, udpResponse.length)
                //println("Receiving Packet $packet")
                when (packet.packetBody) {
                    is AckPacketBody -> {
                        rttCalculator.endRoundtrip(packet.seqNr)
                        updateRetransmissionTimeout()

                        if (lastReceivedAck?.seqNr != packet.seqNr) {
                            onAckReceived(packet)
                            lastReceivedAck = packet
                        } else {
                            onPacketLoss()
                        }
                    }
                    is ErrorPacketBody -> throw Exception((packet.packetBody as ErrorPacketBody).reason)
                    else -> throw Exception("Receiver responded with unknown Packet")
                }

            } catch (e: SocketTimeoutException) {
                println("Timeout")
                onPacketLoss()
            }
        }
    }


    private fun onAckReceived(ack: Packet) {
        bufferLock.withLock {
            val sendBufferIterator = unackedPackets.iterator()
            while (sendBufferIterator.hasNext()) {
                val packet = sendBufferIterator.next()
                if (packet.seqNr <= ack.seqNr) {
                    sendBufferIterator.remove()
                    sendingBlocked.signal()
                } else {
                    break
                }
            }

            if (unackedPacketsTailOffset > unackedPackets.size.toUInt()) {
                unackedPacketsTailOffset = unackedPackets.size.toUInt()
            }
        }
    }

    private fun onPacketLoss() {
        bufferLock.withLock {
            unackedPacketsTailOffset = unackedPackets.size.toUInt()
            sendingBlocked.signal()
        }
    }

    private fun updateRetransmissionTimeout(){
        socket.soTimeout = rttCalculator.calculate()
    }
}

