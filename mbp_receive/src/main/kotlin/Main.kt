import java.io.File

fun main(args: Array<String>) {
	if (args.size !in 1..2) {
		println("Usage: program <path to dropoff folder> [port]")
		return
	}

	val dropoffFolder = File(args[0])
	if (dropoffFolder.exists() && !dropoffFolder.isDirectory) {
		println("File exists!")
		return
	}
	dropoffFolder.mkdirs()

	val port = if (args.size == 2) args[1].toInt() else 6969

	Receiver(dropoffFolder, port).start()
}
