// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.perf.synthetic;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("performance-tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("../idea/tests/testData/highlighter")
public abstract class PerformanceHighlightingTestGenerated extends AbstractPerformanceHighlightingTest {
    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../idea/tests/testData/highlighter/deprecated")
    public static class Deprecated extends AbstractPerformanceHighlightingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doPerfTest, this, testDataFilePath);
        }

        @TestMetadata("Class.kt")
        public void testClass() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Class.kt");
        }

        @TestMetadata("ClassObject.kt")
        public void testClassObject() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/ClassObject.kt");
        }

        @TestMetadata("Constructor.kt")
        public void testConstructor() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Constructor.kt");
        }

        @TestMetadata("ExtensionFunction.kt")
        public void testExtensionFunction() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/ExtensionFunction.kt");
        }

        @TestMetadata("Function.kt")
        public void testFunction() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Function.kt");
        }

        @TestMetadata("Get.kt")
        public void testGet() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Get.kt");
        }

        @TestMetadata("Getter.kt")
        public void testGetter() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Getter.kt");
        }

        @TestMetadata("Inc.kt")
        public void testInc() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Inc.kt");
        }

        @TestMetadata("Invalid.kt")
        public void testInvalid() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Invalid.kt");
        }

        @TestMetadata("Invoke.kt")
        public void testInvoke() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Invoke.kt");
        }

        @TestMetadata("Operation.kt")
        public void testOperation() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Operation.kt");
        }

        @TestMetadata("Property.kt")
        public void testProperty() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Property.kt");
        }

        @TestMetadata("RangeTo.kt")
        public void testRangeTo() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/RangeTo.kt");
        }

        @TestMetadata("Setter.kt")
        public void testSetter() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Setter.kt");
        }

        @TestMetadata("SuperCall.kt")
        public void testSuperCall() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/SuperCall.kt");
        }

        @TestMetadata("Trait.kt")
        public void testTrait() throws Exception {
            runTest("../idea/tests/testData/highlighter/deprecated/Trait.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../idea/tests/testData/highlighter")
    public static class Uncategorized extends AbstractPerformanceHighlightingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doPerfTest, this, testDataFilePath);
        }

        @TestMetadata("Annotations.kt")
        public void testAnnotations() throws Exception {
            runTest("../idea/tests/testData/highlighter/Annotations.kt");
        }

        @TestMetadata("AutoCreatedItParameter.kt")
        public void testAutoCreatedItParameter() throws Exception {
            runTest("../idea/tests/testData/highlighter/AutoCreatedItParameter.kt");
        }

        @TestMetadata("DelegatingCtor.kt")
        public void testDelegatingCtor() throws Exception {
            runTest("../idea/tests/testData/highlighter/DelegatingCtor.kt");
        }

        @TestMetadata("Destructuring.kt")
        public void testDestructuring() throws Exception {
            runTest("../idea/tests/testData/highlighter/Destructuring.kt");
        }

        @TestMetadata("Dynamic.kt")
        public void testDynamic() throws Exception {
            runTest("../idea/tests/testData/highlighter/Dynamic.kt");
        }

        @TestMetadata("Enums.kt")
        public void testEnums() throws Exception {
            runTest("../idea/tests/testData/highlighter/Enums.kt");
        }

        @TestMetadata("Field.kt")
        public void testField() throws Exception {
            runTest("../idea/tests/testData/highlighter/Field.kt");
        }

        @TestMetadata("Functions.kt")
        public void testFunctions() throws Exception {
            runTest("../idea/tests/testData/highlighter/Functions.kt");
        }

        @TestMetadata("InaccessibleExpression.kt")
        public void testInaccessibleExpression() throws Exception {
            runTest("../idea/tests/testData/highlighter/InaccessibleExpression.kt");
        }

        @TestMetadata("InvokeCall.kt")
        public void testInvokeCall() throws Exception {
            runTest("../idea/tests/testData/highlighter/InvokeCall.kt");
        }

        @TestMetadata("JavaTypes.kt")
        public void testJavaTypes() throws Exception {
            runTest("../idea/tests/testData/highlighter/JavaTypes.kt");
        }

        @TestMetadata("KDoc.kt")
        public void testKDoc() throws Exception {
            runTest("../idea/tests/testData/highlighter/KDoc.kt");
        }

        @TestMetadata("KotlinInjection.kt")
        public void testKotlinInjection() throws Exception {
            runTest("../idea/tests/testData/highlighter/KotlinInjection.kt");
        }

        @TestMetadata("Labels.kt")
        public void testLabels() throws Exception {
            runTest("../idea/tests/testData/highlighter/Labels.kt");
        }

        @TestMetadata("NamedArguments.kt")
        public void testNamedArguments() throws Exception {
            runTest("../idea/tests/testData/highlighter/NamedArguments.kt");
        }

        @TestMetadata("NonNullAssertion.kt")
        public void testNonNullAssertion() throws Exception {
            runTest("../idea/tests/testData/highlighter/NonNullAssertion.kt");
        }

        @TestMetadata("Object.kt")
        public void testObject() throws Exception {
            runTest("../idea/tests/testData/highlighter/Object.kt");
        }

        @TestMetadata("PropertiesWithPropertyDeclarations.kt")
        public void testPropertiesWithPropertyDeclarations() throws Exception {
            runTest("../idea/tests/testData/highlighter/PropertiesWithPropertyDeclarations.kt");
        }

        @TestMetadata("RecursiveTypes.kt")
        public void testRecursiveTypes() throws Exception {
            runTest("../idea/tests/testData/highlighter/RecursiveTypes.kt");
        }

        @TestMetadata("SmartCast.kt")
        public void testSmartCast() throws Exception {
            runTest("../idea/tests/testData/highlighter/SmartCast.kt");
        }

        @TestMetadata("Suspend.kt")
        public void testSuspend() throws Exception {
            runTest("../idea/tests/testData/highlighter/Suspend.kt");
        }

        @TestMetadata("SyntheticExtensionProperty.kt")
        public void testSyntheticExtensionProperty() throws Exception {
            runTest("../idea/tests/testData/highlighter/SyntheticExtensionProperty.kt");
        }

        @TestMetadata("Todo.kt")
        public void testTodo() throws Exception {
            runTest("../idea/tests/testData/highlighter/Todo.kt");
        }

        @TestMetadata("TopLevelDestructuring.kt")
        public void testTopLevelDestructuring() throws Exception {
            runTest("../idea/tests/testData/highlighter/TopLevelDestructuring.kt");
        }

        @TestMetadata("TopLevelOpenSuspendFun.kt")
        public void testTopLevelOpenSuspendFun() throws Exception {
            runTest("../idea/tests/testData/highlighter/TopLevelOpenSuspendFun.kt");
        }

        @TestMetadata("TypeAlias.kt")
        public void testTypeAlias() throws Exception {
            runTest("../idea/tests/testData/highlighter/TypeAlias.kt");
        }

        @TestMetadata("TypeAlias2.kt")
        public void testTypeAlias2() throws Exception {
            runTest("../idea/tests/testData/highlighter/TypeAlias2.kt");
        }

        @TestMetadata("TypesAndAnnotations.kt")
        public void testTypesAndAnnotations() throws Exception {
            runTest("../idea/tests/testData/highlighter/TypesAndAnnotations.kt");
        }

        @TestMetadata("Variables.kt")
        public void testVariables() throws Exception {
            runTest("../idea/tests/testData/highlighter/Variables.kt");
        }

        @TestMetadata("VariablesAsFunctions.kt")
        public void testVariablesAsFunctions() throws Exception {
            runTest("../idea/tests/testData/highlighter/VariablesAsFunctions.kt");
        }
    }
}
