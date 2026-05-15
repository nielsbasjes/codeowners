/*
 * CodeOwners Tools
 * Copyright (C) 2023-2025 Niels Basjes
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
package nl.basjes.maven.enforcer.codeowners

import org.apache.maven.enforcer.rule.api.EnforcerLogger
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.MessageFormatter

class EnforcerLoggerSlf4j(private val logger: EnforcerLogger) : Logger {
    private fun log(level: Level, messagePattern: String?, vararg arguments: Any?) {
        val tuple = MessageFormatter.format(messagePattern, arguments)
        when (level) {
            Level.ERROR -> logger.error(tuple.message)
            Level.WARN -> logger.warn(tuple.message)
            Level.INFO -> logger.info(tuple.message)
            Level.DEBUG, Level.TRACE -> logger.debug(tuple.message)
        }
    }

    override fun getName(): String {
        return "EnforcerLoggerSlf4j"
    }

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun trace(msg: String?) {
        log(Level.TRACE, msg)
    }

    override fun trace(format: String?, arg: Any?) {
        log(Level.TRACE, format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        log(Level.TRACE, format, arg1, arg2)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        log(Level.TRACE, format, *arguments)
    }

    override fun trace(msg: String?, t: Throwable?) {
        log(Level.TRACE, msg)
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return false
    }

    override fun trace(marker: Marker?, msg: String?) {
        log(Level.TRACE, msg)
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        log(Level.TRACE, format, arg)
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        log(Level.TRACE, format, arg1, arg2)
    }

    override fun trace(marker: Marker?, format: String?, vararg arguments: Any?) {
        log(Level.TRACE, format, *arguments)
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        log(Level.TRACE, msg)
    }

    override fun isDebugEnabled(): Boolean {
        return logger.isDebugEnabled
    }

    override fun debug(msg: String?) {
        log(Level.DEBUG, msg)
    }

    override fun debug(format: String?, arg: Any?) {
        log(Level.DEBUG, format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        log(Level.DEBUG, format, arg1, arg2)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        log(Level.DEBUG, format, *arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        log(Level.DEBUG, msg)
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return logger.isDebugEnabled
    }

    override fun debug(marker: Marker?, msg: String?) {
        log(Level.DEBUG, msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        log(Level.DEBUG, format, arg)
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        log(Level.DEBUG, format, arg1, arg2)
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        log(Level.DEBUG, format, *arguments)
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        log(Level.DEBUG, msg)
    }

    override fun isInfoEnabled(): Boolean {
        return logger.isInfoEnabled()
    }

    override fun info(msg: String?) {
        log(Level.INFO, msg)
    }

    override fun info(format: String?, arg: Any?) {
        log(Level.INFO, format, arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        log(Level.INFO, format, arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        log(Level.INFO, format, *arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        log(Level.INFO, msg)
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return logger.isInfoEnabled()
    }

    override fun info(marker: Marker?, msg: String?) {
        log(Level.INFO, msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        log(Level.INFO, format, arg)
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        log(Level.INFO, format, arg1, arg2)
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        log(Level.INFO, format, *arguments)
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        log(Level.INFO, msg)
    }

    override fun isWarnEnabled(): Boolean {
        return logger.isWarnEnabled()
    }

    override fun warn(msg: String?) {
        log(Level.WARN, msg)
    }

    override fun warn(format: String?, arg: Any?) {
        log(Level.WARN, format, arg)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        log(Level.WARN, format, *arguments)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        log(Level.WARN, format, arg1, arg2)
    }

    override fun warn(msg: String?, t: Throwable?) {
        log(Level.WARN, msg)
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return logger.isWarnEnabled()
    }

    override fun warn(marker: Marker?, msg: String?) {
        log(Level.WARN, msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        log(Level.WARN, format, arg)
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        log(Level.WARN, format, arg1, arg2)
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        log(Level.WARN, format, *arguments)
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        log(Level.WARN, msg)
    }

    override fun isErrorEnabled(): Boolean {
        return logger.isErrorEnabled()
    }

    override fun error(msg: String?) {
        log(Level.ERROR, msg)
    }

    override fun error(format: String?, arg: Any?) {
        log(Level.ERROR, format, arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        log(Level.ERROR, format, arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        log(Level.ERROR, format, *arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        log(Level.ERROR, msg)
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return logger.isErrorEnabled()
    }

    override fun error(marker: Marker?, msg: String?) {
        log(Level.ERROR, msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        log(Level.ERROR, format, arg)
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        log(Level.ERROR, format, arg1, arg2)
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        log(Level.ERROR, format, *arguments)
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        log(Level.ERROR, msg)
    }
}
