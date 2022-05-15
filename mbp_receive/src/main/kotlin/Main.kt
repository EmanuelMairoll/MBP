import java.io.File

fun main(args: Array<String>) {
	if (args.size !in 1..3) {
		println("Usage: program <path to dropoff folder> [port] [retransmission delay]")
		return
	}

	val dropoffFolder = File(args[0])
	if (dropoffFolder.exists() && !dropoffFolder.isDirectory) {
		println("File exists!")
		return
	}
	dropoffFolder.mkdirs()

	val port = if (args.size >= 2) args[1].toInt() else 6969
	val retransmissionDelayMs = if (args.size >= 3) args[2].toInt() else 1000

	Receiver(dropoffFolder, port, retransmissionDelayMs).start()
}
