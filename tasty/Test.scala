package test

object Foo {
  object Bar {
    def quux(str: String): String = str + "w"
    def quux2(i: Int): String = i.toString + "l"
  }
  def quux(str: String): String = str + "n"
  def strId(str: String): String = str
  def strLen(str: String): Int = str.length
  def sum(i1: Int, i2: Int): Int = i1 + i2
}

def asdf(str: String): String = str + "a"
