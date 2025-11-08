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

package nl.basjes.maven.enforcer.codeowners;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class EnforcerLoggerSlf4j implements Logger {
    private final EnforcerLogger logger;

    public EnforcerLoggerSlf4j(EnforcerLogger logger) {
        this.logger = logger;
    }

    private void log(Level level, String messagePattern, Object... arguments) {
        FormattingTuple tuple = MessageFormatter.format(messagePattern, arguments);
        switch (level) {
            case ERROR:
                logger.error(tuple.getMessage());
                break;
            case WARN:
                logger.warn(tuple.getMessage());
                break;
            case INFO:
                logger.info(tuple.getMessage());
                break;
            case DEBUG:
            case TRACE:
                logger.debug(tuple.getMessage());
                break;
        }
    }

    @Override
    public String getName() {
        return "EnforcerLoggerSlf4j";
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String msg) {
        log(Level.TRACE, msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log(Level.TRACE, format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(Level.TRACE, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(Level.TRACE, msg);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(Level.TRACE, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log(Level.TRACE, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.TRACE, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        log(Level.TRACE, format, arguments);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log(Level.TRACE, msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        log(Level.DEBUG, msg);

    }

    @Override
    public void debug(String format, Object arg) {
        log(Level.DEBUG, format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Level.DEBUG, msg);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String msg) {
        log(Level.DEBUG, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log(Level.DEBUG, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.DEBUG, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        log(Level.DEBUG, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log(Level.DEBUG, msg);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(String format, Object arg) {
        log(Level.INFO, format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(Level.INFO, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log(Level.INFO, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.INFO, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        log(Level.INFO, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log(Level.INFO, msg);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        log(Level.WARN, msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log(Level.WARN, format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(Level.WARN, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARN, msg);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(Level.WARN, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log(Level.WARN, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.WARN, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        log(Level.WARN, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log(Level.WARN, msg);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        log(Level.ERROR, msg);
    }

    @Override
    public void error(String format, Object arg) {
        log(Level.ERROR, format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(Level.ERROR, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.ERROR, msg);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String msg) {
        log(Level.ERROR, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log(Level.ERROR, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log(Level.ERROR, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        log(Level.ERROR, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log(Level.ERROR, msg);
    }

}
