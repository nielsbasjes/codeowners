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

package nl.basjes.codeowners.validator.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class TestLogger extends AbstractLogger {

    public TestLogger(String className) {
        logger = LoggerFactory.getLogger(className);
    }

    final Logger logger;

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }


    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        FormattingTuple tuple = MessageFormatter.format(messagePattern, arguments, throwable);
        loggedLines.add(new nl.basjes.codeowners.validator.utils.Line(level, tuple.getMessage()));

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


    final List<Line> loggedLines = new ArrayList<>();

    private void assertContainsMsg(Level level, String expectedSubstring) {
        for (Line loggedLine : loggedLines) {
            if (loggedLine.level == level) {
                if (loggedLine.message.contains(expectedSubstring)) {
                    return;
                }
            }
        }
        fail("A message of level "+level+" containing '" + expectedSubstring + "' was not found.\n"+ "Known lines are: \n" + loggedLines.stream().map(line -> "---> " + line.level + " | " + line.message + "\n").collect(Collectors.joining()));
    }

    public void assertContainsDebug(String message) {
        assertContainsMsg(Level.DEBUG, message);
    }
    public void assertContainsInfo(String message) {
        assertContainsMsg(Level.INFO ,message);
    }
    public void assertContainsWarn(String message) {
        assertContainsMsg(Level.WARN, message);
    }
    public void assertContainsError(String message) {
        assertContainsMsg(Level.ERROR, message);
    }

    public long countDebug()       { return loggedLines.stream().filter(l -> l.level == Level.DEBUG)         .count(); }
    public long countInfo()        { return loggedLines.stream().filter(l -> l.level == Level.INFO)          .count(); }
    public long countWarn()        { return loggedLines.stream().filter(l -> l.level == Level.WARN)          .count(); }
    public long countError()       { return loggedLines.stream().filter(l -> l.level == Level.ERROR)         .count(); }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

}
