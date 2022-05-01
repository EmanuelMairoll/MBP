import java.io.File
import java.net.InetAddress

fun main(args: Array<String>) {
	if (args.size !in 2..4) {
		println("Usage: program <path to file> <receiver ip> [port] [max chunk size]")
		return
	}

	val fileToTransfer = File(args[0])
	if (!fileToTransfer.exists() || fileToTransfer.isDirectory) {
		println("File not found!")
		return
	}

	val receiverIp = InetAddress.getByName(args[1])
	val port = if (args.size in 3..4) args[2].toInt() else 6969
	val maxChunkSize = if (args.size == 4) args[3].toInt() else 500

	Sender(fileToTransfer, receiverIp, port, maxChunkSize).send()
}
