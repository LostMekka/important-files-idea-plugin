package de.lostmekka.importantfilesideaplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.TableView
import com.intellij.util.messages.Topic
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import javax.swing.ListSelectionModel

object ApplicationSettingsNotifications {
    interface ApplicationSettingsUpdateListener {
        fun onSettingsUpdated()
    }

    val MessageTopic = Topic.create(
        "MySettingsUpdated",
        ApplicationSettingsUpdateListener::class.java
    )
}

class ApplicationSettingsConfigurable : BoundConfigurable("Important Files") {
    private val settings = service<ApplicationSettings>()
    private val updatePublisher = ApplicationManager
        .getApplication()
        .messageBus
        .syncPublisher(ApplicationSettingsNotifications.MessageTopic)

    private val tableModel = ListTableModel(
        arrayOf(
            object : ColumnInfo<String, String>("File") {
                override fun valueOf(path: String) = path
            }
        ),
        settings.filePaths,
    )

    private val table = TableView(tableModel).apply {
        setShowColumns(false)
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        emptyText.text = "file path"
    }

    private val tableContainer = ToolbarDecorator
        .createDecorator(table)
        .setAddAction { addFile() }
        .setEditAction { editFile() }
        .setRemoveAction { removeFile() }
        .setMoveUpAction { move(-1) }
        .setMoveDownAction { move(1) }
        .setEditActionUpdater { true }
        .setRemoveActionUpdater { true }
        .createPanel()

    private fun chooseFile(action: (String) -> Unit) {
        val file = FileChooser.chooseFile(
            FileChooserDescriptor(
                true,
                false,
                false,
                false,
                false,
                false,
            ),
            null,
            null,
        )
        if (file != null) action(file.path)
    }

    private fun withSelectedIndex(action: (Int) -> Unit) {
        val index = table.selectedColumn
        if (index >= 0) action(index)
    }

    private fun changeSettings(mutator: (List<String>) -> List<String>) {
        val oldData = settings.filePaths
        val newData = mutator(oldData)
        if (newData != oldData) {
            settings.filePaths = newData
            tableModel.items = newData
            updatePublisher.onSettingsUpdated()
        }
    }

    private fun addFile() {
        chooseFile { path ->
            changeSettings { it + path }
        }
    }

    private fun removeFile() {
        withSelectedIndex { index ->
            changeSettings {
                it.filterIndexed { i, _ -> i != index }
            }
        }
    }

    private fun editFile() {
        withSelectedIndex { index ->
            chooseFile { path ->
                changeSettings {
                    it.mapIndexed { i, s -> if (i == index) path else s }
                }
            }
        }
    }

    private fun move(amount: Int) {
        withSelectedIndex { sourceIndex ->
            changeSettings {
                val targetIndex = sourceIndex + amount
                if (targetIndex !in it.indices) return@changeSettings it
                val source = it[sourceIndex]
                val target = it[targetIndex]
                it.mapIndexed { i, s ->
                    when (i) {
                        sourceIndex -> target
                        targetIndex -> source
                        else -> s
                    }
                }
            }
        }
    }

    override fun createPanel(): DialogPanel = panel {
        row {
            cell(tableContainer)
                .horizontalAlign(HorizontalAlign.FILL)
                .verticalAlign(VerticalAlign.FILL)
        }
    }
}
