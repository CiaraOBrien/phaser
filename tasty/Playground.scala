import phaser.{_, given}

import phaser.contraband._
import phaser.lifts._
import scala.quoted._

object Playground {

  def main(args: Array[String]): Unit = {
    import FooGiven.given
    val test: String = "test"
    val dyn: Int = 6
    println(lowerTest(dyn, 5, "lmao"))
    val StringFromExpr = "5"
    //println(GenericFromExpr.compileTime[Foo](Foo("bruh", 5)))
  }

}

case class Foo(val str: String, val int: Int) {



}

object FooGiven {

  val dynInt = 9

  given FooFromExpr: FromExpr[Foo] with { // = new FromExpr[Foo] {
    def unapply(x: Expr[Foo])(using Quotes): Option[Foo] = x match
      case '{ Foo(${Expr(str: String)}, ${Expr(int: Int)}) } => Some(Foo(str, int))
      case _ => None
  }
    
  given FooToExpr: ToExpr[Foo] with
    def apply(foo: Foo)(using Quotes): Expr[Foo] = '{Foo(${Expr(foo.str)}, ${Expr(foo.int)})}

}