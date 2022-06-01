import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.io.IOException

class PacketFileWriter(
	private val dropoffFolder: File,
	private val info: InfoPacketBody
) {
	private val f = run {
		val f = File(dropoffFolder, info.fileName)
		if (!isParent(dropoffFolder, f)) throw Transmission.ProtocolException
		f.delete()
		f
	}
	private val os = f.outputStream()
	private val h = HashingSink128(0)

	fun putData(data: DataPacketBody) {
		os.write(data.data)
		h.putBytes(data.data)
	}

	fun putFin(fin: FinalizePacketBody) {
		val hashShould = fin.murmurHash3
		val hashActual = h.finish().asBigEndianByteArray()

		if (hashShould contentEquals hashActual) {
			println("File successfully transferred")
			os.flush()
			os.close()

		} else {
			//println("Hashes do not match! Keeping corrupted file anyway!")
			println("Hashes do not match! Deleting corrupted file!")
			println("Should: ${hashShould[0]} ${hashShould[1]}")
			println("Actual: ${hashActual[0]} ${hashActual[1]}")
			f.delete()
		}
	}

	fun del(){
		os.close()
		f.delete()
	}

	private fun isParent(parent: File, file: File): Boolean {
		var p = parent
		var f: File?
		try {
			p = p.canonicalFile
			f = file.canonicalFile
		} catch (e: IOException) {
			return false
		}
		while (f != null) {
			// equals() only works for paths that are normalized, hence the need for
			// getCanonicalFile() above. "a" isn't equal to "./a", for example.
			if (p == f) {
				return true
			}
			f = f.parentFile
		}
		return false
	}

}
