package dev.eastgate.metamaskclone.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the MetaMask Clone tool window.
 *
 * Note: isApplicable() and isDoNotActivateOnStart() are deprecated in IntelliJ 2025.1+.
 * These behaviors are now configured via plugin.xml attributes:
 * - doNotActivateOnStart="true" replaces isDoNotActivateOnStart()
 * - Tool window is applicable to all projects by default (no override needed)
 */
class MetaMaskToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val contentFactory = ContentFactory.getInstance()
        val toolWindowPanel = MetaMaskToolWindow(project)
        val content = contentFactory.createContent(toolWindowPanel.getContent(), "", false)

        // Register disposable to properly clean up coroutine scope
        Disposer.register(content, Disposable { toolWindowPanel.dispose() })

        toolWindow.contentManager.addContent(content)
    }
}
