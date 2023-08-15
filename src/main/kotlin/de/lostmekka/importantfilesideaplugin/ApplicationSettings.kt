package de.lostmekka.importantfilesideaplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "de.lostmekka.important-files-idea-plugin.Settings",
    storages = [Storage("ImportantFiles.xml")],
)
class ApplicationSettings : PersistentStateComponent<ApplicationSettings?> {
    private var listeners: List<ChangeListener> = listOf()
    var filePaths: List<String> = listOf()
        set(value) {
            field = value
            notifyListeners()
        }

    override fun getState() = this

    override fun loadState(state: ApplicationSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun registerListener(listener: ChangeListener) {
        listeners = listeners.plus(listener)
    }

    private fun notifyListeners() {
        listeners.forEach(ChangeListener::onChange)
    }

    interface ChangeListener {
        fun onChange()
    }
}
