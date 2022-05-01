import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PacketTest {

	@Test
	fun testInfoPacketSerialisation() {
		val toSerialize = Packet(5u, 1u, InfoPacketBody(100u, "test"))

		val serialized = toSerialize.serialize()

		val deserialized = Packet(serialized)

		assertEquals(toSerialize, deserialized)
	}

	@Test
	fun testDataPacketSerialisation() {
		val toSerialize = Packet(5u, 1u, DataPacketBody(byteArrayOf(0x0a, 0x0b, 0x0c, 0x0d, 0x0e)))

		val serialized = toSerialize.serialize()

		val deserialized = Packet(serialized)

		assertEquals(toSerialize, deserialized)
	}

	@Test
	fun testFinalizePacketSerialisation() {
		val toSerialize = Packet(5u, 1u, FinalizePacketBody(byteArrayOf(0x1a, 0x1b, 0x1c, 0x1d, 0x1e)))

		val serialized = toSerialize.serialize()

		val deserialized = Packet(serialized)

		assertEquals(toSerialize, deserialized)
	}

}
