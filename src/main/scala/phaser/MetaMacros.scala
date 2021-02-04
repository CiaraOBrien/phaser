package phaser

import scala.quoted._
import cats.data.OneAnd

/** Ill-fated attempt to reduce boilerplate in declaring `Phunction`s by automatically deriving stage-1 lambdas
  * for stage-0 method calls. Maybe I'll figure this out one day. */
object MetaMacros {

  

  inline def liftEta[A : Type, B : Type](inline f: A => B)(using q: Quotes): Expr[A] => Expr[B] = {
    import q.reflect._
    val (methName, ownerName) = liftEtaImpl(f)
    val methTree: TermRef = Symbol.requiredModule(methName).owner.moduleClass.tree.asInstanceOf[Term].tpe.asInstanceOf[TermRef]
    (e: Expr[A]) => Ref.term(methTree).appliedTo(e.asTerm).asExprOf[B]
  }

  type EtaContraband = (String, Option[String])

  private inline def liftEtaImpl[A, B](inline f: A => B)(using q: Quotes): EtaContraband = ${ liftEtaMacro('f) }
  private def liftEtaMacro[A : Type, B : Type](f: Expr[A => B])(using q: Quotes): Expr[EtaContraband] =
    import quotes.reflect._
    def body(tree: Term): List[Symbol] = tree match {
      case Select(t, s) => s        :: body(t)
      case Ident (t   ) => t.symbol :: Nil
      case _ => report.error(s"""More complex function lifting is not yet supported."""); ???
    }
    def rec(tree: Term): (TypeRepr, List[Symbol]) = tree match {
      case Lambda(_, Apply(r, _)) => (r.tpe, body(r))
      case Block(List(), e)       => rec(e)
      case Typed(e, _)            => rec(e)
      case Inlined(_, List(), e)  => rec(e)
      case _  => report.error(s"""Unable to lift eta-expanded method call "${f.show}", "
        + "make sure it is a simple invocation of a method or lambda available in this stage."""); ???
    }
    val init = f.asTerm.underlying
    val symbols: List[Symbol] = rec(init)
    val tpe: TypeRepr = init.tpe
    val methTpe: TypeRepr = methRef.tpe
    val ownerRef: Option[Term] = methRef match
      case Select(qual, _) => Some(qual)
      case _ => None
    val ownerTpe: Option[TypeRepr] = ownerRef.map(_.tpe)
    println(methRef)
    println(methTpt)
    Expr((methTpe.show, ownerTpe.map(_.show)))