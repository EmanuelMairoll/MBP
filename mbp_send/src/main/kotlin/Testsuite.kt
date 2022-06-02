fun main() {
    val rec = "10.3.3.140"
    val ports = arrayOf("6969", "4200")
    val files = arrayOf("file_send/1M", "file_send/100M")
    val payloadSizes = arrayOf(1000, 16000, 64000)
    val reps = 1..10

    for (port in ports) {
        for (file in files) {
            for (payload_size in payloadSizes) {
                for (rep in reps) {
                    if (file == "file_send/100M" && payload_size == 1000 && rep > 3) continue

                    val start = System.currentTimeMillis()
                    println("Params: $port $file $payload_size $rep at $start")
                    main(arrayOf(file, rec, port, (payload_size - 6).toString()))
                    println("Done, needed: ${System.currentTimeMillis() - start}m s")
                    Thread.sleep(1000)
                }
            }
        }

    }
}
