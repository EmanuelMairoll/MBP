sealed class PacketBody

////////////// Sender -> Receiver //////////////

data class InfoPacketBody(val fileSize: ULong, val fileName: String) : PacketBody()

data class DataPacketBody(val data: ByteArray) : PacketBody() {
	override fun equals(other: Any?): Boolean =
		(other is DataPacketBody) && (data.contentEquals((other as DataPacketBody).data))

	override fun hashCode(): Int = data.contentHashCode()

	override fun toString(): String = "DataPacketBody(data=[${data.toHex()}])"
}

data class FinalizePacketBody(val murmurHash3: ByteArray) : PacketBody() {
	override fun equals(other: Any?): Boolean =
		(other is FinalizePacketBody) && (murmurHash3.contentEquals((other as FinalizePacketBody).murmurHash3))

	override fun hashCode(): Int = murmurHash3.contentHashCode()

	override fun toString(): String = "FinalizePacketBody(murmurHash3=[${murmurHash3.toHex()}])"
}

////////////// Receiver -> Sender //////////////

object AckPacketBody : PacketBody()

data class ErrorPacketBody(val reason: String) : PacketBody()

////////////// Packet Header //////////////

const val HEADER_SIZE = 6

data class Packet(val seqNr: UInt, val uid: UByte, val packetBody: PacketBody) {

	constructor(data: ByteArray, len: Int = data.size) : this(
		data.getUIntAt(0),
		data.getUByteAt(4),
		when (data.getUByteAt(5).toUInt()) {
			0x00u -> InfoPacketBody(
				data.getULongAt(HEADER_SIZE),
				data.getStringAt(HEADER_SIZE + 8, len - (HEADER_SIZE + 8))
			)
			0x01u -> DataPacketBody(
				data.getByteArrayAt(HEADER_SIZE, len - HEADER_SIZE)
			)
			0xFDu -> ErrorPacketBody(
				data.getStringAt(HEADER_SIZE, len - HEADER_SIZE)
			)
			0xFEu -> AckPacketBody
			0xFFu -> FinalizePacketBody(
				data.getByteArrayAt(HEADER_SIZE, len - HEADER_SIZE)
			)
			else -> throw Exception("unknown packet type")
		}
	)

	fun serialize(): ByteArray {
		val size = when (packetBody) {
			is InfoPacketBody -> HEADER_SIZE + 8 + packetBody.fileName.length
			is DataPacketBody -> HEADER_SIZE + packetBody.data.size
			is ErrorPacketBody -> HEADER_SIZE + packetBody.reason.length
			is AckPacketBody -> HEADER_SIZE
			is FinalizePacketBody -> HEADER_SIZE + packetBody.murmurHash3.size
		}

		val bytes = ByteArray(size)
		bytes.setUIntAt(0, seqNr)
		bytes.setUByteAt(4, uid)

		when (packetBody) {
			is InfoPacketBody -> {
				bytes.setUByteAt(HEADER_SIZE - 1, 0x00u)
				bytes.setULongAt(HEADER_SIZE, packetBody.fileSize)
				bytes.setStringAt(HEADER_SIZE + 8, packetBody.fileName)
			}
			is DataPacketBody -> {
				bytes.setUByteAt(HEADER_SIZE - 1, 0x01u)
				bytes.setByteArrayAt(HEADER_SIZE, packetBody.data)
			}
            is ErrorPacketBody -> {
                bytes.setUByteAt(HEADER_SIZE - 1, 0xFEu)
                bytes.setStringAt(HEADER_SIZE, packetBody.reason)
            }
			AckPacketBody -> {
                bytes.setUByteAt(HEADER_SIZE - 1, 0xFEu)
            }
			is FinalizePacketBody -> {
				bytes.setUByteAt(HEADER_SIZE - 1, 0xFFu)
				bytes.setByteArrayAt(HEADER_SIZE, packetBody.murmurHash3)
			}
		}

		return bytes
	}
}

