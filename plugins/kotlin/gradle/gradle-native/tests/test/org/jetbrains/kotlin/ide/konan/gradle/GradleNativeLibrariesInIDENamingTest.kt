// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.ide.konan.gradle

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtilCore.urlToPath
import org.jetbrains.kotlin.ide.konan.NativeLibraryKind
import org.jetbrains.kotlin.idea.configuration.externalCompilerVersion
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.konan.library.konanCommonLibraryPath
import org.jetbrains.kotlin.konan.library.konanPlatformLibraryPath
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Assert.*
import org.junit.Test
import org.junit.runners.Parameterized
import java.io.File


class GradleNativeLibrariesInIDENamingTest15 : TestCaseWithFakeKotlinNative() {
    // Test naming of Kotlin/Native libraries
    @Test
    fun testLibrariesNaming() {
        configureProject()
        importProject()

        myProject.allModules().forEach { assertValidModule(it, projectPath) }
    }

    override fun getExternalSystemConfigFileName() = GradleConstants.KOTLIN_DSL_SCRIPT_NAME

    override fun testDataDirName() = "nativeLibraries"

    companion object {
        @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
        @Throws(Throwable::class)
        @JvmStatic
        fun data() = listOf(arrayOf("4.10.2"))
    }
}

private val NATIVE_LIBRARY_NAME_REGEX = Regex("^Kotlin/Native ([\\d\\w.-]+) - ([\\w\\d]+)( \\| \\[?([\\w\\d_]+)]?)?$")

private val NATIVE_LINUX_LIBRARIES = listOf(
    "Kotlin/Native {version} - stdlib",
    "Kotlin/Native {version} - linux | linux_x64",
    "Kotlin/Native {version} - posix | linux_x64",
    "Kotlin/Native {version} - zlib | linux_x64"
)

private val NATIVE_MACOS_LIBRARIES = listOf(
    "Kotlin/Native {version} - stdlib",
    "Kotlin/Native {version} - Foundation | macos_x64",
    "Kotlin/Native {version} - UIKit | macos_x64",
    "Kotlin/Native {version} - objc | macos_x64"
)

private val NATIVE_MINGW_LIBRARIES = listOf(
    "Kotlin/Native {version} - stdlib",
    "Kotlin/Native {version} - iconv | mingw_x64",
    "Kotlin/Native {version} - opengl32 | mingw_x64",
    "Kotlin/Native {version} - windows | mingw_x64"
)

private val Module.libraries
    get() = ModuleRootManager.getInstance(this).orderEntries
        .asSequence()
        .filterIsInstance<LibraryOrderEntry>()
        .mapNotNull { it.library }

private fun assertValidModule(module: Module, projectRoot: String) {
    val (nativeLibraries, otherLibraries) = module.libraries.partition { library ->
        detectLibraryKind(library.getFiles(OrderRootType.CLASSES)) == NativeLibraryKind
    }

    if (module.platform.isNative()) {
        assertFalse("No Kotlin/Native libraries in $module", nativeLibraries.isEmpty())
        nativeLibraries.forEach { assertValidNativeLibrary(it, projectRoot) }

        val kotlinVersion = requireNotNull(module.externalCompilerVersion) {
            "External compiler version should not be null"
        }

        val expectedNativeLibraryNames = when {
            module.name.contains("linux", ignoreCase = true) -> NATIVE_LINUX_LIBRARIES
            module.name.contains("macos", ignoreCase = true) -> NATIVE_MACOS_LIBRARIES
            module.name.contains("mingw", ignoreCase = true) -> NATIVE_MINGW_LIBRARIES
            else -> emptyList()
        }.map { it.replace("{version}", kotlinVersion) }.sorted()

        val actualNativeLibraryNames = nativeLibraries.map { it.name.orEmpty() }.sorted()

        assertEquals("Different set of Kotlin/Native libraries in $module", expectedNativeLibraryNames, actualNativeLibraryNames)

    } else {
        assertTrue("Unexpected Kotlin/Native libraries in $module: $nativeLibraries", nativeLibraries.isEmpty())
    }

    otherLibraries.forEach(::assertValidNonNativeLibrary)
}

private fun assertValidNativeLibrary(library: Library, projectRoot: String) {
    val fullName = library.name.orEmpty()

    val result = NATIVE_LIBRARY_NAME_REGEX.matchEntire(fullName)
    assertTrue("Invalid Kotlin/Native library name: $fullName", result?.groups?.size == 5)

    val (_, name, _, platform) = result!!.destructured

    val libraryUrls = library.getUrls(OrderRootType.CLASSES).toList()
    assertTrue("Kotlin/Native library $fullName has multiple roots (${libraryUrls.size}): $libraryUrls", libraryUrls.size == 1)

    val actualShortPath = File(urlToPath(libraryUrls.single()))
        .relativeTo(File(projectRoot).resolve(DOUBLE_DOT_PATH).normalize())
        .relativeTo(FAKE_KOTLIN_NATIVE_HOME_RELATIVE_PATH)

    val expectedDirectoryName = if (name == "stdlib") name else "org.jetbrains.kotlin.native.$name"
    val expectedShortPath = if (platform.isEmpty()) {
        konanCommonLibraryPath(expectedDirectoryName)
    } else {
        konanPlatformLibraryPath(expectedDirectoryName, platform)
    }
    assertEquals("The short path of $fullName does not match its location", expectedShortPath, actualShortPath)
}

private fun assertValidNonNativeLibrary(library: Library) {
    val name = library.name.orEmpty()
    assertFalse("Invalid non-native library name: $name", name.contains("Kotlin/Native"))
}
