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
package nl.basjes.codeowners.validator.utils

import kotlin.math.max

open class StringTable {
    private var headers: List<String> = listOf()
    private val lines: MutableList<List<String>> = mutableListOf()

    var cachedColumnWidths: MutableList<Int>? = null
    protected fun calculateColumnWidths(): MutableList<Int> {
        if (cachedColumnWidths == null) {
            val columnWidths: MutableList<Int> = ArrayList<Int>()
            for (column in headers.indices) {
                var maxWidth = headers[column].length
                for (line in lines) {
                    if (!line.isEmpty() && line.size > column) {
                        val columnValue = line[column]
                        val count = columnValue.codePoints().count()
                        if (count < Int.MAX_VALUE) {
                            maxWidth = max(maxWidth, count.toInt())
                        }
                    }
                }
                columnWidths.add(maxWidth)
            }
            cachedColumnWidths = columnWidths
        }
        return cachedColumnWidths!!
    }

    override fun toString(): String {
        val sb = StringBuilder(1024)
        sb.append(writeSeparator()).append("\n")
        sb.append(writeLine(headers)).append("\n")
        sb.append(writeSeparator()).append("\n")
        for (line in lines) {
            sb.append(writeLine(line)).append("\n")
        }
        sb.append(writeSeparator()).append("\n")
        return sb.toString()
    }

    protected fun writeHeaders(): String {
        return writeLine(headers)
    }

    protected fun writeSeparator(): String {
        val sb = StringBuilder(512)
        var first = true
        for (columnWidth in calculateColumnWidths()) {
            if (first) {
                sb.append('|')
                first = false
            } else {
                sb.append('+')
            }
            sb.append("-".repeat(columnWidth + 2))
        }
        sb.append('|')
        return sb.toString()
    }

    protected fun writeLine(fields: List<String>): String {
        if (fields.isEmpty()) {
            return writeSeparator()
        }

        val columnWidths = calculateColumnWidths()
        val columns = max(columnWidths.size, fields.size)

        val sb = StringBuilder(512)
        for (columnNr in 0..<columns) {
            var columnWidth = 1
            if (columnNr < columnWidths.size) {
                columnWidth = columnWidths[columnNr]
            }
            if (columnNr <= columnWidths.size) {
                sb.append('|')
            }
            var field = ""
            if (columnNr < fields.size) {
                field = fields[columnNr]
            }
            sb.append(String.format(" %-${columnWidth}s ", field)) // NOSONAR java:S3457 This is creative, I know.
        }
        if (columns <= columnWidths.size) {
            sb.append('|')
        }
        return sb.toString()
    }

    fun withHeaders(vararg fields: String): StringTable {
        this.headers = listOf(*fields)
        return this
    }

    fun withHeaders(fields: MutableList<String>): StringTable {
        this.headers = fields.toList()
        return this
    }

    fun addRow(vararg fields: String): StringTable {
        addRow(listOf(*fields))
        return this
    }

    fun addRow(fields: List<String>): StringTable {
        this.lines.add(fields.toList())
        return this
    }

    fun addRowSeparator(): StringTable {
        this.lines.add(listOf()) // Empty array
        return this
    }
}
