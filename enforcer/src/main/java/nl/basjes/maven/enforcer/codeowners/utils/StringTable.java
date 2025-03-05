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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringTable {

    private List<String> headers = new ArrayList<>();
    private final List<List<String>> lines = new ArrayList<>();

    List<Integer> cachedColumnWidths = null;
    protected List<Integer> calculateColumnWidths() {
        if (cachedColumnWidths == null) {
            List<Integer> columnWidths = new ArrayList<>();
            for (int column = 0; column < headers.size(); column++) {
                int maxWidth = headers.get(column).length();
                for (List<String> line : lines) {
                    if (!line.isEmpty() && line.size() > column) {
                        String columnValue = line.get(column);
                        if (columnValue != null) {
                            long count = columnValue.codePoints().count();
                            if (count < Integer.MAX_VALUE) {
                                maxWidth =  Math.max(maxWidth, (int) count);
                            }
                        }
                    }
                }
                columnWidths.add(maxWidth);
            }
            cachedColumnWidths = columnWidths;
        }
        return cachedColumnWidths;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(writeSeparator()).append("\n");
        sb.append(writeLine(headers)).append("\n");
        sb.append(writeSeparator()).append("\n");
        for (List<String> line : lines) {
            sb.append(writeLine(line)).append("\n");
        }
        sb.append(writeSeparator()).append("\n");
        return sb.toString();
    }

    protected String writeHeaders() {
        return writeLine(headers);
    }

    protected String writeSeparator() {
        StringBuilder sb = new StringBuilder(512);
        boolean first = true;
        for (Integer columnWidth : calculateColumnWidths()) {
            if (first) {
                sb.append('|');
                first = false;
            } else {
                sb.append('+');
            }
            sb.append("-".repeat(columnWidth+2));
        }
        sb.append('|');
        return sb.toString();
    }

    protected String writeLine(List<String> fields) {
        if (fields.isEmpty()) {
            return writeSeparator();
        }

        List<Integer> columnWidths = calculateColumnWidths();
        int columns = Math.max(columnWidths.size(), fields.size());

        StringBuilder sb = new StringBuilder(512);
        for (int columnNr = 0; columnNr < columns; columnNr++) {
            int columnWidth = 1;
            if (columnNr < columnWidths.size()) {
                columnWidth = columnWidths.get(columnNr);
            }
            if (columnNr <= columnWidths.size()) {
                sb.append('|');
            }
            String field = "";
            if (columnNr < fields.size()) {
                field = fields.get(columnNr);
            }
            sb.append(String.format(" %-" + columnWidth + "s ", field)); // NOSONAR java:S3457 This is creative, I know.
        }
        if (columns <= columnWidths.size()) {
            sb.append('|');
        }
        return sb.toString();
    }

    public StringTable withHeaders(String... fields) {
        this.headers = Arrays.asList(fields);
        return this;
    }

    public StringTable withHeaders(List<String> fields) {
        this.headers = new ArrayList<>(fields);
        return this;
    }

    public StringTable addRow(String... fields) {
        this.lines.add(Arrays.asList(fields));
        return this;
    }

    public StringTable addRow(List<String> fields) {
        this.lines.add(new ArrayList<>(fields));
        return this;
    }

    public StringTable addRowSeparator() {
        this.lines.add(new ArrayList<>()); // Empty array
        return this;
    }

}
