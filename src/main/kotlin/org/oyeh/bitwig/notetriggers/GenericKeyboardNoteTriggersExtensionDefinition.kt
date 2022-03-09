package org.oyeh.bitwig.notetriggers

import com.bitwig.extension.api.PlatformType
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.ControllerExtensionDefinition
import com.bitwig.extension.controller.api.ControllerHost
import java.util.*

class GenericKeyboardNoteTriggersExtensionDefinition : ControllerExtensionDefinition() {

    override fun getId(): UUID = DRIVER_ID
    override fun getRequiredAPIVersion(): Int = 15
    override fun getHardwareVendor(): String = "Generic"
    override fun getHardwareModel(): String = "Generic Keyboard With Note Triggers"
    override fun getName(): String = NAME
    override fun getAuthor(): String = "Hrvoje Velic"
    override fun getVersion(): String = "0.1"

    override fun getNumMidiInPorts(): Int = 1
    override fun getNumMidiOutPorts(): Int = 0

    override fun listAutoDetectionMidiPortNames(
        list: AutoDetectionMidiPortNamesList, platformType: PlatformType
    ) {}

    override fun createInstance(host: ControllerHost): ControllerExtension =
        GenericKeyboardNoteTriggersExtension.init(this, host)

    companion object {
        @JvmField val NAME = "Generic Keyboard With Note Triggers (Oyeh)"
        @JvmField val DRIVER_ID = UUID.fromString("de0ef3ac-799a-4ef5-9b19-91ed5c4a4e24")
    }
}
