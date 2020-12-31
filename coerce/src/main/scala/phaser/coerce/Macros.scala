package phaser.coerce

import scala.quoted._

def booleanToString(expr: Expr[Boolean])(using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Boolean.toString(v)     )
  case _       =>      '{ java.lang.Boolean.toString($expr) }
def byteToString   (expr: Expr[Byte])   (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Byte.toString(v)     )
  case _       =>      '{ java.lang.Byte.toString($expr) }
def shortToString  (expr: Expr[Short])  (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Short.toString(v)     )
  case _       =>      '{ java.lang.Short.toString($expr) }
def intToString    (expr: Expr[Int])    (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Integer.toString(v)     )
  case _       =>      '{ java.lang.Integer.toString($expr) }
def longToString   (expr: Expr[Long])   (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Long.toString(v)     )
  case _       =>      '{ java.lang.Long.toString($expr) }
def floatToString  (expr: Expr[Float])  (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Float.toString(v)     )
  case _       =>      '{ java.lang.Float.toString($expr) }
def doubleToString (expr: Expr[Double]) (using Quotes): Expr[String] = expr.value match
  case Some(v) =>   Expr( java.lang.Double.toString(v)     )
  case _       =>      '{ java.lang.Double.toString($expr) }