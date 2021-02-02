import scala.quoted._
import phaser._

inline def lowerTest(inline str: String): Int = ${lowerTestMacro('str)}
def lowerTestMacro(str: Expr[String])(using Quotes, Type[String], Type[Int]): Expr[Int] = {
  import quotes.reflect._

  val f = Phunction((s: String) => s.length, (e: Expr[String]) => '{ $e.length }).compose(Phunction(Foo.quux, '{Foo.quux}))
  println(f)
  val p1 = Phaser.lift(str)
  println(p1.toString + " => " + f(p1).toString)
  val p2 = Phaser("wow")
  println(p2.toString + " => " + f(p2).toString)

  val l1 = f(p1)
  val l2 = f(p2)
  val fc = Phunction((i1: Int, i2: Int) => i1 + i2, (i1: Expr[Int], i2: Expr[Int]) => '{$i1 + $i2})
  println(fc)
  fc(l1, l2).defer.expr
}

