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

package nl.basjes.codeowners;

import java.util.List;

public class ApprovalRule extends Rule {
    protected final List<String> approvers;

    public ApprovalRule(String fileExpression, List<String> approvers) {
        super(fileExpression);
        this.approvers = approvers;
    }

    /**
     * @return All approvers (in the same order as they are in the file) that will be returned IF
     * the file pattern matches.
     */
    public List<String> getApprovers() {
        return approvers;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (verbose) {
            result.append("# Regex used for the next rule:   ").append(filePattern).append('\n');
        }
        return result.append(fileExpression).append(" ").append(String.join(" ", approvers)).toString();
    }
}
