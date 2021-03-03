package phaser.lifts

import scala.quoted.*

import RefTreeStealer.*

object LiftPureEta {

  inline def liftE[A: Type, B: Type](inline f: A => B)(using q: Quotes): Expr[A] => Expr[B] =
    import q.reflect.*;
    (e: Expr[A]) => RefTreeStealer.buildTree(parseEtaCall(f)).appliedTo(e.asTerm).asExprOf[B]

  private inline def parseEtaCall[A, B](inline eta: A => B)(using q: Quotes): List[RefStep] = ${ parseEtaCallMacro('eta) }
  private def parseEtaCallMacro[A : Type, B : Type](eta: Expr[A => B])(using q: Quotes): Expr[List[RefStep]] =
    import quotes.reflect.*
    println(eta.show); println(eta.asTerm.show(using Printer.TreeStructure));
    def unwrapLambda(tree: Term): List[Symbol] = tree match 
      case Lambda(params, Apply(r, _)) => RefTreeStealer.steal(r)
      case Block(List(), e)      => unwrapLambda(e)
      case Typed(e, _)           => unwrapLambda(e)
      case Inlined(_, List(), e) => unwrapLambda(e)
      case e  => report.error(s"""Unable to lift eta-expanded method call "${eta.show}", 
        |make sure it is a simple invocation of a method or lambda available in this stage.
        |failed at: ${e.show(using Printer.TreeStructure)}""".stripMargin); ???
    Expr(RefTreeStealer.pack(unwrapLambda(eta.asTerm)))

}

object GenericFromExpr {

  inline def compileTime[T](inline expr: T)(using inline fe: FromExpr[T]): String = ${compileTimeImpl('expr, 'fe)}
  private def compileTimeImpl[T : Type, F <: FromExpr[T] : Type](expr: Expr[T], from: Expr[F])(using Quotes): Expr[String] = 
    import quotes.reflect.*
    def unwrap(tree: Term): TypeRepr = tree match 
      case i @ Ident(s)          => i.tpe
      case TypeApply(i, _)       => unwrap(i)
      case Block(List(), e)      => unwrap(e)
      case Typed(e, _)           => unwrap(e)
      case Inlined(_, List(), e) => unwrap(e)
    val unwrapped = unwrap(from.asTerm)
    println(unwrapped.classSymbol)
    println(unwrapped.typeSymbol.declaredMethods)
    println(unwrapped.termSymbol)
    println(Symbol.classSymbol(unwrapped.termSymbol.fullName))
    val t = summon[Type[F]]
    Expr("String")


}
