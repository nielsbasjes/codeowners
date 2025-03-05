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
package nl.basjes.maven.enforcer.codeowners.utils;

import lombok.extern.slf4j.Slf4j;
import nl.basjes.maven.enforcer.codeowners.EnforcerTestLogger;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestProblemTable {

    @Test
    void testProblemTable() {
        ProblemTable table = new ProblemTable();
        table.addProblem(new Problem.Info    ("Section Info",    "Expression Info",    "Approver Info",    "Problem Info"));
        table.addProblem(new Problem.Warning ("Section Warning", "Expression Warning", "Approver Warning", "Problem Warning"));
        table.addProblem(new Problem.Error   ("Section Error",   "Expression Error",   "Approver Error",   "Problem Error"));
        table.addProblem(new Problem.Fatal   ("Section Fatal",   "Expression Fatal",   "Approver Fatal",   "Problem Fatal"));

        table.toLog(new EnforcerTestLogger());
    }

}
