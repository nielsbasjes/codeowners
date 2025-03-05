/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.maven.enforcer.codeowners.utils;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Problem {
    private final String section;
    private final String expression;
    private final String approver;
    private final String description;
    private final String marker;

    public Problem(String section, String expression, String approver, String description, String marker) {
        this.section = section;
        this.expression = expression;
        this.approver = approver;
        this.description = description;
        this.marker = marker;
    }

    public Problem(String section, String expression, String approver, String description) {
        this(section, expression, approver, description, "\uD83E\uDD26");
    }

    public static class Info extends Problem {
        public Info(String section, String expression, String approver, String description) {
            super(section, expression, approver, description, "");
        }
    }

    public static class Warning extends Problem {
        public Warning(String section, String expression, String approver, String description) {
            super(section, expression, approver, description, "⚠️");
        }
    }

    public static class Error extends Problem {
        public Error(String section, String expression, String approver, String description) {
            super(section, expression, approver, description, "❌");
        }
    }

    public static class Fatal extends Problem {
        public Fatal(String section, String expression, String approver, String description) {
            super(section, expression, approver, description, "⛔\uFE0F");
        }
    }

    @Override
    public String toString() {
        return "Problem "+this.getClass().getSimpleName() +
            "(Section='" + section + '\'' +
            " Expression='" + expression + '\'' +
            " Approver='" + approver + '\'' +
            " Description='" + description + '\'' +
            ')';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Problem problem = (Problem) o;
        return
            Objects.equals(section, problem.section) &&
            Objects.equals(expression, problem.expression) &&
            Objects.equals(approver, problem.approver) &&
            Objects.equals(description, problem.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(section, expression, approver, description);
    }
}
