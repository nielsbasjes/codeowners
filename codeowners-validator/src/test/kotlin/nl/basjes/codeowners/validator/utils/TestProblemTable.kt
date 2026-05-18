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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestProblemTable {
    @Test
    fun testProblemTable() {
        val logger = TestLogger("testProblemTable")

        val table = ProblemTable()
        table.addProblem(Problem.Info("Section Info", "Expression Info", "Approver Info", "Problem Info"))
        table.toLogAsBlock(logger)
        assertEquals(1, table.numberOfProblems)
        table.addProblem(
            Problem.Warning(
                "Section Warning",
                "Expression Warning",
                "Approver Warning",
                "Problem Warning"
            )
        )
        table.toLogAsBlock(logger)
        assertEquals(2, table.numberOfProblems)
        table.addProblem(Problem.Error("Section Error", "Expression Error", "Approver Error", "Problem Error"))
        table.toLogAsBlock(logger)
        assertEquals(3, table.numberOfProblems)
        table.addProblem(Problem.Fatal("Section Fatal", "Expression Fatal", "Approver Fatal", "Problem Fatal"))
        table.toLogAsBlock(logger)
        assertEquals(4, table.numberOfProblems)

        assertTrue(
            table.contains(
                Problem.Info(
                    "Section Info",
                    "Expression Info",
                    "Approver Info",
                    "Problem Info"
                )
            )
        )
        assertTrue(
            table.contains(
                Problem.Warning(
                    "Section Warning",
                    "Expression Warning",
                    "Approver Warning",
                    "Problem Warning"
                )
            )
        )
        assertTrue(
            table.contains(
                Problem.Error(
                    "Section Error",
                    "Expression Error",
                    "Approver Error",
                    "Problem Error"
                )
            )
        )
        assertTrue(
            table.contains(
                Problem.Fatal(
                    "Section Fatal",
                    "Expression Fatal",
                    "Approver Fatal",
                    "Problem Fatal"
                )
            )
        )

        assertFalse(
            table.contains(
                Problem.Info(
                    "x Section Info",
                    "x Expression Info",
                    "x Approver Info",
                    "x Problem Info"
                )
            )
        )
        assertFalse(
            table.contains(
                Problem.Warning(
                    "x Section Warning",
                    "x Expression Warning",
                    "x Approver Warning",
                    "x Problem Warning"
                )
            )
        )
        assertFalse(
            table.contains(
                Problem.Error(
                    "x Section Error",
                    "x Expression Error",
                    "x Approver Error",
                    "x Problem Error"
                )
            )
        )
        assertFalse(
            table.contains(
                Problem.Fatal(
                    "x Section Fatal",
                    "x Expression Fatal",
                    "x Approver Fatal",
                    "x Problem Fatal"
                )
            )
        )

        table.toLog(logger)
        logger.assertContainsInfo("| Section Info    | Expression Info    | Approver Info    | Problem Info    |")
        logger.assertContainsWarn("| Section Warning | Expression Warning | Approver Warning | Problem Warning |")
        logger.assertContainsError("| Section Error   | Expression Error   | Approver Error   | Problem Error   |")
        logger.assertContainsError("| Section Fatal   | Expression Fatal   | Approver Fatal   | Problem Fatal   |")

        table.toLogAsBlock(logger)
    }
}
