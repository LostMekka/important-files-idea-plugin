package de.lostmekka.importantfilesideaplugin

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager

class MyTreeStructureProvider(
    private val project: Project,
) : TreeStructureProvider, ApplicationSettingsNotifications.ApplicationSettingsUpdateListener {
    private val log = Logger.getInstance(MyTreeStructureProvider::class.java)
    private val psiManager = PsiManager.getInstance(project)
    private val localFileSystem = LocalFileSystem.getInstance()
    private val appSettings = service<ApplicationSettings>()

    init {
        project.messageBus.connect()
            .subscribe(ApplicationSettingsNotifications.MessageTopic, this)
    }

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        if (parent.value is ProjectEx) {
            val fileChildren = appSettings.filePaths.mapNotNull {
                val virtualFile = localFileSystem.findFileByPath(it)
                if (virtualFile != null) {
                    val psiFile = psiManager.findFile(virtualFile)
                    if (psiFile != null) {
                        // Here we're directly adding PsiFileNode inside the "ImportantFile" node
                        PsiFileNode(project, psiFile, settings)
                    } else null
                } else null
            }
            children.add(ImportantFilesNode(project, fileChildren.toMutableList()))
            log.debug("modified parent ${parent.name} (${parent::class.simpleName} | ${parent.value::class.simpleName})")
        }
        return children
    }

    override fun onSettingsUpdated() {
        ProjectView.getInstance(project).refresh()
    }
}

class ImportantFilesNode(
    project: Project,
    private val children: MutableCollection<PsiFileNode>,
) : AbstractTreeNode<Any>(project, "Important Files") {
    override fun getChildren() = children
    override fun isAlwaysLeaf() = false

    override fun update(presentationData: PresentationData) {
        presentationData.presentableText = "Important Files"
        presentationData.setIcon(AllIcons.Nodes.HomeFolder)
    }
}
