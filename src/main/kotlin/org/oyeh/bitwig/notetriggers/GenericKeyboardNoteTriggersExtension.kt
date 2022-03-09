package org.oyeh.bitwig.notetriggers

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.callback.DoubleValueChangedCallback
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.ControllerHost
import com.bitwig.extension.controller.api.HardwareActionBindable
import com.bitwig.extension.controller.api.HardwareButton
import org.oyeh.bitwig.*

class GenericKeyboardNoteTriggersExtension(private val log: Logger, host: ControllerHost) {
    companion object {

        /**
         * Construct extension facade
         */
        @JvmStatic
        fun init(
            definition: GenericKeyboardNoteTriggersExtensionDefinition,
            host: ControllerHost
        ): ControllerExtension {
            return object : ControllerExtension(definition, host) {
                val log = Logger(host, LogSeverity.TRACE)
                var db: GenericKeyboardNoteTriggersExtension? = null

                override fun init() {
                    if (db != null) {
                        log.important("Init: Extension already initialized!")
                        return
                    }
                    db = GenericKeyboardNoteTriggersExtension(log, host)
                }

                override fun flush() {
                    db ?: log.important("Flush: Extension inactive. Nothing to do.")
                }

                override fun exit() {
                    db
                        ?.dispose()
                        ?: log.important("Exit: Extension inactive. Nothing to do.")
                    db = null
                }
            }
        }
    }

    private val settings = host.preferences
    private val isDebug =
        false
//        settings.getBooleanSetting("Debug Mode", "General", true).get()

    private val keys = (0..127).toList().toTypedArray()
    private val midiInput = host.getMidiInPort(0)
    private val noteInput = midiInput
        .createNoteInput("Port 1")
        .also {
            if (isDebug)
                it.setShouldConsumeEvents(false)

            it.includeInAllInputs().set(true)
            it.setKeyTranslationTable(keys)
        }
    private val transport = host.createTransport()
    private val cursorTrack = host.createCursorTrack(
        "gknt-track", "Current Track", 0, 0, true
    )
    private data class Trigger(
        val label: String,
        val category: String,
        val id: String,
        val action: HardwareActionBindable
    )

    private val hardware = host.createHardwareSurface()
    private val triggers = listOf(
        Trigger("Play", "Transport", "gknt-play", transport.playAction()),
        Trigger("Stop", "Transport", "gknt-stop", transport.stopAction()),
        Trigger("Record", "Transport", "gknt-record", transport.recordAction()),
        Trigger("Tap tempo", "Transport", "gknt-tap-tempo", transport.tapTempoAction()),
        Trigger("Record arm", "Cursor track", "gknt-track-arm", cursorTrack.arm().toggleAction()),
        Trigger("Solo", "Cursor track", "gknt-track-solo", cursorTrack.solo().toggleAction()),
        Trigger("Mute", "Cursor track", "gknt-track-mute", cursorTrack.mute().toggleAction()),
        Trigger("Previous track", "Cursor track", "gknt-track-prev", cursorTrack.selectPreviousAction()),
        Trigger("Next track", "Cursor track", "gknt-track-next", cursorTrack.selectNextAction())
    )
        .map {
            val trigger = hardware.createHardwareButton(it.id).apply {
                name = it.label
                pressedAction().setBinding(it.action)
            }
            val setting = settings.getNumberSetting(
                it.label, it.category, -1.0, 127.0, 1.0, "", -1.0
            )
            val initialValue = updateActionNote(trigger, -1, setting.raw)
            setting.addRawValueObserver(object : DoubleValueChangedCallback {
                private var previousNote = initialValue
                override fun valueChanged(newValue: Double) {
                    previousNote = updateActionNote(trigger, previousNote, newValue)
                    updateKeyFilter()
                }
            })
            trigger
        }

    private fun updateActionNote(action: HardwareButton, previousNote: Int, note: Double): Int {
        val noteNumber = note.toInt()
        if (previousNote == noteNumber)
            return previousNote

        if (previousNote >= 0)
            keys[previousNote] = previousNote

        val noteMatcher = when {
            (noteNumber >= 0) -> midiInput.createActionMatcher(
                "((status & 240) == 144 && data1 == ${noteNumber})"
            )
            else -> null
        }
        action.pressedAction().setActionMatcher(noteMatcher)

        if (noteNumber >= 0)
            keys[noteNumber] = -1

        return noteNumber
    }

    private fun updateKeyFilter() {
        noteInput.setKeyTranslationTable(keys.clone())
    }

    init {
//        if (isDebug) {
//            host.getMidiInPort(0).setMidiCallback { statusByte, data1, data2 ->
//                val command = statusByte and 0xf0
//                val channel = statusByte and 0xf
//                val tag = "MIDI IN"
//                val message = when (command) {
//                    ShortMidiMessage.CONTROL_CHANGE ->
//                        "ControlChange(channel: ${channel}, cc: ${data1}, value: ${data2}"
//                    ShortMidiMessage.NOTE_ON ->
//                        "NoteOn(channel: ${channel}, note: ${data1}, velocity: ${data2}"
//                    ShortMidiMessage.NOTE_OFF ->
//                        "NoteOff(channel: ${channel}, note: ${data1}, release velocity: ${data2}"
//                    else ->
//                        "${command}(channel: ${channel}, data1: ${data1}, data2: ${data2}"
//                }
//                log.trace(message, tag)
//            }
//        }
        updateKeyFilter()
    }

    fun dispose() {
        triggers.forEach {
            it.pressedAction().apply {
                clearBindings()
                setActionMatcher(null)
            }
        }
        log.info("${GenericKeyboardNoteTriggersExtensionDefinition.NAME} extension disposed.")
    }
}
