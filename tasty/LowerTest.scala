import scala.quoted._
import phaser._

import phaser.lifts.LiftPureEta.*
import cats.{Alternative, Monad, Monoid}
import cats.implicits.{*, given}
import cats.data.Chain
import cats.data.Chain.{*, given}

inline def lowerTest(inline dyn: Int, inline static: Int, inline str: String): String = ${lowerTestMacro('dyn, 'static, 'str)}
def lowerTestMacro(dyn: Expr[Int], static: Expr[Int], str: Expr[String])(using q: Quotes, s: Type[String], i: Type[Int]): Expr[String] = {
  import quotes.reflect.*
  import phaser.contraband.given
  
  def baz(s: String): String = "5" + s

  // FromExpr
  println('{Chain.concat(Chain(5, 6, $dyn, 8), Chain.one(7))}.value) // Should fail
  println('{Chain.concat(Chain(5, 6, $static, 8), Chain.one(7))}.value)
  println('{Chain.concat(Chain(5, 6, $static, 8), Chain.empty[Int])}.value)
  println('{Monoid[Chain[Int]].combine(Chain(5, 6, $dyn, 8), Chain.one(7))}.value) // Should fail
  println('{Monoid[Chain[Int]].combine(Chain(5, 6, $static, 8), Chain.one(7))}.value)
  println('{Alternative[Chain].combineK(Chain(5, 6, $dyn, 8), Chain.one(7))}.value) // Should fail
  println('{Alternative[Chain].combineK(Chain(5, 6, $static, 8), Chain.one(7))}.value)
  println('{Chain(2, 6, $dyn,    8, 9, 7).reverse}.value) // Should fail
  println('{Chain(2, 6, $static, 8, 9, 7).reverse}.value)
  println('{Chain(2, 6, $dyn,    8).append($static)}.value) // Should fail
  println('{Chain(2, 6, $static, 8).append($static)}.value)
  // ToExpr vs quoted vs FromExpr value
  println(Expr(Chain(1, 2, 3, 4, 5)).asTerm.show(using Printer.TreeAnsiCode))
  println(Expr(Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3))).asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3))}.asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3))}.value)

  println(Expr(Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3)).append(16)).asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3)).append(16)}.asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3)).append(16)}.value)

  println(Expr(Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3).reverse).append(16)).asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3).reverse).append(16)}.asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, 8), Chain(9, 52, 3).reverse).append(16)}.value)

  println(Expr(Alternative[Chain].combineK(Chain(5, 6, 3, 4), Chain(9, 52, 3).reverse).append(16)).asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, $dyn),      Chain(9, 52, 3).reverse).append(16)}.asTerm.show(using Printer.TreeAnsiCode))
  println(   '{Alternative[Chain].combineK(Chain(5, 6, 3, $dyn),      Chain(9, 52, 3).reverse).append(16)}.value) // Should fail
    
  //liftE(test.Foo.Bar.quux)(str)                       // - Yes
  //liftE(test.Foo.Bar.quux2)(int)                      // - Yes
  //liftE(test.Foo.quux)(str)                           // - Yes
  //liftE(baz)(str)                                     // - No - fails with a type error because it can't access it from _root_
  //liftE(test.asdf)(str)
  //liftE(qwerty)(str).show)                            // - No - fails with a type error because it can't access it from _root_
  //liftE((str: String) => str + "4")(str)
  //liftE((str: String) => test.Foo.Bar.quux(str))(str)
  //liftE((str: String) => str)(str)
  Expr("")
}

