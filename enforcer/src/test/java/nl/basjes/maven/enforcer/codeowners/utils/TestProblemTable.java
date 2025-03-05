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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TestProblemTable {

    @Test
    void testProblemTable() {
        ProblemTable table = new ProblemTable();
        table.addProblem(new Problem         ("Section Unknown",   "Expression Unknown",   "Approver Unknown",   "Problem Unknown"));
        table.addProblem(new Problem.Info    ("Section Info",    "Expression Info",    "Approver Info",    "Problem Info"));
        table.addProblem(new Problem.Warning ("Section Warning", "Expression Warning", "Approver Warning", "Problem Warning"));
        table.addProblem(new Problem.Error   ("Section Error",   "Expression Error",   "Approver Error",   "Problem Error"));
        table.addProblem(new Problem.Fatal   ("Section Fatal",   "Expression Fatal",   "Approver Fatal",   "Problem Fatal"));

        assertTrue(table.contains(new Problem          ("Section Unknown",   "Expression Unknown",   "Approver Unknown",   "Problem Unknown")));
        assertTrue(table.contains(new Problem.Info     ("Section Info",      "Expression Info",      "Approver Info",      "Problem Info")));
        assertTrue(table.contains(new Problem.Warning  ("Section Warning",   "Expression Warning",   "Approver Warning",   "Problem Warning")));
        assertTrue(table.contains(new Problem.Error    ("Section Error",     "Expression Error",     "Approver Error",     "Problem Error")));
        assertTrue(table.contains(new Problem.Fatal    ("Section Fatal",     "Expression Fatal",     "Approver Fatal",     "Problem Fatal")));

        assertFalse(table.contains(new Problem         ("x Section Unknown", "x Expression Unknown", "x Approver Unknown", "x Problem Unknown")));
        assertFalse(table.contains(new Problem.Info    ("x Section Info",    "x Expression Info",    "x Approver Info",    "x Problem Info")));
        assertFalse(table.contains(new Problem.Warning ("x Section Warning", "x Expression Warning", "x Approver Warning", "x Problem Warning")));
        assertFalse(table.contains(new Problem.Error   ("x Section Error",   "x Expression Error",   "x Approver Error",   "x Problem Error")));
        assertFalse(table.contains(new Problem.Fatal   ("x Section Fatal",   "x Expression Fatal",   "x Approver Fatal",   "x Problem Fatal")));

        EnforcerTestLogger logger = new EnforcerTestLogger("testProblemTable");
        table.toLog(logger);
        logger.assertContainsInfo ("| Section Unknown | Expression Unknown | Approver Unknown | Problem Unknown |");
        logger.assertContainsInfo ("| Section Info    | Expression Info    | Approver Info    | Problem Info    |");
        logger.assertContainsWarn ("| Section Warning | Expression Warning | Approver Warning | Problem Warning |");
        logger.assertContainsError("| Section Error   | Expression Error   | Approver Error   | Problem Error   |");
        logger.assertContainsError("| Section Fatal   | Expression Fatal   | Approver Fatal   | Problem Fatal   |");

        table.toLogAsBlock(logger);
    }

}
