package phaser.lifts

import scala.quoted._

import RefTreeStealer._

object LiftPureEta {

  inline def liftE[A : Type, B : Type](inline f: A => B)(using q: Quotes): Expr[A] => Expr[B] =
    import q.reflect._; 
    (e: Expr[A]) => RefTreeStealer.buildTree(parseEtaCall(f)).appliedTo(e.asTerm).asExprOf[B]

  private inline def parseEtaCall[A, B](inline eta: A => B)(using q: Quotes): List[RefStep] = ${ parseEtaCallMacro('eta) }
  private def parseEtaCallMacro[A : Type, B : Type](eta: Expr[A => B])(using q: Quotes): Expr[List[RefStep]] =
    import quotes.reflect._
    def unwrapLambda(tree: Term): List[Symbol] = tree match 
      case Lambda(params, Apply(r, _)) => RefTreeStealer.steal(r)
      case Block(List(), e)      => unwrapLambda(e)
      case Typed(e, _)           => unwrapLambda(e)
      case Inlined(_, List(), e) => unwrapLambda(e)
      case _  => report.error(s"""Unable to lift eta-expanded method call "${eta.show}", 
        |make sure it is a simple invocation of a method or lambda available in this stage.""".stripMargin); ???
    Expr(RefTreeStealer.pack(unwrapLambda(eta.asTerm)))


}