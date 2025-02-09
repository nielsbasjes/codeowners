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

import java.util.ArrayList;
import java.util.List;

import static nl.basjes.codeowners.CodeOwners.LOG;
import static nl.basjes.codeowners.CodeOwnersLoader.IMPLICIT_SECTION_NAME;

public class Section {
    private boolean verbose = false;
    boolean optional = false;
    private final String name;
    private int minimalNumberOfApprovers = 0;
    private final List<String> defaultApprovers = new ArrayList<>();
    private final List<ApprovalRule> approvalRules = new ArrayList<>();

    public Section(String name) {
        this.name = name;
    }

    void addDefaultApprover(String name) {
        String cleanedName = name.trim();
        if (!defaultApprovers.contains(cleanedName)) {
            defaultApprovers.add(cleanedName);
        }
    }

    void addApprovalRule(ApprovalRule rule) {
        approvalRules.add(rule);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        approvalRules.forEach(rule -> rule.setVerbose(verbose));
    }

    public String getName() {
        return name;
    }

    public List<String> getDefaultApprovers() {
        return defaultApprovers;
    }

    public List<ApprovalRule> getApprovalRules() {
        return approvalRules;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isDefaultSection() {
        return IMPLICIT_SECTION_NAME.equals(name);
    }

    void setMinimalNumberOfApprovers(int minimalNumberOfApprovers) {
        this.minimalNumberOfApprovers = minimalNumberOfApprovers;
    }

    public int getMinimalNumberOfApprovers() {
        return minimalNumberOfApprovers;
    }

    /**
     * @param filename The filename for which the approvers are requested.
     * @return The list of approver usernames for this filename in the order (as good as possible) as they appear in the code owner rules in this section.
     */
    public List<String> getApprovers(String filename) {
        if (verbose) {
            LOG.info("# ---------------------------");
            LOG.info("# Section [{}]", getName());
        }
        List<String> approvers = new ArrayList<>();
        for (ApprovalRule approvalRule : approvalRules) {
            List<String> ruleApprovers = approvalRule.getApprovers(filename);
            if (ruleApprovers != null) {
                // GitHub: Order is important; the last matching pattern takes the most precedence.
                // Gitlab: When a file or directory matches multiple entries in the CODEOWNERS file, the users from last pattern matching the file or directory are used.
                approvers.clear();
                if (ruleApprovers.isEmpty()) {
                    if (verbose) {
                        LOG.info("-- MATCH WITHOUT APPROVERS --> Using Default approvers {}", defaultApprovers);
                    }
                    approvers.addAll(defaultApprovers);
                } else {
                    approvers.addAll(ruleApprovers);
                }
            }
        }
        if (verbose) {
            LOG.info("# Section [{}] approvers: {}", getName(), approvers);
            LOG.info("# ---------------------------");
        }
        return approvers;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (optional) {
            result.append('^');
        }
        result.append('[').append(name).append(']');
        if (minimalNumberOfApprovers > 0) {
            result.append('[').append(minimalNumberOfApprovers).append(']');
        }
        if (!defaultApprovers.isEmpty()) {
            result.append(' ').append(String.join(" ", defaultApprovers));
        }
        result.append('\n');
        for (ApprovalRule approvalRule : approvalRules) {
            result.append(approvalRule).append('\n');
        }
        return result.toString();
    }
}
