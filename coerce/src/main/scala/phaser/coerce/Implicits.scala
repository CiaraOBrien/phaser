package phaser.coerce

trait Coerce[-A, +B] extends Conversion[A, B] {
  def apply(a: A): B = fallback(a)
  private def fallback(a: A): B = throw new Error("""Coerce called at runtime""".stripMargin)
}

given BooleanToString: Coerce[Boolean,      String] with
  override inline def apply(expr: Boolean): String = ${ booleanToString('expr) }
given ByteToString: Coerce[Byte,            String] with 
  override inline def apply(expr: Byte):    String = ${ byteToString('expr) }
given ShortToString: Coerce[Short,          String] with
  override inline def apply(expr: Short):   String = ${ shortToString('expr) }
given IntToString: Coerce[Int,              String] with
  override inline def apply(expr: Int):     String = ${ intToString('expr) }
given LongToString: Coerce[Long,            String] with
  override inline def apply(expr: Long):    String = ${ longToString('expr) }
given FloatToString: Coerce[Float,          String] with
  override inline def apply(expr: Float):   String = ${ floatToString('expr) }
given DoubleToString: Coerce[Double,        String] with
  override inline def apply(expr: Double):  String = ${ doubleToString('expr) }