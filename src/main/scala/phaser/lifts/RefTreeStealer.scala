package phaser.lifts

import scala.quoted._

object RefTreeStealer {

  val error = """More-complex function lifting is not yet supported. Currently, only implicitly-eta-expanded method calls"
                |are available for automatic lifting. If you need a more complex function and can't express it as a static method,"
                |You can supply both the A => B and Expr[A] => Expr[B] by hand yourself.""".stripMargin

  def steal(using q: Quotes)(tree: q.reflect.Term): List[q.reflect.Symbol] = 
    import q.reflect._; tree match
      case s @  Select(t, _) => s.symbol :: steal(t)
      case i:   Ident        => i.symbol :: Nil
      case _ => report.error(error); ???

  def pack(using q: Quotes)(syms: List[q.reflect.Symbol]): List[RefStep] = 
    import q.reflect._
    def typeHint(s: Symbol): String = {
      val str = s.toString.nn
      str.substring(0, str.length() - s.name.length()).trim.nn
    }
    syms.map(s => RefStep(s.name, typeHint(s), s.fullName))

  def buildTree(steps: List[RefStep])(using q: Quotes): q.reflect.Ref = 
    import q.reflect._
    Ref.term(steps.foldRight { TypeIdent(defn.RootClass).tpe } {
      (step: RefStep, tpe: TypeRepr) => TermRef(tpe, step.name)
    }.asInstanceOf[TermRef])

  final case class RefStep(name: String, typeHint: String, mangledName: String)
  given RefStepToExpr(using ToExpr[String]): ToExpr[RefStep] with
    def apply(rs: RefStep)(using Quotes): Expr[RefStep] =
      '{RefStep(${Expr(rs.name)}, ${Expr(rs.typeHint)}, ${Expr(rs.mangledName)})}

}