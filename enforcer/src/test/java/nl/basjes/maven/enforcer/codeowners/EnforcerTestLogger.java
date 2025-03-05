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

import lombok.AllArgsConstructor;
import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class EnforcerTestLogger implements EnforcerLogger {

    public EnforcerTestLogger(String className) {
        logger = LoggerFactory.getLogger(className);
    }

    final Logger logger;

    enum LineType {
        WARN_OR_ERROR,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    @AllArgsConstructor
    static class Line {
        LineType type;
        String message;

        @Override
        public String toString() {
            return "Line {" +
                "type=" + type +
                ", message='" + message + '\'' +
                '}';
        }
    }

    final List<Line> loggedLines = new ArrayList<>();

    private void assertContainsMsg(LineType type, String expectedSubstring) {
        for (Line loggedLine : loggedLines) {
            if (loggedLine.type == type) {
                if (loggedLine.message.contains(expectedSubstring)) {
                    return;
                }
            }
        }
        fail("A message of type "+type+" containing '" + expectedSubstring + "' was not found.\n"+ "Known lines are: \n" + loggedLines.stream().map(line -> "---> " + line.type + " | " + line.message + "\n").collect(Collectors.joining()));
    }

    public void assertContainsWarnOrError(String message) {
        assertContainsMsg(LineType.WARN_OR_ERROR, message);
    }
    public void assertContainsDebug(String message) {
        assertContainsMsg(LineType.DEBUG, message);
    }
    public void assertContainsInfo(String message) {
        assertContainsMsg(LineType.INFO ,message);
    }
    public void assertContainsWarn(String message) {
        assertContainsMsg(LineType.WARN, message);
    }
    public void assertContainsError(String message) {
        assertContainsMsg(LineType.ERROR, message);
    }

    public long countWarnOrError() { return loggedLines.stream().filter(l -> l.type == LineType.WARN_OR_ERROR) .count(); }
    public long countDebug()       { return loggedLines.stream().filter(l -> l.type == LineType.DEBUG)         .count(); }
    public long countInfo()        { return loggedLines.stream().filter(l -> l.type == LineType.INFO)          .count(); }
    public long countWarn()        { return loggedLines.stream().filter(l -> l.type == LineType.WARN)          .count(); }
    public long countError()       { return loggedLines.stream().filter(l -> l.type == LineType.ERROR)         .count(); }

    @Override
    public void warnOrError(CharSequence charSequence) {
        loggedLines.add(new Line(LineType.WARN_OR_ERROR, charSequence.toString()));
        logger.warn(charSequence.toString());
    }

    @Override
    public void warnOrError(Supplier<CharSequence> supplier) {
        loggedLines.add(new Line(LineType.WARN_OR_ERROR, supplier.get().toString()));
        logger.warn(supplier.get().toString());
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(CharSequence charSequence) {
        loggedLines.add(new Line(LineType.DEBUG, charSequence.toString()));
        logger.debug(charSequence.toString());
    }

    @Override
    public void debug(Supplier<CharSequence> supplier) {
        loggedLines.add(new Line(LineType.DEBUG, supplier.get().toString()));
        logger.debug(supplier.get().toString());
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(CharSequence charSequence) {
        loggedLines.add(new Line(LineType.INFO, charSequence.toString()));
        logger.info(charSequence.toString());
    }

    @Override
    public void info(Supplier<CharSequence> supplier) {
        loggedLines.add(new Line(LineType.INFO, supplier.get().toString()));
        logger.info(supplier.get().toString());
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(CharSequence charSequence) {
        loggedLines.add(new Line(LineType.WARN, charSequence.toString()));
        logger.warn(charSequence.toString());
    }

    @Override
    public void warn(Supplier<CharSequence> supplier) {
        loggedLines.add(new Line(LineType.WARN, supplier.get().toString()));
        logger.warn(supplier.get().toString());
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(CharSequence charSequence) {
        loggedLines.add(new Line(LineType.ERROR, charSequence.toString()));
        logger.error(charSequence.toString());
    }

    @Override
    public void error(Supplier<CharSequence> supplier) {
        loggedLines.add(new Line(LineType.ERROR, supplier.get().toString()));
        logger.error(supplier.get().toString());
    }
}
