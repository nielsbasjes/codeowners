/*
 * CodeOwners Tools
 * Copyright (C) 2023-2026 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.codeowners.validator.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter
import kotlin.test.fail

class TestLogger(className: String?) : AbstractLogger() {
    val logger: Logger = LoggerFactory.getLogger(className)

    override fun getFullyQualifiedCallerName(): String? {
        return null
    }

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?
    ) {
        val tuple = MessageFormatter.format(messagePattern, arguments, throwable)
        loggedLines.add(Line(level, tuple.message))

        when (level) {
            Level.ERROR -> logger.error(tuple.message)
            Level.WARN -> logger.warn(tuple.message)
            Level.INFO -> logger.info(tuple.message)
            Level.DEBUG, Level.TRACE -> logger.debug(tuple.message)
        }
    }

    val loggedLines: MutableList<Line> = mutableListOf()

    private fun assertContainsMsg(level: Level?, expectedSubstring: String) {
        for (loggedLine in loggedLines) {
            if (loggedLine.level == level) {
                if (loggedLine.message.contains(expectedSubstring)) {
                    return
                }
            }
        }
        fail(
            "A message of level $level containing '$expectedSubstring' was not found.\n" +
            "Known lines are: \n" +
            loggedLines.map { "---> ${it.level} | ${it.message}\n" }.toList()
        )
    }

    fun assertContainsDebug(message: String) {
        assertContainsMsg(Level.DEBUG, message)
    }

    fun assertContainsInfo(message: String) {
        assertContainsMsg(Level.INFO, message)
    }

    fun assertContainsWarn(message: String) {
        assertContainsMsg(Level.WARN, message)
    }

    fun assertContainsError(message: String) {
        assertContainsMsg(Level.ERROR, message)
    }

    fun countDebug(): Int {
        return loggedLines.count { it.level == Level.DEBUG }
    }

    fun countInfo(): Int {
        return loggedLines.count { it.level == Level.INFO }
    }

    fun countWarn(): Int {
        return loggedLines.count { it.level == Level.WARN }
    }

    fun countError(): Int {
        return loggedLines.count { it.level == Level.ERROR }
    }

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return false
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return false
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return false
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return false
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return false
    }
}
