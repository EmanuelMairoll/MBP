import com.github.eprst.murmur3.HashingSink128
import java.io.File
import java.util.concurrent.ThreadLocalRandom

class SequenceBuilder(
    private val fileToTransfer: File,
    private val chunkSize: Int,
) {
    fun build(): Sequence<Packet> {
        var seqNr = 0u
        val s = HashingSink128(0)
        val uid = ThreadLocalRandom.current().nextInt().toUByte()

        val info = sequence {
            yield(
                Packet(
                    seqNr++, uid, InfoPacketBody(
                        fileToTransfer.length().toULong(),
                        fileToTransfer.name
                    )
                )
            )
        }

        val data = fileToTransfer
            .chunkedSequence(chunkSize)
            .map { chunk ->
                s.putBytes(chunk)
                Packet(
                    seqNr++,
                    uid,
                    DataPacketBody(chunk)
                )
            }

        val finalize = sequence {
            yield(
                Packet(
                    seqNr++,
                    uid,
                    FinalizePacketBody(
                        s.finish().asBigEndianByteArray()
                    )
                )
            )
        }

        return info + data + finalize
    }

    private fun File.chunkedSequence(chunk: Int): Sequence<ByteArray> {
        val input = this.inputStream().buffered()
        val buffer = ByteArray(chunk)
        return generateSequence {
            val red = input.read(buffer)
            if (red >= 0) buffer.copyOf(red)
            else {
                input.close()
                null
            }
        }
    }
}