import java.net.InetAddress

class OpenPacketSequence {
	val packets = mutableListOf<Packet>()
	val openedAt = System.currentTimeMillis()
}

class PacketSequenceCollector(
	private val maxOpenSequences: Int, private val finishedSequenceHandler: (List<Packet>) -> Unit
) {
	private val openSequences = mutableMapOf<Int, OpenPacketSequence>()

	fun push(packet: Packet, receiverAddress: InetAddress, receiverPort: Int) {
		val id = id(packet.uid, receiverAddress, receiverPort)
		val sequence = openSequences.getOrPut(id) { OpenPacketSequence() }
		val packets = sequence.packets

		val skipSorting = packets.size == 0 || packets.last().seqNr < packet.seqNr
		packets.add(packet)

		if (!skipSorting){
			packets.sortBy { it.seqNr }
		}

		if (packets.isComplete()){
			openSequences.remove(id)
			finishedSequenceHandler(packets)
		}

		purgeOldest()
	}

	private fun id(uid: UByte, receiverAddress: InetAddress, receiverPort: Int) =
		uid.hashCode() + receiverAddress.hashCode() + receiverPort.hashCode()

	private fun purgeOldest() {
		while (openSequences.size > maxOpenSequences) {
			openSequences.remove(
				openSequences.minByOrNull { it.value.openedAt }?.key
			)
		}
	}

	private fun List<Packet>.isComplete(): Boolean =
		this.first().packetBody is InfoPacketBody && this.last().packetBody is FinalizePacketBody && this.isAscending()

	private fun List<Packet>.isAscending(): Boolean {
		for (i in 0..this.size - 2) {
			if (this[i].seqNr + 1u != this[i + 1].seqNr) return false
		}
		return true
	}

}

