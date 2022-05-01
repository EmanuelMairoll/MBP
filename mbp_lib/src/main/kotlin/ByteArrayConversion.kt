import com.github.eprst.murmur3.MurmurHash3.HashCode128

fun ByteArray.getULongAt(idx: Int) =
	((this[idx + 7].toULong() and 0xFFu) shl 56) or
			((this[idx + 6].toULong() and 0xFFu) shl 48) or
			((this[idx + 5].toULong() and 0xFFu) shl 40) or
			((this[idx + 4].toULong() and 0xFFu) shl 32) or
			((this[idx + 3].toULong() and 0xFFu) shl 24) or
			((this[idx + 2].toULong() and 0xFFu) shl 16) or
			((this[idx + 1].toULong() and 0xFFu) shl 8) or
			((this[idx + 0].toULong() and 0xFFu) shl 0)

fun ByteArray.setULongAt(idx: Int, uLong: ULong) {
	this[idx + 7] = ((uLong shr 56) and 0xFFu).toByte()
	this[idx + 6] = ((uLong shr 48) and 0xFFu).toByte()
	this[idx + 5] = ((uLong shr 40) and 0xFFu).toByte()
	this[idx + 4] = ((uLong shr 32) and 0xFFu).toByte()
	this[idx + 3] = ((uLong shr 24) and 0xFFu).toByte()
	this[idx + 2] = ((uLong shr 16) and 0xFFu).toByte()
	this[idx + 1] = ((uLong shr 8) and 0xFFu).toByte()
	this[idx + 0] = ((uLong shr 0) and 0xFFu).toByte()
}

fun ByteArray.getUIntAt(idx: Int) =
	((this[idx + 3].toUInt() and 0xFFu) shl 24) or
			((this[idx + 2].toUInt() and 0xFFu) shl 16) or
			((this[idx + 1].toUInt() and 0xFFu) shl 8) or
			((this[idx + 0].toUInt() and 0xFFu) shl 0)

fun ByteArray.setUIntAt(idx: Int, uInt: UInt) {
	this[idx + 3] = ((uInt shr 24) and 0xFFu).toByte()
	this[idx + 2] = ((uInt shr 16) and 0xFFu).toByte()
	this[idx + 1] = ((uInt shr 8) and 0xFFu).toByte()
	this[idx + 0] = ((uInt shr 0) and 0xFFu).toByte()
}


fun ByteArray.getUByteAt(idx: Int) =
	(this[idx].toUByte() and 0xFFu)

fun ByteArray.setUByteAt(idx: Int, uByte: UByte) {
	this[idx] = uByte.toByte()
}


fun ByteArray.getStringAt(ind: Int, length: Int) = String(this, ind, length)

fun ByteArray.setStringAt(ind: Int, string: String) {
	string.toByteArray().copyInto(this, ind)
}


fun ByteArray.getByteArrayAt(ind: Int, length: Int) = copyOfRange(ind, ind + length)

fun ByteArray.setByteArrayAt(ind: Int, array: ByteArray) {
	array.copyInto(this, ind)
}

fun ByteArray.toHex(): String = joinToString(separator = " ") { eachByte -> "%02x".format(eachByte) }

/*
fun LongArray.asBigEndianByteArray(): ByteArray {
	val bytes = ByteArray(this.size * 8)

	this.forEachIndexed { i, long ->
		bytes[8 * i + 0] = ((long shr 56) and 0xFF).toByte()
		bytes[8 * i + 1] = ((long shr 48) and 0xFF).toByte()
		bytes[8 * i + 2] = ((long shr 40) and 0xFF).toByte()
		bytes[8 * i + 3] = ((long shr 32) and 0xFF).toByte()
		bytes[8 * i + 4] = ((long shr 24) and 0xFF).toByte()
		bytes[8 * i + 5] = ((long shr 16) and 0xFF).toByte()
		bytes[8 * i + 6] = ((long shr 8) and 0xFF).toByte()
		bytes[8 * i + 7] = ((long shr 0) and 0xFF).toByte()
	}

	return bytes
}
 */

fun HashCode128.asBigEndianByteArray(): ByteArray {
	val longs = longArrayOf(val1, val2)
	val bytes = ByteArray(longs.size * 8)

	longs.forEachIndexed { i, long ->
		bytes[8 * i + 0] = ((long shr 56) and 0xFF).toByte()
		bytes[8 * i + 1] = ((long shr 48) and 0xFF).toByte()
		bytes[8 * i + 2] = ((long shr 40) and 0xFF).toByte()
		bytes[8 * i + 3] = ((long shr 32) and 0xFF).toByte()
		bytes[8 * i + 4] = ((long shr 24) and 0xFF).toByte()
		bytes[8 * i + 5] = ((long shr 16) and 0xFF).toByte()
		bytes[8 * i + 6] = ((long shr 8) and 0xFF).toByte()
		bytes[8 * i + 7] = ((long shr 0) and 0xFF).toByte()
	}

	return bytes
}
