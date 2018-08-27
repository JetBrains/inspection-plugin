package org.jetbrains.idea.inspections.control

import java.io.Closeable
import java.io.OutputStream
import java.io.PrintStream
import java.security.Permission

class DisableSystemExit : Closeable {

    override fun close() = enableSystemExit()

    private var systemErrorStream: PrintStream? = null
    private var systemOutputStream: PrintStream? = null

    private fun disablePrinting() {
        systemErrorStream = System.err
        systemOutputStream = System.err
        val output = object : OutputStream() {
            override fun write(b: Int) {}
        }
        System.setErr(PrintStream(output))
        System.setOut(PrintStream(output))
    }

    private fun enablePrinting() {
        System.setErr(systemErrorStream)
        System.setErr(systemOutputStream)
    }

    private var previousManager: SecurityManager? = null

    private fun disableSystemExit() {
        previousManager = System.getSecurityManager()
        val securityManager = StopExitSecurityManager()
        System.setSecurityManager(securityManager)
    }

    private fun enableSystemExit() {
        enablePrinting()
        System.setSecurityManager(previousManager)
    }

    private class ExitTrappedException : SecurityException()

    private inner class StopExitSecurityManager : SecurityManager() {
        override fun checkPermission(perm: Permission) {}

        override fun checkExit(status: Int) {
            super.checkExit(status)
            disablePrinting()
            throw ExitTrappedException()
        }
    }

    init {
        disableSystemExit()
    }
}