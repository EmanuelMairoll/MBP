import java.net.DatagramSocket
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RTTCalculator(val socket: DatagramSocket) {

    private val maxHistoryMs = 10000
    private val maxRTTs = 16

    var openRoundtripsMutex = ReentrantLock()
    var openRoundtrips = mutableMapOf<UInt, Long>()
    var rtts = LinkedList<Int>()

    fun startRoundtrip(seqNr: UInt) = openRoundtripsMutex.withLock {
        openRoundtrips[seqNr] = System.currentTimeMillis()

        val maxAge = System.currentTimeMillis() - maxHistoryMs
        openRoundtrips.entries.removeAll { (_, startTime) -> startTime < maxAge }
    }

    fun endRoundtrip(seqNr: UInt) = openRoundtripsMutex.withLock {
        val endTime = System.currentTimeMillis()
        openRoundtrips.remove(seqNr)?.let { startTime ->
            rtts.addLast((endTime - startTime).toInt())

            while (rtts.size > maxRTTs) {
                rtts.removeFirst()
            }
        }
    }

    fun calculate(): Int =
        if (rtts.size < maxRTTs) {
            1000
        } else {
            rtts.average().toInt() * 10
        }


}