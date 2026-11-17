package com.threedreport.app.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinuxColorSchemeTest {
    @Test
    fun theReadAnswerOfThePortalSaysDarkOnlyForOne() {
        assertEquals(true, LinuxColorScheme.parseColorScheme("(<uint32 1>,)\n"))
        assertEquals(false, LinuxColorScheme.parseColorScheme("(<uint32 0>,)\n"))
        assertEquals(false, LinuxColorScheme.parseColorScheme("(<uint32 2>,)\n"))
    }

    @Test
    fun theChangeSignalIsReadToo() {
        val signal = "/org/freedesktop/portal/desktop: org.freedesktop.portal.Settings.SettingChanged " +
            "('org.freedesktop.appearance', 'color-scheme', <uint32 1>)"
        assertEquals(true, LinuxColorScheme.parseColorScheme(signal))
    }

    @Test
    fun anAnswerWithoutNumberIsUnknown() {
        assertNull(LinuxColorScheme.parseColorScheme("Error: GDBus.Error:org.freedesktop.DBus.Error.ServiceUnknown"))
    }
}
