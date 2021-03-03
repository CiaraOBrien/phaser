package phaser.contraband

import scala.quoted.*
import cats.data.Chain

final class Patsy private[contraband] () {

}

private[contraband] def smuggle(erased contraband: Chain[String]): Patsy = new Patsy()

inline def putPatsy: Patsy = smuggle(Chain.empty)

inline def passMessage(inline p: Patsy, inline m: String): Patsy = ${ passMessageMacro('p, 'm) }
def passMessageMacro(patsy: Expr[Patsy], message: Expr[String])(using Quotes): Expr[Patsy] =
  import quotes.reflect.*
  patsy match 
    case '{ smuggle(${Expr(contraband)}) } => '{ smuggle(${Expr(contraband)}) }
    case e => patsy

given ChainFromExpr[A: Type](using f: FromExpr[A]): FromExpr[Chain[A]] with
  // Some of the type parameters on the right hand sides are unnecessary
  def unapply(a: Expr[Chain[A]])(using Quotes) = 
    import quotes.reflect.*
    import cats.data.Chain.{catsDataInstancesForChain as inst, catsDataMonoidForChain as monoid}
    a match 
    // An illustration of the limitations of `Varargs` and `Exprs`: we still need to ascribe to varargs
    // on both sides of the arrow, and `Exprs` pretty much only works alongside `Varargs`.
    // Sometimes : _* on the rhs doesn't work properly in my recollection, but seems to be fine now.
    // If you want to get a sequence of the expressions of the arguments, use Varargs(elems).
    // Varargs turns an Expr[Seq[A]], for example, `Seq(1, 2, 3): _*`, into a Seq[Expr[A], or vice versa.
    // Exprs can distribute Expr over the Seq once this is done, but it basically doesn't work
    // for most use cases, so it's recommended you just `map(_.value)` instead, outside of `Varargs`.
    // FromExpr[Seq[A]] is implemented using Varargs, while Varargs itself is implemented
    // with limited recursion over a TASTy tree with only the relevant extractors -
    // a very useful pattern for precise extraction of values from complex expressions.
    case '{ Chain.apply  [A](${ Varargs(Exprs(elems)) }*) } => Some(Chain.apply[A](elems*))
    // No need to invoke Varargs, just FromExpr[Seq[A]]
    case '{ Chain.fromSeq[A](${ Expr(elems) }) } => Some(Chain.fromSeq[A](elems))
    // These can be |'d together because we don't need to preserve any pattern params
    case '{ Chain.nil } | '{ Chain.empty[A] } 
       | '{ cats.Monoid[Chain[A]](monoid[A]).empty: Chain[A] } // Supporting typeclasses is always dicey, you will need to
       | '{ cats.Alternative[Chain](inst).empty[A]: Chain[A] } => Some(Chain.empty[A]) // check the output tree to determine what to do, 
         // the underlying encoding of these sorts of constructs varies greatly by their implementation in the
         // library in question and manner of use in the calling code.
    // Finally something easy!
    case '{ Chain.one[A](${Expr(elem)}) } => Some(Chain.one[A](elem))
    // Ultimately supporting this kind of stuff is a pointless endeavour, you should just worry about supporting
    // the syntax your users will be most immediately likely to use/copy-paste off your github readme/microsite tutorial
    case '{ cats.Monad      [Chain](inst).pure[A](${Expr(elem)}) } => Some(Chain.one[A](elem))
    case '{ cats.Alternative[Chain](inst).pure[A](${Expr(elem)}) } => Some(Chain.one[A](elem))
    // Not nearly as hard as it could've been, I suppose
    case '{ Chain.concat [A](${Expr(chain1)}, ${Expr(chain2)}) } => Some(Chain.concat[A](chain1, chain2))
    case '{ cats.Monoid[Chain[A]](monoid[A]).combine(${Expr(chain1)}: Chain[A], ${Expr(chain2)}: Chain[A]) } => Some(Chain.concat[A](chain1, chain2))
    // Until today, I never had occasion to care about Alternative at all, let alone that it extends MonoidK
    case '{ cats.Alternative[Chain](inst).combineK[A](${Expr(chain1)}, ${Expr(chain2)}) } => Some(Chain.concat[A](chain1, chain2))
    // Supporting operations on a value of the type rather than the companion object requires some extra specification.
    case '{ (${Expr(chain)}: Chain[A]).reverse } => Some(chain.reverse)
    // Append operations are very tricky due to typing issues, because we are parameterized on the type of
    // the result, not the input. The two are linked: `(_: Chain[T]).append[T2 >: T](x: T2): Chain[T2]`, so 
    // if we substitute in our A for their T2 and flip things around to match how we need to express it
    // in order to parameterize on A (since we can't parameterize on B as we only have A),
    // we get (_: Chain[B <: A]).append[A >: B](a: A): Chain[A].
    // For safety's sake, we at least check if pre, really an Expr[Chain[? <: A]], would form a valid 
    // Expr[Chain[A]] before applying our FromExpr[A] to it - the worst that can do is return None, I suppose.
    // You could also compare their TypeReprs for a more granular way of discriminating types.
    case '{ ($pre: Chain[? <: A]).append[A](${ Expr(post) }: A) } => if pre.isExprOf[Chain[A]]
                                                                   then pre.asExprOf[Chain[A]].value.map(_.append(post)) else None
    // Useful for building up and testing your FromExpr until it's robust, but obviously comment it out or whatever when done.
    case e => /*println(e.asTerm.show); println(e.asTerm.show(using Printer.TreeStructure));*/ None

// Implementing support for `filter`, `map`, etc. would either be impossible or require extensive, hacky, flakey tree-stealing
// to circumvent the Phase Consistency Principle, on top of requiring TypeRepr-based verification of types, 
// and is far, far beyond the scope of this document, or even what is reasonable to bother
// implementing - some operations are going to render the value-flow irreversibly dynamic, there is no way around this.

// ToExprs are almost invariably much easier, unless you're dealing with someone else's types and they've decided
// to design their API in an inherently obfuscatory way. Sadly we don't have access to the ADT so the best we can 
// do is flatten the Chain every time we convert to an expression. You could lift the major operations you want to apply into 
// Expr[Chain[A]] => Expr[Chain[B]], which would refrain from screwing up the Chain structure, at the marginal cost of
// forfieting your ability to do fully-phase-0 computations. Flattening means that a major part of the purpose of using a Chain 
// (amortized O(1) uncons) is probably somewhat compromised, but ideally a ToExpr/FromExpr would be implemented
// within the library itself, so wouldn't suffer from problems like these. Macros can inject calls to global private library 
// functions even if they're private from the calling site's perspective, just so long as the macro itself can access the functions.
// This is done by generating forwarders, but they're quite robust and seamless.
given ChainToExpr[A: Type](using t: ToExpr[A]): ToExpr[Chain[A]] with
  def apply(chain: Chain[A])(using Quotes): Expr[Chain[A]] =
    '{ Chain.apply[A](${Varargs(chain.iterator.map(t.apply).toList)}*) }
