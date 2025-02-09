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

import java.util.ArrayList;
import java.util.List;

@Getter
public class Problem {
    private final List<String> fields = new ArrayList<>();

    public Problem(String section, String expression, String approver, String description) {
        fields.add(section);
        fields.add(expression);
        fields.add(approver);
        fields.add(description);
    }

    public static class Info extends Problem {
        public Info(String section, String expression, String approver, String description) {
            super(section, expression, approver, description);
        }
    }

    public static class Warning extends Problem {
        public Warning(String section, String expression, String approver, String description) {
            super(section, expression, approver, description);
        }
    }

    public static class Error extends Problem {
        public Error(String section, String expression, String approver, String description) {
            super(section, expression, approver, description);
        }
    }
}
