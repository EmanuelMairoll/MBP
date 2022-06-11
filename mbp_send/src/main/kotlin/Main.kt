import java.io.File
import java.net.InetAddress

fun main(args: Array<String>) {
	if (args.size !in 2..5) {
		println("Usage: program <path to file> <receiver ip> [port] [chunk size] [retransmission delay]")
		return
	}

	val fileToTransfer = File(args[0])
	if (!fileToTransfer.exists() || fileToTransfer.isDirectory) {
		println("File not found!")
		return
	}

	val receiverIp = InetAddress.getByName(args[1])
	val port = if (args.size >= 3) args[2].toInt() else 6969
	val chunkSize = if (args.size >= 4) args[3].toInt() else 1400

	Sender(
		fileToTransfer,
		receiverIp,
		port,
		chunkSize,
	).send()
}
