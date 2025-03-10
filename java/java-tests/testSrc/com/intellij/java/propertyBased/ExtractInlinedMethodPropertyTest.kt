// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.propertyBased

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.extractMethod.newImpl.MethodExtractor
import com.intellij.refactoring.inline.InlineMethodHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.SkipSlowTestLocally
import com.intellij.testFramework.propertyBased.MadTestingUtil
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.PropertyChecker
import org.jetbrains.plugins.groovy.lang.psi.util.isWhiteSpaceOrNewLine
import org.junit.Assume

@SkipSlowTestLocally
class ExtractInlinedMethodPropertyTest : BaseUnivocityTest() {

  fun testInlineExtractMethodCompilation() {
    val facade = JavaPsiFacade.getInstance(myProject)
    val allScope = GlobalSearchScope.allScope(myProject)
    Assume.assumeTrue("Maven import failed",
                      facade.findClass("org.testng.Assert", allScope) != null &&
                      facade.findClass("com.univocity.test.OutputTester", allScope) != null)

    val documentManger = PsiDocumentManager.getInstance(myProject) as PsiDocumentManagerImpl
    documentManger.disableBackgroundCommit(testRootDisposable)
    RecursionManager.disableMissedCacheAssertions(testRootDisposable)
    initCompiler()

    val fileGenerator = psiJavaFiles()
    PropertyChecker.customized().withIterationCount(30)
      .checkScenarios { inlineExtractMethodCompilation(fileGenerator) }
  }

  private fun inlineExtractMethodCompilation(javaFiles: Generator<PsiJavaFile>) = ImperativeCommand { env ->
    val file = env.generateValue(javaFiles, null)

    env.logMessage("Open file in editor: ${file.virtualFile.path}")
    val editor = FileEditorManager.getInstance(myProject)
                   .openTextEditor(OpenFileDescriptor(myProject, file.virtualFile), true)
                 ?: return@ImperativeCommand

    val methodCalls = methodCalls(file) ?: return@ImperativeCommand
    val methodCall = env.generateValue(methodCalls, null)
    val method = methodCall.resolveMethod() ?: return@ImperativeCommand
    val parentStatement = PsiTreeUtil.getParentOfType(methodCall, PsiStatement::class.java) ?: return@ImperativeCommand
    val rangeToExtract = createGreedyMarker(editor.document, parentStatement)

    MadTestingUtil.changeAndRevert(myProject) {
      val numberOfMethods = countMethodsInsideFile(file)
      val caret = methodCall.methodExpression.textRange.endOffset
      val logicalPosition = editor.offsetToLogicalPosition(caret)
      env.logMessage("Move caret to ${logicalPosition.line + 1}:${logicalPosition.column + 1}")
      editor.caretModel.moveToOffset(caret)

      ignoreRefactoringErrorHints {
        env.logMessage("Inline method call: ${methodCall.text}")
        InlineMethodHandler.performInline(myProject, editor, method, true)

        val range = TextRange(rangeToExtract.startOffset, rangeToExtract.endOffset)
        env.logMessage("Extract inlined lines: ${editor.document.getText(range)}")
        MethodExtractor().doExtract(file, range)
        require(numberOfMethods != countMethodsInsideFile(file)) { "Method is not extracted" }

        checkCompiles(myCompilerTester.make())
      }
    }
  }

  private fun ignoreRefactoringErrorHints(runnable: Runnable){
    try {
      runnable.run()
    } catch (_: CommonRefactoringUtil.RefactoringErrorHintException) {
    } catch (_: BaseRefactoringProcessor.ConflictsInTestsException) {
    }
  }

  private fun countMethodsInsideFile(file: PsiFile): Int {
    return PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java).size
  }

  private fun methodCalls(file: PsiFile): Generator<PsiMethodCallExpression>? {
    val methodCalls = PsiTreeUtil
      .findChildrenOfType(file, PsiMethodCallExpression::class.java)
      .filter { PsiTreeUtil.getParentOfType(it, PsiExpression::class.java) == null }
    if (methodCalls.isEmpty()) return null
    return Generator.sampledFrom(methodCalls)
  }

  private fun createGreedyMarker(document: Document, element: PsiElement): RangeMarker {
    val previousSibling = PsiTreeUtil.skipMatching(element, PsiElement::getPrevSibling, PsiElement::isWhiteSpaceOrNewLine)
    val nextSibling = PsiTreeUtil.skipMatching(element, PsiElement::getNextSibling, PsiElement::isWhiteSpaceOrNewLine)
    val start = (previousSibling ?: element.parent.firstChild).textRange.endOffset
    val end = (nextSibling ?: element.parent.lastChild).textRange.startOffset
    val rangeMarker = document.createRangeMarker(start, end)
    rangeMarker.isGreedyToLeft = true
    rangeMarker.isGreedyToRight = true
    return rangeMarker
  }

}