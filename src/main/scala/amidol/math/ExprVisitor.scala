package amidol.math

trait ExprVisitor {

  def visit[A](expr: Expr[A]): Unit = expr match {
    case e: Not => visitNot(e)
    case e: And => visitAnd(e)
    case e: Or => visitOr(e)
    case e: Comparision => visitComparision(e)
    case e: Plus => visitPlus(e)
    case e: Mult => visitMult(e)
    case e: Negate => visitNegate(e)
    case e: Inverse => visitInverse(e)
    case e: Variable => visitVariable(e)
    case e: Literal[_] => visitLiteral(e)
    case e: Sin => visitSin(e)
    case e: Max => visitMax(e)
    case e: DataSeries => visitDataSeries(e)
  }

  def visitNot(not: Not): Unit = {
    visit(not.e)
  }

  def visitAnd(and: And): Unit = {
    visit(and.lhs)
    visit(and.rhs)
  }

  def visitOr(or: Or): Unit = {
    visit(or.lhs)
    visit(or.rhs)
  }

  def visitComparision(comp: Comparision): Unit = {
    visit(comp.lhs)
    visit(comp.rhs)
  }

  def visitPlus(plus: Plus): Unit = {
    visit(plus.lhs)
    visit(plus.rhs)
  }

  def visitMult(mult: Mult): Unit = {
    visit(mult.lhs)
    visit(mult.rhs)
  }

  def visitNegate(neg: Negate): Unit = {
    visit(neg.e)
  }

  def visitInverse(inv: Inverse): Unit = {
    visit(inv.e)
  }

  def visitVariable(variable: Variable): Unit = ()

  def visitLiteral(lit: Literal[_]): Unit = ()

  def visitSin(sin: Sin): Unit = {
    visit(sin.arg)
  }

  def visitMax(max: Max): Unit = {
    visit(max.lhs)
    visit(max.rhs)
  }

  def visitDataSeries(series: DataSeries): Unit = ()
}

