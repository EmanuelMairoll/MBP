import java.io.File
import java.net.InetAddress

fun main(args: Array<String>) {
	if (args.size !in 2..5) {
		println("Usage: program <path to file> <receiver ip> [port] [max chunk size]")
		return
	}

	val fileToTransfer = File(args[0])
	if (!fileToTransfer.exists() || fileToTransfer.isDirectory) {
		println("File not found!")
		return
	}

	val receiverIp = InetAddress.getByName(args[1])
	val port = if (args.size in 3..5) args[2].toInt() else 6969
	val chunkSize = if (args.size in 4..5) args[3].toInt() else 1400
	val packetDelayUs = if (args.size in 5..5) args[4].toLong() else 100

	Sender(fileToTransfer, receiverIp, port, chunkSize, packetDelayUs).send()
}
