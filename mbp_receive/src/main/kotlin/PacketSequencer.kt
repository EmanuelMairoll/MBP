class PacketSequencer(
	private val maxOpenSequences: Int,
	private val maxCachedPacketsPerSequence: Int,
	private val continueSequence: (Int, Packet) -> Boolean,
	private val cancelSequence: (Int) -> Unit
) {

	private class OpenPacketSequence {
		var nextSeqNr = 0u
		val cachedPackets = mutableListOf<Packet>()
		val openedAt = System.currentTimeMillis()
	}

	private val openSequences = mutableMapOf<Int, OpenPacketSequence>()

	fun push(packet: Packet, transmissionId: Int) {
		try {
			val sequence = openSequences.getOrPut(transmissionId) { OpenPacketSequence() }

			if (packet.seqNr != sequence.nextSeqNr) {
				sequence.cachedPackets += packet

				if (sequence.cachedPackets.size > maxCachedPacketsPerSequence) {
					openSequences.remove(transmissionId)
					cancelSequence(transmissionId)
				}
			} else {
				val continueSequence = continueSequence(transmissionId, packet)
				if (!continueSequence) {
					openSequences.remove(transmissionId)
				} else {
					sequence.nextSeqNr++

					val cache = sequence.cachedPackets
					if (cache.size > 0) {
						cache.sortBy { it.seqNr }

						while (cache.size > 0 && cache.first().seqNr == sequence.nextSeqNr) {
							val nextPacket = cache.removeFirst()
							val continueSequenceNow = continueSequence(transmissionId, nextPacket)

							if (!continueSequenceNow) {
								cache.clear()
								openSequences.remove(transmissionId)
							} else {
								sequence.nextSeqNr++
							}
						}
					}
				}
			}
		} catch (e: Exception) {
			cancelSequence(transmissionId)
		}


		while (openSequences.size > maxOpenSequences) {
			openSequences.remove(
				openSequences.minByOrNull { it.value.openedAt }?.key
			)
		}
	}
}
