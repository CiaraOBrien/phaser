package phaser

import scala.quoted._

import cats.arrow.Arrow

/** The equivalent of a function for `Phaser`, because due to the `Option`ality of `FromExpr`, `Phaser`
  * does not form a lawful functor unless you throw away everything useful about it. 
  * All `Phunction`s must possess an `Expr[A] => Expr[B]`, called a "beta" stage-1 lambda.
  * This may be passed into an alternate constructor as an `Expr[A => B]`, or "eta" lambda, which is automatically 
  * beta-reduced where possible. Beta lambdas are probably "better", but the difference is small enough that it's
  * liable to be entirely optimized away long before runtime. `Static`s also possess stage-0 lambdas, `A => B`,
  * so they can operate on both stage-0 and stage-1 values. `Kleisli`s have mixed-stage lambdas, `A => Phaser[B]`,
  * which are more flexible but potentially more annoying. `Phunction`s compose very well, and that is the intended
  * way of using them in general. */
enum Phunction[A : Type, B : Type](val beta: Expr[A] => Expr[B])(using Quotes) {

  /** Possesses a stage-0 (pure) lambda, `A => B`, alongside its "beta" stage-1 lambda, so it preserves the `Static`ity
    * of `Static` `Phaser`s where possible. */
  case Static [A : Type, B : Type](f: A => B,         fe: Expr[A] => Expr[B])(using Quotes) extends Phunction[A, B](fe)
  /** Possesses a mixed-stage (effectful) lambda, `A => Phaser[B]`, alongside its "beta" stage-1 lambda, so it can react to
    * the value present in a `Static` `Phaser` and determine the outcome (`Static` or `Dynamic`) of the computation based on that.
    * Technically monadic I suppose but that would be a pain in the ass to implement so we're using this which I guess is a Kleisli technically. */
  case Kleisli[A : Type, B : Type](k: A => Phaser[B], fe: Expr[A] => Expr[B])(using Quotes) extends Phunction[A, B](fe)
  /** Just has a "beta" stage-1 lambda, so it can only act on `Phaser`s by making them `Dynamic` and modifying them that way.
    * A `Dynamic` path could be saved with judicious application of a well-designed `FromExpr` but generally `Dynamic`s will
    * tend to propagate. */
  case Dynamic[A : Type, B : Type](                   fe: Expr[A] => Expr[B])(using Quotes) extends Phunction[A, B](fe)

  override def toString: String = 
    val an = Type.show[A]; val bn = Type.show[B]; this match 
      case Static (f, fe) => s"Phunction.Static($an => $bn, Expr[$an] => Expr[$bn])"
      case Kleisli(k, fe) => s"Phunction.Kleisli($an => Phaser[$bn], Expr[$an] => Expr[$bn])"
      case Dynamic(   fe) => s"Phunction.Static(Expr[$an] => Expr[$bn])"

  /** If this `Phunction` is a `Static`, returns its `A => B`, else returns `None`. */
  def lambda: Option[A => B] = this match
    case Static (f, _) => Some(f)
    case _ => None

  /** If this `Phunction` is a `Kleisli`, returns its `A => Phaser[B]`, else returns `None`. */
  def kleisli: Option[A => Phaser[B]] = this match
    case Kleisli (k, _) => Some(k)
    case _ => None

  /** Applies this `Phunction` to the given `Phaser[A]`. */
  def apply(p: Phaser[A])(using ToExpr[A]): Phaser[B] = p match 
    case s: phaser.Static[A]  => this match
      case Static (f, _) => s.map(f)
      case Kleisli(k, _) => k(s.value)
      case Dynamic(fe)   => s.defer.mapBeta(fe)
    case d: phaser.Dynamic[A] => d.mapBeta(beta)

  /** Composes two `Phunction`s into a new one, with this one applied first, a la `Function1.andThem`. */
  def andThen[C : Type](g: Phunction[B, C])(using ToExpr[A], ToExpr[B], ToExpr[C]): Phunction[A, C] = this match
    case Static(f1, fe1) => g match 
      case Static (f2, fe2) => Static (f1.andThen(f2),                                   fe1.andThen(fe2))
      case Kleisli(k2, fe2) => Kleisli(f1.andThen(k2),                                   fe1.andThen(fe2))
      case Dynamic(    fe2) => Kleisli(f1.andThen(phaser.Static.apply).andThen(g.apply), fe1.andThen(fe2))
    case Kleisli(k1, fe1) => g match 
      case Static (f2, fe2) => Kleisli(k1.andThen(g.apply), fe1.andThen(fe2))
      case Kleisli(k2, fe2) => Kleisli(k1.andThen(g.apply), fe1.andThen(fe2))
      case Dynamic(    fe2) => Dynamic(                     fe1.andThen(fe2))
    case Dynamic(    fe1) => Dynamic(fe1.andThen(g.beta))
  
  /** Composes two `Phunction`s into a new one, with this one applied last, a la `Function1.compose`. */
  def compose[AA : Type](g: Phunction[AA, A])(using ToExpr[AA], ToExpr[A], ToExpr[B]): Phunction[AA, B] = g.andThen(this)
  
}

object Phunction {
  // 1-ary
  def apply[A : Type, B : Type](lambda:  A =>        B,  beta: Expr[A] => Expr[B])(using Quotes): Static [A, B] = Static (lambda, beta)
  def apply[A : Type, B : Type](lambda:  A =>        B,  eta:  Expr[A  =>      B])(using Quotes): Static [A, B] = Static (lambda, reduce(eta))
  def apply[A : Type, B : Type](kleisli: A => Phaser[B], beta: Expr[A] => Expr[B])(using Quotes): Kleisli[A, B] = Kleisli(kleisli, beta)
  def apply[A : Type, B : Type](kleisli: A => Phaser[B], eta:  Expr[A  =>      B])(using Quotes): Kleisli[A, B] = Kleisli(kleisli, reduce(eta))
  def apply[A : Type, B : Type](                         beta: Expr[A] => Expr[B])(using Quotes): Dynamic[A, B] = Dynamic(beta)
  def apply[A : Type, B : Type](                         eta:  Expr[A  =>      B])(using Quotes): Dynamic[A, B] = Dynamic(reduce(eta))

  // 2-ary
  def apply[A1: Type, A2: Type, B: Type](lambda:  (A1, A2) =>        B,   beta: (Expr[A1], Expr[A2]) => Expr[B])(using Quotes): Static [(A1, A2), B] = 
    Static (lambda.tupled,  tupleBeta(beta))
  def apply[A1: Type, A2: Type, B: Type](lambda:  (A1, A2) =>        B,   eta:  Expr[(A1,       A2)  =>      B])(using Quotes): Static [(A1, A2), B] = 
    Static (lambda.tupled,  tupleEta(eta))
  def apply[A1: Type, A2: Type, B: Type](kleisli: (A1, A2) => Phaser[B],  beta: (Expr[A1], Expr[A2]) => Expr[B])(using Quotes): Kleisli[(A1, A2), B] = 
    Kleisli(kleisli.tupled, tupleBeta(beta))
  def apply[A1: Type, A2: Type, B: Type](kleisli: (A1, A2) => Phaser[B],  eta:  Expr[(A1,       A2)  =>      B])(using Quotes): Kleisli[(A1, A2), B] = 
    Kleisli(kleisli.tupled, tupleEta(eta))
  def apply[A1: Type, A2: Type, B: Type](beta: (Expr[A1], Expr[A2]) => Expr[B])(using Quotes): Dynamic[(A1, A2), B] = 
    Dynamic(                tupleBeta(beta))
  def apply[A1: Type, A2: Type, B: Type](eta:  Expr[(A1,       A2)  =>      B])(using Quotes): Dynamic[(A1, A2), B] = 
    Dynamic(                tupleEta(eta))
  
  extension [A1: Type, A2: Type, B: Type](pt: Phunction[(A1, A2), B]) {
    /** Convenience method for applying 2-ary `Phunction`s, exactly equivalent to `pt(p1.product(p2))`. */
    def apply(p1: Phaser[A1], p2: Phaser[A2])(using ToExpr[A1], ToExpr[A2]): Phaser[B] = pt(p1.product(p2))
  }

  /** Transforms an `Expr[A => B]` into an `Expr[A] => Expr[B]` efficiently by beta-reduction.
    * Thanks to this, it usually doesn't matter which you provide when creating your `Phunction`. */
  def reduce[A : Type, B : Type](eta: Expr[A => B])(using Quotes): Expr[A] => Expr[B] = (e: Expr[A]) => Expr.betaReduce('{$eta($e)})

  /** Enables `Expr[(A1, A2) => B]` to be used as `Expr[(A1, A2)] => Expr[B]`, including beta-reduction. */
  def tupleEta [A1 : Type, A2 : Type, B : Type](eta: Expr[( A1,       A2)  =>      B])(using Quotes): Expr[(A1, A2)] => Expr[B] = 
    (t: Expr[(A1, A2)]) => val (e1, e2) = untupleExpr(t)
      Expr.betaReduce('{$eta($e1, $e2)})

  /** Enables `(Expr[A1], Expr[A2]) => Expr[B]` to be used as `Expr[(A1, A2)] => Expr[B]`, very efficiently, not that it matters. */
  def tupleBeta[A1 : Type, A2 : Type, B : Type](beta: (Expr[A1], Expr[A2]) => Expr[B])(using Quotes): Expr[(A1, A2)] => Expr[B] = 
    (t: Expr[(A1, A2)]) => beta.tupled.apply(untupleExpr(t))

  /** Adapted from `Tuple2FromExpr`, distributes `Expr` over a 2-tuple interior or reports an error. */
  def untupleExpr[A1 : Type, A2 : Type](e: Expr[(A1, A2)])(using Quotes): (Expr[A1], Expr[A2]) = e match
    case '{ new Tuple2[A1, A2]($a1, $a2) } => Tuple2(a1, a2)
    case '{     Tuple2[A1, A2]($a1, $a2) } => Tuple2(a1, a2)
    case '{   (${a1}: A1) -> (${a2}: A2) } => Tuple2(a1, a2)
    case _ => quotes.reflect.report.error("Unable to untuple \"" + e.show + "\""); ???

}