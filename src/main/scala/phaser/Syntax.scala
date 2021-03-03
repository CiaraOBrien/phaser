package phaser

import scala.quoted.*

extension [A : Type : FromExpr](e: Expr[A])(using Quotes)
  def require(desc: String, validate: A => Boolean = ((a: A) => true), as: String = "failed to validate"): Static[A] = 
    e.value.map(Static.apply) match 
      case Some(s: Static[A]) => if validate(s.value) then s else 
        quotes.reflect.report.error(s"$desc $as.", e); ???
      case _ => quotes.reflect.report.error(s"$desc must be static.", e); ???
  def phase: Phaser[A] = e.value.map(Static.apply).getOrElse(Dynamic(e))

extension [A : Type](e: Expr[A])(using Quotes) def defer: Dynamic[A] = Dynamic(e)