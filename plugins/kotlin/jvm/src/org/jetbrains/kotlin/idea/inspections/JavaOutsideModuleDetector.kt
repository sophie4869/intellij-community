// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.inspections

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinJvmBundle
import org.jetbrains.kotlin.idea.configuration.isGradleModule
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.findModule
import org.jetbrains.kotlin.idea.util.sourceRoots

class JavaOutsideModuleDetector(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!FileTypeRegistry.getInstance().isFileOfType(file, JavaFileType.INSTANCE)) return null
        val module = file.findModule(project) ?: return null
        if (!module.isGradleModule()) return null
        val facetSettings = KotlinFacet.get(module)?.configuration?.settings ?: return null

        val filePath = file.path
        val nonKotlinPath = module.sourceRoots.map { it.path } - facetSettings.pureKotlinSourceFolders
        if (nonKotlinPath.any { filePath.startsWith(it) }) return null
        return EditorNotificationPanel().apply {
            text(KotlinJvmBundle.message("this.java.file.is.outside.of.java.source.roots.and.won.t.be.added.to.the.classpath"))
            icon(AllIcons.General.Warning)
        }
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("JavaOutsideModuleDetector")
    }
}