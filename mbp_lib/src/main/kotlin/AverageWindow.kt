class AverageWindow(private val capacity: Int) {

	private val values = mutableListOf<Int>()

	fun put(value: Int) {
		values.add(value)

		while (values.size > capacity) {
			values.removeFirst()
		}
	}

	fun calcAverage() = values.average()
}
