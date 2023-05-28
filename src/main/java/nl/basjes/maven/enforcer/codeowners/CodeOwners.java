package nl.basjes.maven.enforcer.codeowners;

import nl.basjes.codeowners.CodeOwnersBaseVisitor;
import nl.basjes.codeowners.CodeOwnersLexer;
import nl.basjes.codeowners.CodeOwnersParser;
import nl.basjes.codeowners.CodeOwnersParser.ApprovalRuleContext;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STGroupString;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CodeOwners extends CodeOwnersBaseVisitor<Void> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeOwners.class);

    public Map<String, Section> getSections() {
        return sections;
    }

    // Map name of Section to Sections
    private Map<String, Section> sections = new TreeMap<>();

    // Load the code owners from a file
    public CodeOwners(File file) throws IOException {
        this(FileUtils.readFileToString(file, UTF_8));
    }

    public CodeOwners(String codeownersContent) {
        currentSection = new Section("Implicit Default Section");

        CodePointCharStream input = CharStreams.fromString(codeownersContent);
        CodeOwnersLexer lexer = new CodeOwnersLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CodeOwnersParser parser = new CodeOwnersParser(tokens);
        CodeOwnersParser.CodeownersContext codeowners = parser.codeowners();
        visit(codeowners);

        // Make sure we retain the last section also
        // Only if the previous Section had ANY rules do we keep it.
        if (!currentSection.approvalRules.isEmpty()) {
            sections.put(currentSection.name, currentSection);
        }
        warnAboutStructuralProblems();
    }

    private void warnAboutStructuralProblems() {
        for (Section section : sections.values()) {
            Integer minimalNumberOfApprovers = section.getMinimalNumberOfApprovers();
            if (minimalNumberOfApprovers != null) {
                if (section.isOptional() && minimalNumberOfApprovers != 0) {
                    LOG.warn("CODEOWNERS Section \"{}\" is Optional so the specified MinimalNumberOfApprovers {} is IGNORED!",
                            section.getName(), minimalNumberOfApprovers);
                }
            }
        }
    }

    public List<String> getMandatoryApprovers(String filename) {
        List<String> approvers = new ArrayList<>();
        for (Section section: sections.values()) {
            if (!section.isOptional()) {
                approvers.addAll(section.getApprovers(filename));
            }
        }
        return approvers.stream().sorted().distinct().collect(Collectors.toList());
    }

    public List<String> getAllApprovers(String filename) {
        List<String> approvers = new ArrayList<>();
        for (Section section: sections.values()) {
            approvers.addAll(section.getApprovers(filename));
        }
        return approvers.stream().sorted().distinct().collect(Collectors.toList());
    }

    private Section currentSection;

    @Override
    public Void visitSection(CodeOwnersParser.SectionContext ctx) {
        String sectionName = ctx.section.getText();
        Section section = sections.get(sectionName);
        if (section == null) {
            // New section.
            section = new Section(sectionName);
            section.optional = ctx.OPTIONAL() != null;
            if (ctx.approvers != null) {
                section.setMinimalNumberOfApprovers(Integer.valueOf(ctx.approvers.getText().trim()));
            }
            for (TerminalNode user : ctx.USERID()) {
                section.addDefaultApprover(user.getText());
            }

        }

        // Only if the previous Section had ANY rules do we keep it.
        if (!currentSection.approvalRules.isEmpty()) {
            sections.put(currentSection.name, currentSection);
        }
        currentSection = section;
        return null;
    }

    @Override
    public Void visitApprovalRule(ApprovalRuleContext ctx) {
        String filePattern = ctx.fileExpression.getText();
        List<String> approvers = ctx.USERID().stream()
                .map(ParseTree::getText)
                .map(String::trim)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        currentSection.addApprovalRule(new ApprovalRule(filePattern, approvers));
        return null;
    }

    @Override
    public String toString() {
        return ST_GROUP_STRING.getInstanceOf("CodeOwners").add("codeowners", this).render();
    }

    public static class Section {
        boolean optional = false;
        String name;
        Integer minimalNumberOfApprovers = null;
        List<String> defaultApprovers = new ArrayList<>();
        List<ApprovalRule> approvalRules = new ArrayList<>();

        public Section(String name) {
            this.name = name;
        }

        void addDefaultApprover(String name) {
            if (!defaultApprovers.contains(name)) {
                defaultApprovers.add(name);
            }
        }

        void addApprovalRule(ApprovalRule rule) {
            approvalRules.add(rule);
        }

        void setOptional(boolean optional) {
            this.optional = optional;
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

        void setMinimalNumberOfApprovers(Integer minimalNumberOfApprovers) {
            this.minimalNumberOfApprovers = minimalNumberOfApprovers;
        }

        public Integer getMinimalNumberOfApprovers() {
            return minimalNumberOfApprovers;
        }

        public List<String> getApprovers(String filename) {
            List<String> approvers = new ArrayList<>();
            for (ApprovalRule approvalRule : approvalRules) {
                List<String> ruleApprovers = approvalRule.getApprovers(filename);
                if (ruleApprovers != null) {
                    // GitHub: Order is important; the last matching pattern takes the most precedence.
                    // Gitlab: When a file or directory matches multiple entries in the CODEOWNERS file, the users from last pattern matching the file or directory are used.
                    approvers.clear();
                    approvers.addAll(ruleApprovers);
                }
            }
            return approvers;
        }

        @Override
        public String toString() {
            return ST_GROUP_STRING.getInstanceOf("Section").add("section", this).render();
        }
    }

    public static class ApprovalRule {
        private final String fileExpression;
        private final List<String> approvers;

        private final Pattern filePattern;

        public ApprovalRule(String fileExpression, List<String> approvers) {
            this.fileExpression = fileExpression;
            this.approvers = approvers;

            String fileRegex = fileExpression
                    .replace(".", "\\.") // Avoid bad wildcards
                    .replace("**",".*") // Convert to the Regex wildcards
                    // "/foo" --> End can be a filename (so we pin to the end) or a directory name (so we expect another / )
                    .replaceAll("([^/*])$", "$1(/|\\$)")

                    .replaceAll("/\\*$","/[^/]+\\$") // A trailing '/*' means no further subdirs should be matched
                    .replaceAll("^\\*", ".*") // Match anything at the start
                    .replaceAll("^/", "^/") // If starts with / then pin to the start.
                    ;

            LOG.info("{}     -->     {}", fileExpression, fileRegex);
            filePattern = Pattern.compile(fileRegex);
        }

        public String getFileExpression() {
            return fileExpression;
        }

        public List<String> getApprovers() {
            return new ArrayList<>(approvers);
        }

        public List<String> getApprovers(String filename) {
            if (!filePattern.matcher(filename).find()) {
                return null;
            }
            return new ArrayList<>(approvers);
        }

        @Override
        public String toString() {
            return ST_GROUP_STRING.getInstanceOf("ApprovalRule").add("approvalRule", this).render();
        }
    }

    private static final STGroupString ST_GROUP_STRING = new STGroupString(
        "CodeOwners(codeowners) ::= <<\n" +
        "CodeOwnwer:\n" +
        "  <codeowners.sections.values:Section() ;separator=\"\n\n\">\n" +
        ">>\n" +

        "Section(section) ::= <<\n" +
        "<if(section.optional)>^<endif>" +
            "[<section.name>]" +
            "<if(section.minimalNumberOfApprovers)>[<section.minimalNumberOfApprovers>]<endif>" +
            "<if(section.defaultApprovers)> Default Users: <section.defaultApprovers;separator=\" ~ \"><endif>" +
        "<section.approvalRules:{ rule | - <ApprovalRule(rule)>};separator=\"\n\">\n" +
        ">>\n" +

        "ApprovalRule(approvalRule) ::= <<\n" +
        "<approvalRule.fileExpression><if(approvalRule.approvers)> <approvalRule.approvers; separator=\",\"><else>#NO Approvers specified<endif>\n" +
        ">>\n"
    );
}
