package org.jetbrains.plugins.scala.lang.transformation

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.transformation.annotations._
import org.jetbrains.plugins.scala.lang.transformation.calls._
import org.jetbrains.plugins.scala.lang.transformation.general._
import org.jetbrains.plugins.scala.lang.transformation.implicits._
import org.jetbrains.plugins.scala.lang.transformation.references._
import org.jetbrains.plugins.scala.lang.transformation.types._

/**
  * @author Pavel Fatin
  */
trait Transformer {
  // TODO return updated element instead of Boolean to enable fine-grained recursion
  def transform(e: PsiElement): Boolean
}

object Transformer {
  private val RecursionDepthThreshold = 10

  // TODO rely on a single set of transformers, use different means of ordering transformer applications
  // In principle, we can rely on the SelectionDialog's set of transformers to produce the equivalent result.
  // We use this second collection to minimize number of intermediate transformations by ordering the transformers.
  private val All = Seq(
    AppendSemicolon,
    ExpandFunctionType,
    ExpandTupleType,
    ExpandStringInterpolation,
    ExpandForComprehension,
    ExpandApplyCall,
    ExpandUpdateCall,
    ExpandUnaryCall,
    ExpandAssignmentCall,
    ExpandDynamicCall,
    CanonizeInfixCall,
    CanonizePostifxCall,
    CanonizeZeroArityCall,
    ExpandSetterCall,
    CanonizeBlockArgument,
    ExpandAutoTupling,
//    ExpandVarargArgument,
    ExpandTupleInstantiation,
    ExpandImplicitConversion,
    InscribeImplicitParameters,
    AddTypeToFunctionParameter,
    AddTypeToMethodDefinition,
    AddTypeToUnderscoreParameter,
    AddTypeToValueDefinition,
    AddTypeToVariableDefinition,
    AddTypeToReferencePattern,
    PartiallyQualifySimpleReference
  )

  // TODO use in debugger's "evaluate expression" to simpify its code and to support many language features automatically (e.g. string interpolation)
  // TODO support fine-grained recursion with dependencies
  def transform(file: PsiElement, range: Option[RangeMarker] = None, transformers: Set[Transformer] = All.toSet) {
    val enabledTransformers = All.filter(transformers)

    val resultIterator = Iterator.continually{
      enabledTransformers.flatMap { transformer =>
        val elementIterator = range.map(elementsIn(file, _)).getOrElse(file.depthFirst)
        elementIterator.map(transformer.transform).toVector
      }
    }

    resultIterator.takeWhile(_.contains(true)).take(RecursionDepthThreshold).toVector
  }

  private def elementsIn(file: PsiElement, range: RangeMarker): Iterator[PsiElement] = {
    val first = file.findElementAt(range.getStartOffset)
    val last = file.findElementAt(range.getEndOffset)
    val parent = PsiTreeUtil.findCommonParent(first, last)
    parent.depthFirst.filter(e => contains(range, e.getTextRange))
  }

  private def contains(marker: RangeMarker, range: TextRange): Boolean =
    range.getStartOffset >= marker.getStartOffset &&
      range.getEndOffset <= marker.getEndOffset
}