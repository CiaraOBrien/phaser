package phaser

import scala.quoted._

/** A datatype that, alongside `Phunction`, facilitates value-oriented metaprogramming, by allowing you to
  * abstract over the uncertainty and boilerplate of "hoisting" values from stage 1 to stage 0.
  * `FromExpr[A]` is needed to "hoist", that is, `Option`ally convert from `Static` to `Dynamic`.
  * `ToExpr[A]` is needed for pretty much everything else of use, so types lacking `ToExpr` are only really
  * useable as intermediate values, and even then, you can only `Static.map` over them, hopefully to a type
  * that actually has `ToExpr`. */
sealed trait Phaser[A : Type](using Quotes) {

  /** Returns `true` if this is `Static`, `false` if `Dynamic`. */
  def isStatic: Boolean = this match
    case Static (_) => true
    case Dynamic(_) => false
  
  /** Converts from `Static` (or `Dynamic`) to `Dynamic`. This is
    * guaranteed to succeed, and the canonical way to finalize the results of your macro
    * is to call `defer` on your final `Phaser` to make it `Dynamic`, then `expr` on that
    * to extract the underlying `Expr`. */
  def defer(using ToExpr[A]): Dynamic[A] = this match
    case Static(a)  => Dynamic(Expr(a))
    case d: Dynamic[A] => d

  /** Converts from `Dynamic` (or `Static`) to `Static`. This is not at all
    * guaranteed to succeed, if it fails it will return the original `Dynamic`, so you can
    * try `hoist`ing any `Phaser` you construct from an `Expr` input, and seamlessly proceed 
    * whether or not it succeeded. This does not happen automatcially, you have to either call
    * `hoist` on an existing `Phaser` or use `Phaser.lift` to lift `Expr`s. */
  def hoist(using FromExpr[A]): Phaser[A] = this match
    case s: Static[A] => s
    case Dynamic(e)   => e.value.map(Static.apply).getOrElse(this)

  /** If this is `Static(a)`, returns `Some(a)`, else returns `None`. */
  def toOption: Option[A] = this match
    case Static(a)  => Some(a)
    case Dynamic(e) => None

  /** If this is `Static(a)`, returns `Right(a)`, or if it's `Dynamic(e)`, returns `Left(e)`. */
  def toEither: Either[Expr[A], A] = this match
    case Static(a)  => Right(a)
    case Dynamic(e) => Left(e)
  
  /** Combines two `Phaser`s into one that carries a `Tuple2`, allowing for 2-ary `Phunction`s.
    * Since this is intended for combining `Phaser`s anyway, if either `Phaser` is `Dynamic`,
    * the result will be `Dynamic` too, as combining those inputs would lead to a `Dynamic` anyway. */
  def product[B : Type](p2: Phaser[B])(using ToExpr[A], ToExpr[B]): Phaser[(A, B)] = this match
    case Static (a1) => p2 match 
      case Static (a2) => Static((a1, a2))
      case Dynamic(e2) => Dynamic('{(${Expr(a1)}, $e2)})
    case Dynamic(e1) => p2 match 
      case Static (a2) => Dynamic('{($e1, ${Expr(a2)})})
      case Dynamic(e2) => Dynamic('{($e1, $e2)})

}

object Phaser {
  /** Wraps a stage-0 value in a `Static`. */
  def apply[A : Type](a:      A )(using Quotes): Phaser[A] = Static(a)
  /** Wraps a stage-1 value in a `Dynamic`. Does not implicitly attempt to hoist it to stage 0, for that use `hoist` or `Phaser.lift`. */
  def apply[A : Type](e: Expr[A])(using Quotes): Phaser[A] = Dynamic(e)
  /** Tries to hoist a stage-1 value into a stage-0 value and return it as a `Static`, if it can't be hoisted, return it unchanged in a `Dynamic`.
    * Equivalent to `Phaser.apply(expr).hoist`. */
  def lift [A : Type](e: Expr[A])(using Quotes, FromExpr[A]): Phaser[A] = e.value.map(Static.apply).getOrElse(Dynamic(e))
}
  
/** A `Phaser` that holds a value known at stage 0 (that is, macro expansion), therefore a pure value. */
final case class Static[A : Type](val value: A)(using Quotes) extends Phaser[A] {

  /** Maps a stage-0 lambda, that is, an `A => B`, over this `Static`.
    * Pretty limited since it can't abstract over `Dynamic`s, but it doesn't require `ToExpr`
    * so if you paint yourself into a corner, here you go. */
  def map[B : Type](f: A => B): Static[B] = Static(f(value))

}

final case class Dynamic[A : Type](val expr: Expr[A])(using Quotes) extends Phaser[A] {
  
  override def toString: String = if expr.show.length <= 100 then s"Dynamic(${expr.show})" else "Dynamic(...)"

  /** Maps an "eta" stage-1 lambda, that is, an `Expr[A => B]`, over this `Dynamic`, performing beta-reduction as needed. */
  def mapEta [B : Type](eta:  Expr[A  =>      B]): Dynamic[B] = Dynamic(Expr.betaReduce('{$eta($expr)}))

  /** Maps a "beta" stage-1 lambda, that is, an `Expr[A] => Expr[B]`, over this `Dynamic`, with no explicit beta-reduction
    * (as it's unnecessary), although the lambda may perform beta-reduction within itself. */
  def mapBeta[B : Type](beta: Expr[A] => Expr[B]): Dynamic[B] = Dynamic(beta(expr))

}