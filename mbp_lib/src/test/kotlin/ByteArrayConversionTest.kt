import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayConversionTest {

	@Test
	fun testUIntConversion(){
		val toConvert = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
		val converted = ByteArray(8)

		val i0 = toConvert.getUIntAt(0)
		val i4 = toConvert.getUIntAt(4)

		converted.setUIntAt(0, i0)
		converted.setUIntAt(4, i4)

		assertArrayEquals(toConvert, converted)
	}

	@Test
	fun testUIntEndianness(){
		val bytes = byteArrayOf(0x0d, 0x0c, 0x0b, 0x0a)
		val littleEndian = bytes.getUIntAt(0)

		assertEquals(0x0a0b0c0du, littleEndian)
	}
}
