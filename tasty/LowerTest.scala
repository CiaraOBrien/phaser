import scala.quoted._
import phaser._

import phaser.lifts.LiftPureEta._

inline def lowerTest(inline int: Int, inline str: String): Int = ${lowerTestMacro('int, 'str)}
def lowerTestMacro(int: Expr[Int], str: Expr[String])(using q: Quotes, s: Type[String], i: Type[Int]): Expr[Int] = {
  import quotes.reflect._

  def baz(s: String): String = "5" + s

  println(liftE(test.Foo.Bar.quux)(str).show)
  println(liftE(test.Foo.Bar.quux2)(int).show)
  println(liftE(test.Foo.quux)(str).show)
  println(liftE(baz)(str).show)

  val f = Phunction((s: String) => s.length, (e: Expr[String]) => '{ $e.length }).compose(Phunction(test.Foo.Bar.quux, '{test.Foo.Bar.quux}))
  println(f)
  val p1 = str.require("String", _.nonEmpty, "must not be empty")
  println(p1.toString + " => " + f(p1).toString)
  val p2 = Phaser("wow")
  println(p2.toString + " => " + f(p2).toString)

  val l1 = f(p1)
  val l2 = f(p2)
  val fc = Phunction((i1: Int, i2: Int) => i1 + i2, (i1: Expr[Int], i2: Expr[Int]) => '{$i1 + $i2})
  println(fc)
  fc(l1, l2).defer.expr
}

