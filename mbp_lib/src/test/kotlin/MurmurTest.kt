import com.github.eprst.murmur3.HashingSink128
import com.github.eprst.murmur3.MurmurHash3

import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import kotlin.test.assertContentEquals

class MurmurTest {

	@Test
	fun testEmptyString() {
		val should = "00000000000000000000000000000000".decodeHex()
		val s = HashingSink128(0)
		s.putString("", StandardCharsets.UTF_8);
		val actual = s.finish().asBigEndianByteArray()
		assertContentEquals(should, actual)
	}

	@Test
	fun testHello() {
		val should = "cbd8a7b341bd9b025b1e906a48ae1d19".decodeHex()
		val s = HashingSink128(0)
		s.putString("hello", StandardCharsets.UTF_8);
		val actual = s.finish().asBigEndianByteArray()
		assertContentEquals(should, actual)
	}

	@Test
	fun testHelloWorld() {
		val should = "342fac623a5ebc8e4cdcbc079642414d".decodeHex()
		val s = HashingSink128(0)
		s.putString("hello, world", StandardCharsets.UTF_8);
		val actual = s.finish().asBigEndianByteArray()
		assertContentEquals(should, actual)
	}



	fun String.decodeHex(): ByteArray {
		check(length % 2 == 0) { "Must have an even length" }

		return chunked(2)
			.map { it.toInt(16).toByte() }
			.toByteArray()
	}
}
