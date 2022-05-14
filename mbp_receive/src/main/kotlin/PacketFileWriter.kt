import com.github.eprst.murmur3.HashingSink128
import java.io.File

class PacketFileWriter(
	private val dropoffFolder: File,
	private val info: InfoPacketBody
) {
	private val f = run {
		val f = File(dropoffFolder, info.fileName)
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

}
