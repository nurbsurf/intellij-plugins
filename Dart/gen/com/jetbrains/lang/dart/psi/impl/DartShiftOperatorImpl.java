// This is a generated file. Not intended for manual editing.
package com.jetbrains.lang.dart.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.lang.dart.DartTokenTypes.*;
import com.jetbrains.lang.dart.psi.*;
import com.jetbrains.lang.dart.util.DartPsiImplUtil;

public class DartShiftOperatorImpl extends DartPsiCompositeElementImpl implements DartShiftOperator {

  public DartShiftOperatorImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public DartShiftRightOperator getShiftRightOperator() {
    return findChildByClass(DartShiftRightOperator.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DartVisitor) ((DartVisitor)visitor).visitShiftOperator(this);
    else super.accept(visitor);
  }

}
