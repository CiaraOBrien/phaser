import minitest._
import scala.util.Random
import phaser.coerce.given

object PrimitiveTests extends TestSuite[Random] {

  def setup(): Random = new Random()
  def tearDown(r: Random): Unit = ()

  def str(s: String): String = s

  test("BooleanToString") { r =>
    assertEquals(str(true),  "true")
    assertEquals(str(false), "false")
    val rand = r.nextBoolean
    assertEquals(str(rand), rand.toString)
  }
  test("ByteToString") { r =>
    assertEquals(str(1: Byte), "1")
    assertEquals(str(-128: Byte), "-128")
    assertEquals(str(127: Byte), "127")
    val byte = 50: Byte
    assertEquals(str(byte), byte.toString)
  }
  test("IntToString") { r =>
    assertEquals(str(1), "1")
    assertEquals(str(-2147483648), "-2147483648")
    assertEquals(str(2147483647), "2147483647")
    val rand = r.nextInt
    assertEquals(str(rand), rand.toString)
  }
  
}