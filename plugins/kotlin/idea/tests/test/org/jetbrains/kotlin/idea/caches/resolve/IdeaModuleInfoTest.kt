// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.*
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.artifacts.AdditionalKotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleTestSourceInfo
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.platform
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.addJdk
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.KotlinTestUtils.allowProjectRootAccess
import org.jetbrains.kotlin.test.KotlinTestUtils.disposeVfsRootAccess
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.jarRoot
import org.jetbrains.kotlin.test.util.moduleLibrary
import org.jetbrains.kotlin.test.util.projectLibrary
import org.jetbrains.kotlin.types.typeUtil.closure
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.Assert
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class IdeaModuleInfoTest8 : JavaModuleTestCase() {
    private var vfsDisposable: Ref<Disposable>? = null

    fun testSimpleModuleDependency() {
        val (a, b) = modules()
        b.addDependency(a)

        b.production.assertDependenciesEqual(b.production, a.production)
        UsefulTestCase.assertDoesntContain(a.production.dependencies(), b.production)
    }

    fun testCircularDependency() {
        val (a, b) = modules()

        b.addDependency(a)
        a.addDependency(b)

        a.production.assertDependenciesEqual(a.production, b.production)
        b.production.assertDependenciesEqual(b.production, a.production)
    }

    fun testExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(b)

        a.production.assertDependenciesEqual(a.production)
        b.production.assertDependenciesEqual(b.production, a.production)
        c.production.assertDependenciesEqual(c.production, b.production, a.production)
    }

    fun testRedundantExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        a.production.assertDependenciesEqual(a.production)
        b.production.assertDependenciesEqual(b.production, a.production)
        c.production.assertDependenciesEqual(c.production, a.production, b.production)
    }

    fun testCircularExportedDependency() {
        val (a, b, c) = modules()

        b.addDependency(a, exported = true)
        c.addDependency(b, exported = true)
        a.addDependency(c, exported = true)

        a.production.assertDependenciesEqual(a.production, c.production, b.production)
        b.production.assertDependenciesEqual(b.production, a.production, c.production)
        c.production.assertDependenciesEqual(c.production, b.production, a.production)
    }

    fun testSimpleLibDependency() {
        val a = module("a")
        val lib = projectLibrary()
        a.addDependency(lib)

        a.production.assertDependenciesEqual(a.production, lib.classes)
    }

    fun testCircularExportedDependencyWithLib() {
        val (a, b, c) = modules()

        val lib = projectLibrary()

        a.addDependency(lib)

        b.addDependency(a, exported = true)
        c.addDependency(b, exported = true)
        a.addDependency(c, exported = true)

        b.addDependency(lib)
        c.addDependency(lib)

        a.production.assertDependenciesEqual(a.production, lib.classes, c.production, b.production)
        b.production.assertDependenciesEqual(b.production, a.production, c.production, lib.classes)
        c.production.assertDependenciesEqual(c.production, b.production, a.production, lib.classes)
    }

    fun testSeveralModulesExportLibs() {
        val (a, b, c) = modules()

        val lib1 = projectLibraryWithFakeRoot("lib1")
        val lib2 = projectLibraryWithFakeRoot("lib2")

        a.addDependency(lib1, exported = true)
        b.addDependency(lib2, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        c.production.assertDependenciesEqual(c.production, a.production, lib1.classes, b.production, lib2.classes)
    }

    fun testSeveralModulesExportSameLib() {
        val (a, b, c) = modules()

        val lib = projectLibrary()

        a.addDependency(lib, exported = true)
        b.addDependency(lib, exported = true)
        c.addDependency(a)
        c.addDependency(b)

        c.production.assertDependenciesEqual(c.production, a.production, lib.classes, b.production)
    }

    fun testRuntimeDependency() {
        val (a, b) = modules()

        b.addDependency(a, dependencyScope = DependencyScope.RUNTIME)
        b.addDependency(projectLibrary(), dependencyScope = DependencyScope.RUNTIME)

        b.production.assertDependenciesEqual(b.production)
    }

    fun testProvidedDependency() {
        val (a, b) = modules()
        val lib = projectLibrary()

        b.addDependency(a, dependencyScope = DependencyScope.PROVIDED)
        b.addDependency(lib, dependencyScope = DependencyScope.PROVIDED)

        b.production.assertDependenciesEqual(b.production, a.production, lib.classes)
    }

    fun testSimpleTestDependency() {
        val (a, b) = modules()
        b.addDependency(a, dependencyScope = DependencyScope.TEST)

        a.production.assertDependenciesEqual(a.production)
        a.test.assertDependenciesEqual(a.test, a.production)
        b.production.assertDependenciesEqual(b.production)
        b.test.assertDependenciesEqual(b.test, b.production, a.test, a.production)
    }

    fun testLibTestDependency() {
        val a = module("a")
        val lib = projectLibrary()
        a.addDependency(lib, dependencyScope = DependencyScope.TEST)

        a.production.assertDependenciesEqual(a.production)
        a.test.assertDependenciesEqual(a.test, a.production, lib.classes)
    }

    fun testExportedTestDependency() {
        val (a, b, c) = modules()
        b.addDependency(a, exported = true)
        c.addDependency(b, dependencyScope = DependencyScope.TEST)

        c.production.assertDependenciesEqual(c.production)
        c.test.assertDependenciesEqual(c.test, c.production, b.test, b.production, a.test, a.production)
    }

    fun testDependents() {
        //NOTE: we do not differ between dependency kinds
        val (a, b, c) = modules(name1 = "a", name2 = "b", name3 = "c")
        val (d, e, f) = modules(name1 = "d", name2 = "e", name3 = "f")

        b.addDependency(a, exported = true)

        c.addDependency(a)

        d.addDependency(c, exported = true)

        e.addDependency(b)

        f.addDependency(d)
        f.addDependency(e)


        a.test.assertDependentsEqual(a.test, b.test, c.test, e.test)
        a.production.assertDependentsEqual(a.production, a.test, b.production, b.test, c.production, c.test, e.production, e.test)

        b.test.assertDependentsEqual(b.test, e.test)
        b.production.assertDependentsEqual(b.production, b.test, e.production, e.test)


        c.test.assertDependentsEqual(c.test, d.test, f.test)
        c.production.assertDependentsEqual(c.production, c.test, d.production, d.test, f.production, f.test)

        d.test.assertDependentsEqual(d.test, f.test)
        d.production.assertDependentsEqual(d.production, d.test, f.production, f.test)

        e.test.assertDependentsEqual(e.test, f.test)
        e.production.assertDependentsEqual(e.production, e.test, f.production, f.test)

        f.test.assertDependentsEqual(f.test)
        f.production.assertDependentsEqual(f.production, f.test)
    }

    fun testLibraryDependency1() {
        val lib1 = projectLibraryWithFakeRoot("lib1")
        val lib2 = projectLibraryWithFakeRoot("lib2")

        val module = module("module")
        module.addDependency(lib1)
        module.addDependency(lib2)

        lib1.classes.assertAdditionalLibraryDependencies(lib2.classes)
        lib2.classes.assertAdditionalLibraryDependencies(lib1.classes)
    }

    fun testLibraryDependency2() {
        val lib1 = projectLibraryWithFakeRoot("lib1")
        val lib2 = projectLibraryWithFakeRoot("lib2")
        val lib3 = projectLibraryWithFakeRoot("lib3")

        val (a, b, c) = modules()
        a.addDependency(lib1)
        b.addDependency(lib2)
        c.addDependency(lib3)

        c.addDependency(a)
        c.addDependency(b)

        lib1.classes.assertAdditionalLibraryDependencies()
        lib2.classes.assertAdditionalLibraryDependencies()
        lib3.classes.assertAdditionalLibraryDependencies(lib1.classes, lib2.classes)
    }

    fun testLibraryDependency3() {
        val lib1 = projectLibraryWithFakeRoot("lib1")
        val lib2 = projectLibraryWithFakeRoot("lib2")
        val lib3 = projectLibraryWithFakeRoot("lib3")

        val (a, b) = modules()
        a.addDependency(lib1)
        b.addDependency(lib2)

        a.addDependency(lib3)
        b.addDependency(lib3)

        lib1.classes.assertAdditionalLibraryDependencies(lib3.classes)
        lib2.classes.assertAdditionalLibraryDependencies(lib3.classes)
        lib3.classes.assertAdditionalLibraryDependencies(lib1.classes, lib2.classes)
    }

    fun testRoots() {
        val a = module("a", hasProductionRoot = true, hasTestRoot = false)

        val empty = module("empty", hasProductionRoot = false, hasTestRoot = false)
        a.addDependency(empty)

        val b = module("b", hasProductionRoot = false, hasTestRoot = true)
        b.addDependency(a)

        val c = module("c")
        c.addDependency(b)
        c.addDependency(a)

        assertNotNull(a.productionSourceInfo())
        assertNull(a.testSourceInfo())

        assertNull(empty.productionSourceInfo())
        assertNull(empty.testSourceInfo())

        assertNull(b.productionSourceInfo())
        assertNotNull(b.testSourceInfo())

        b.test.assertDependenciesEqual(b.test, a.production)
        c.test.assertDependenciesEqual(c.test, c.production, b.test, a.production)
        c.production.assertDependenciesEqual(c.production, a.production)
    }

    fun testCommonLibraryDoesNotDependOnPlatform() {
        val stdlibCommon = stdlibCommon()
        val stdlibJvm = stdlibJvm()
        val stdlibJs = stdlibJs()

        val a = module("a")
        a.addDependency(stdlibCommon)
        a.addDependency(stdlibJvm)

        val b = module("b")
        b.addDependency(stdlibCommon)
        b.addDependency(stdlibJs)

        stdlibCommon.classes.assertAdditionalLibraryDependencies()
        stdlibJvm.classes.assertAdditionalLibraryDependencies(stdlibCommon.classes)
        stdlibJs.classes.assertAdditionalLibraryDependencies(stdlibCommon.classes)
    }

    fun testScriptDependenciesForModule() {
        val a = module("a")
        val b = module("b")

        with(createFileInModule(a, "script.kts").moduleInfo) {
            dependencies().contains(a.production)
            dependencies().contains(a.test)
            !dependencies().contains(b.production)
        }
    }

    fun testScriptDependenciesForProject() {
        val a = module("a")

        val script = createFileInProject("script.kts").moduleInfo

        !script.dependencies().contains(a.production)
        !script.dependencies().contains(a.test)

        script.dependencies().firstIsInstance<ScriptDependenciesInfo.ForFile>()
    }

    fun testSdkForScript() {
        // The first known jdk will be used for scripting if there is no jdk in the project
        runWriteAction {
            addJdk(testRootDisposable, IdeaTestUtil::getMockJdk16)
            addJdk(testRootDisposable, IdeaTestUtil::getMockJdk9)

            ProjectRootManager.getInstance(project).projectSdk = null
        }

        val firstSDK = getProjectJdkTableSafe().allJdks.firstOrNull() ?: error("no jdks are present")

        with(createFileInProject("script.kts").moduleInfo) {
            UIUtil.dispatchAllInvocationEvents()
            NonBlockingReadActionImpl.waitForAsyncTaskCompletion()

            val filterIsInstance = dependencies().filterIsInstance<SdkInfo>()
            filterIsInstance.singleOrNull { it.sdk == firstSDK }
                ?: error("Unable to look up ${firstSDK.name} in ${filterIsInstance.map { it.name }} / allJdks: ${getProjectJdkTableSafe().allJdks}")
        }
    }

    fun testSdkForScriptProjectSdk() {
        val mockJdk16 = IdeaTestUtil.getMockJdk16()
        val mockJdk9 = IdeaTestUtil.getMockJdk9()

        runWriteAction {
            addJdk(testRootDisposable) { mockJdk16 }
            addJdk(testRootDisposable) { mockJdk9 }

            ProjectRootManager.getInstance(project).projectSdk = mockJdk9
        }

        with(createFileInProject("script.kts").moduleInfo) {
            dependencies().filterIsInstance<SdkInfo>().single { it.sdk == mockJdk9 }
        }
    }

    fun testSdkForScriptModuleSdk() {
        val mockJdk16 = IdeaTestUtil.getMockJdk16()
        val mockJdk9 = IdeaTestUtil.getMockJdk9()

        val a = module("a")

        runWriteAction {
            addJdk(testRootDisposable) { mockJdk16 }
            addJdk(testRootDisposable) { mockJdk9 }

            ProjectRootManager.getInstance(project).projectSdk = mockJdk16
            with(ModuleRootManager.getInstance(a).modifiableModel) {
                sdk = mockJdk9
                commit()
            }
        }

        with(createFileInModule(a, "script.kts").moduleInfo) {
            dependencies().filterIsInstance<SdkInfo>().first { it.sdk == mockJdk9 }
        }
    }

    fun testTransitiveLibraryDependency() {
        val a = module("a")
        val b = module("b")

        val projectLibrary = projectLibraryWithFakeRoot("transitiveForB")
        a.addDependency(projectLibrary)

        val classRoot = createFileInProject("libraryClass")
        val l1 = moduleLibrary(
            module = a,
            libraryName = "#1",
            classesRoot = classRoot,
        )
        val l2 = moduleLibrary(
            module = b,
            libraryName = "#1",
            classesRoot = classRoot,
        )
        Assert.assertEquals("Library infos for the module libraries with equal roots are not equal", l1.classes, l2.classes)

        a.production.assertDependenciesEqual(a.production, projectLibrary.classes, l1.classes)
        b.production.assertDependenciesEqual(b.production, l2.classes)
        projectLibrary.classes.assertAdditionalLibraryDependencies(l1.classes)

        Assert.assertTrue(
            "Missing transitive dependency on the project library",
            projectLibrary.classes in b.production.dependencies().closure { it.dependencies() }
        )
    }

    private fun createFileInModule(module: Module, fileName: String, inTests: Boolean = false): VirtualFile {
        val fileToCopyIO = createTempFile(fileName, "")

        for (contentEntry in ModuleRootManager.getInstance(module).contentEntries) {
            for (sourceFolder in contentEntry.sourceFolders) {
                if (((!inTests && !sourceFolder.isTestSource) || (inTests && sourceFolder.isTestSource)) && sourceFolder.file != null) {
                    return runWriteAction {
                        getVirtualFile(fileToCopyIO).copy(this, sourceFolder.file!!, fileName)
                    }
                }
            }
        }

        error("Couldn't find source folder in ${module.name}")
    }

    private fun createFileInProject(fileName: String): VirtualFile {
        return runWriteAction {
            getVirtualFile(createTempFile(fileName, "")).copy(this, VfsUtil.findFileByIoFile(File(project.basePath!!), true)!!, fileName)
        }
    }

    private fun Module.addDependency(
        other: Module,
        dependencyScope: DependencyScope = DependencyScope.COMPILE,
        exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported)

    private val VirtualFile.moduleInfo: IdeaModuleInfo
        get() {
            return PsiManager.getInstance(project).findFile(this)!!.getModuleInfo()
        }

    private val Module.production: ModuleProductionSourceInfo
        get() = productionSourceInfo()!!

    private val Module.test: ModuleTestSourceInfo
        get() = testSourceInfo()!!

    private val LibraryEx.classes: LibraryInfo
        get() = object : LibraryInfo(project!!, this) {
            override val platform: TargetPlatform
                get() = kind.platform
        }

    private fun module(name: String, hasProductionRoot: Boolean = true, hasTestRoot: Boolean = true): Module {
        return createModuleFromTestData(createTempDirectory().absolutePath, name, StdModuleTypes.JAVA, false).apply {
            if (hasProductionRoot)
                PsiTestUtil.addSourceContentToRoots(this, dir(), false)
            if (hasTestRoot)
                PsiTestUtil.addSourceContentToRoots(this, dir(), true)
        }
    }

    private fun dir() = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTempDirectory())!!

    private fun modules(name1: String = "a", name2: String = "b", name3: String = "c") = Triple(module(name1), module(name2), module(name3))

    private fun IdeaModuleInfo.assertDependenciesEqual(vararg expected: IdeaModuleInfo) {
        Assert.assertEquals(expected.toList(), this.dependencies())
    }

    private fun LibraryInfo.assertAdditionalLibraryDependencies(vararg expected: IdeaModuleInfo) {
        Assert.assertEquals(this, dependencies().first())
        val dependenciesWithoutSelf = this.dependencies().drop(1)
        UsefulTestCase.assertSameElements(dependenciesWithoutSelf, expected.toList())
    }

    private fun ModuleSourceInfo.assertDependentsEqual(vararg expected: ModuleSourceInfo) {
        UsefulTestCase.assertSameElements(this.getDependentModules(), expected.toList())
    }

    private fun stdlibCommon(): LibraryEx = projectLibrary(
      "kotlin-stdlib-common",
      AdditionalKotlinArtifacts.kotlinStdlibCommon.jarRoot,
      kind = CommonLibraryKind
    )

    private fun stdlibJvm(): LibraryEx = projectLibrary("kotlin-stdlib", KotlinArtifacts.instance.kotlinStdlib.jarRoot)

    private fun stdlibJs(): LibraryEx = projectLibrary(
      "kotlin-stdlib-js",
      KotlinArtifacts.instance.kotlinStdlibJs.jarRoot,
      kind = JSLibraryKind
    )

    private fun projectLibraryWithFakeRoot(name: String): LibraryEx {
        return projectLibrary(name, sourcesRoot = createFileInProject(name))
    }

    override fun setUp() {
        super.setUp()

        vfsDisposable = allowProjectRootAccess(this)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { disposeVfsRootAccess(vfsDisposable) },
            ThrowableRunnable { super.tearDown() },
        )
    }
}
