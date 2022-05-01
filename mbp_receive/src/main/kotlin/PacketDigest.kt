import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.io.OutputStream

class PacketDigest(
	private val dropoffFolder: File
) {

	private val openFiles = mutableMapOf<Int, Triple<File, OutputStream, HashingSink128>>()

	fun continueSequence(transmissionId: Int, packet: Packet): Boolean {
		return when (packet.packetBody) {
			is InfoPacketBody -> handleInfoPacket(
				transmissionId, packet.seqNr,
				packet.packetBody as InfoPacketBody
			)
			is DataPacketBody -> handleDataPacket(
				transmissionId, packet.seqNr,
				packet.packetBody as DataPacketBody
			)
			is FinalizePacketBody -> handleFinalizePacket(
				transmissionId, packet.seqNr,
				packet.packetBody as FinalizePacketBody
			)
		}
	}

	private fun handleInfoPacket(transmissionId: Int, seqNr: UInt, info: InfoPacketBody): Boolean {
		if (seqNr != 0u) throw Exception("sequence number invalid")
		if (openFiles[transmissionId] != null) throw Exception("no such open file")

		val f = File(dropoffFolder, info.fileName)

		f.delete()
		val os = f.outputStream()

		val h = HashingSink128(0)

		openFiles[transmissionId] = Triple(f, os, h)

		return true
	}

	private fun handleDataPacket(transmissionId: Int, seqNr: UInt, data: DataPacketBody): Boolean {
		if (seqNr == 0u) throw Exception("sequence number invalid")
		val (_, os, h) = openFiles[transmissionId] ?: throw Exception("no such open file")

		os.write(data.data)
		h.putBytes(data.data)

		return true
	}

	private fun handleFinalizePacket(transmissionId: Int, seqNr: UInt, fin: FinalizePacketBody): Boolean {
		val (_, os, h) = openFiles[transmissionId] ?: throw Exception("no such open file")

		val hashShould = fin.murmurHash3
		val hashActual = h.finish().asBigEndianByteArray()

		if (hashShould contentEquals hashActual) {
			println("File successfully transferred")
			os.flush()
			os.close()

		} else {
			println("Hashes do not match! Keeping corrupted file anyway!")
			println("Should: ${hashShould[0]} ${hashShould[1]}")
			println("Actual: ${hashActual[0]} ${hashActual[1]}")
		}

		return false
	}

	fun cancelSequence(transmissionId: Int) {
		val (f, os, h) = openFiles.remove(transmissionId) ?: return

		h.finish()
		os.close()
		f.delete()
	}

}
