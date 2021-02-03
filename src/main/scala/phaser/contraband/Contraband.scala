package phaser.contraband

import scala.quoted._
import cats.data.State

final class Patsy private[contraband] () {

}

private[contraband] def smuggle(erased s: State[List[String], String]): Patsy = new Patsy()

inline def putPatsy: Patsy = smuggle(State(l => (l, l.head)))

inline def innocentInline(inline p: Patsy, inline s: String): Patsy = ${ innocentMacro('p, 's) }
def innocentMacro(contraband: Expr[Patsy], message: Expr[String])(using Quotes): Expr[Patsy] =
  import quotes.reflect._
  contraband match 
  case '{ smuggle($state) } => println(Printer.TreeStructure.show(state.asTerm)); contraband
  case e => println(Printer.TreeStructure.show(e.asTerm)); contraband