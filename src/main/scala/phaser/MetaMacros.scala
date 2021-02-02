package phaser

import scala.quoted._

/** Ill-fated attempt to reduce boilerplate in declaring `Phunction`s by automatically deriving stage-1 lambdas
  * for stage-0 method calls. Maybe I'll figure this out one day. */
object MetaMacros {

  inline def reverseEta[A : Type, B : Type](inline f: A => B)(using q: Quotes): Expr[A] => Expr[B] = {
    import q.reflect._
    val sym = reverseEtaImpl(f)
    println(Symbol.requiredMethod(sym).signature)
    val ref = Ref(Symbol.requiredMethod("quux"))
    val trA = TypeRepr.of[A]
    val trB = TypeRepr.of[B]
    (e: Expr[A]) => ref.appliedTo(e.asTerm).asExprOf[B]
  }

  private inline def reverseEtaImpl[A, B](inline f: A => B)(using q: Quotes): String = ${ reverseEtaMacro('f) }
  private def reverseEtaMacro[A : Type, B : Type](f: Expr[A => B])(using q: Quotes): Expr[String] =
    import quotes.reflect._
    def rec(tree: Term): Term = tree match
      case Apply(ref, _) => ref
      case Lambda(_, body) => rec(body)
      case Block(Nil, e) => rec(e)
      case Typed(e, _) => rec(e)
      case Inlined(_, Nil, e) => rec(e)
      case _  => report.error(s"""Unable to reverse-eta-expand "${f.show}", make sure it is a simple invocation of a method name available in this stage."""); ???
    val sym = rec(f.asTerm)
    Expr(sym.symbol.fullName)

}