package phaser.lifts

import scala.quoted._

object RefTreeStealer {

  def newFullName(using q: Quotes)(s: q.reflect.Symbol): String =
    import q.reflect._
    if s.flags.is(Flags.Package) then s.fullName
    else if s.isClassDef then
      if s.flags.is(Flags.Module) then
        if s.name == "package$" then newFullName(s.owner)
        else newFullName(s.owner) + "." + s.name.stripSuffix("$")
      else newFullName(s.owner) + "." + s.name
    else newFullName(s.owner)

  def steal(using q: Quotes)(tree: q.reflect.Term): List[q.reflect.Symbol] = 
    import q.reflect._; tree match
      case s @  Select(t, _) => println(s"Stole Select(${s.symbol.fullName})"); s.symbol :: steal(t)
      case i:   Ident        => println(s"Stole Ident(${i.symbol.fullName})"); i.symbol :: Nil
      case e => report.error(s"""More-complex function lifting is not yet supported. Currently, only implicitly-eta-expanded method calls"
                |are available for automatic lifting. If you need a more complex function and can't express it as a static method,"
                |You can supply both the A => B and Expr[A] => Expr[B] by hand yourself. Failed at ${e.show(using Printer.TreeStructure)}""".stripMargin); ???

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
  
  inline def materialize[F](inline symStr: String): F = ${ materializeImpl('symStr) }

  private def materializeImpl[F : Type](symStr: Expr[String])(using Quotes): Expr[F] = {
    import quotes.reflect._
    println(symStr.asTerm)
    Ref(Symbol.requiredModule(symStr.value.get)).asExprOf[F]
  }

}