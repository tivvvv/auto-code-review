package com.tiv.auto.code.review.service.impl;

import com.tiv.auto.code.review.constants.AutoCodeReviewConstants;
import com.tiv.auto.code.review.constants.ExtensionConstants;
import com.tiv.auto.code.review.constants.LanguageConstants;
import com.tiv.auto.code.review.llm.LLMFactory;
import com.tiv.auto.code.review.service.CodeReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CodeReviewServiceImpl implements CodeReviewService {

    @Autowired
    private LLMFactory llmFactory;

    @Value("${code.review.max-chars:50000}")
    private int maxChars;

    @Value("${code.review.style:professional}")
    private String reviewStyle;

    private static final Map<String, String> extensionToLanguage = new HashMap<>();

    static {
        extensionToLanguage.put(ExtensionConstants.JAVA_EXTENSION, LanguageConstants.JAVA);
        extensionToLanguage.put(ExtensionConstants.JAVASCRIPT_EXTENSION, LanguageConstants.JAVASCRIPT);
        extensionToLanguage.put(ExtensionConstants.TYPESCRIPT_EXTENSION, LanguageConstants.TYPESCRIPT);
        extensionToLanguage.put(ExtensionConstants.PYTHON_EXTENSION, LanguageConstants.PYTHON);
        extensionToLanguage.put(ExtensionConstants.VUE_EXTENSION, LanguageConstants.VUE);
        extensionToLanguage.put(ExtensionConstants.GO_EXTENSION, LanguageConstants.GO);
        extensionToLanguage.put(ExtensionConstants.C_EXTENSION, LanguageConstants.C);
        extensionToLanguage.put(ExtensionConstants.CPP_EXTENSION, LanguageConstants.CPP);
        extensionToLanguage.put(ExtensionConstants.PHP_EXTENSION, LanguageConstants.PHP);
    }

    @Override
    public String reviewCode(String diffText, String commitMessage) {
        if (diffText == null || diffText.isBlank()) {
            return null;
        }
        // 1. 检测编程语言
        String detectedLanguage = detectCodeLanguage(diffText);
        log.info("检测到编程语言是: {}", detectedLanguage);

        // 2. 如果代码过长则截断
        if (diffText.length() > maxChars) {
            log.info("代码过长, 截断 {} 字符", diffText.length() - maxChars);
            diffText = diffText.substring(0, maxChars);
            String secondDetectedLanguage = detectCodeLanguage(diffText);
            if (!LanguageConstants.DEFAULT.equals(secondDetectedLanguage)) {
                detectedLanguage = secondDetectedLanguage;
            }
        }

        // 3. 调用大模型评审代码
        return doReviewCode(diffText, commitMessage, detectedLanguage);
    }

    private String doReviewCode(String diffText, String commitMessage, String detectedLanguage) {
        Map<String, String> prompts;
        try {
            prompts = loadPrompts(detectedLanguage, reviewStyle);
        } catch (Exception e) {
            log.warn("加载语言特定提示词失败, 使用通用提示词: {}", e.getMessage());
            try {
                prompts = loadPrompts(LanguageConstants.DEFAULT, reviewStyle);
            } catch (Exception ex) {
                log.error("加载通用提示词失败: {}", ex.getMessage());
                throw new RuntimeException("加载提示词失败", ex);
            }
        }
        String systemContent = prompts.get(AutoCodeReviewConstants.SYSTEM_PROMPT);
        String userContent = prompts.get(AutoCodeReviewConstants.USER_PROMPT)
                .replace("{diff_text}", diffText)
                .replace("{commit_message}", commitMessage);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(AutoCodeReviewConstants.ROLE, AutoCodeReviewConstants.SYSTEM,
                AutoCodeReviewConstants.CONTENT, systemContent));
        messages.add(Map.of(AutoCodeReviewConstants.ROLE, AutoCodeReviewConstants.USER,
                AutoCodeReviewConstants.CONTENT, userContent));

        return llmFactory.getClient().chat(messages);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadPrompts(String language, String style) {
        Yaml yaml = new Yaml();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(AutoCodeReviewConstants.PROMPT_TEMPLATES_FILE)) {
            if (is == null) {
                throw new RuntimeException("未找到提示词模版文件");
            }
            Map<String, Object> allPrompts = yaml.load(is);
            Map<String, String> prompts = (Map<String, String>) allPrompts.get(language);
            if (prompts == null) {
                throw new RuntimeException("未找到指定语言的提示词: " + language);
            }
            String systemPrompt = prompts.get(AutoCodeReviewConstants.SYSTEM_PROMPT);
            String userPrompt = prompts.get(AutoCodeReviewConstants.USER_PROMPT);

            systemPrompt = processPromptTemplate(systemPrompt, style);
            userPrompt = processPromptTemplate(userPrompt, style);

            return Map.of(AutoCodeReviewConstants.SYSTEM_PROMPT, systemPrompt,
                    AutoCodeReviewConstants.USER_PROMPT, userPrompt);
        } catch (IOException e) {
            log.error("加载提示词模版失败", e);
            throw new RuntimeException("加载提示词模版失败: " + e.getMessage());
        }
    }

    private String processPromptTemplate(String promptTemplate, String style) {
        if (promptTemplate == null) return "";

        // 替换 {{ style }} 变量
        promptTemplate = promptTemplate.replace("{{ style }}", style);

        // 处理 {% if style == 'xxx' %} ... {% elif ... %} ... {% endif %} 块
        promptTemplate = processStyleConditions(promptTemplate, style);

        return promptTemplate;
    }

    private String processStyleConditions(String promptTemplate, String style) {
        // 处理多重条件块
        StringBuilder result = new StringBuilder();
        int pos = 0;

        while (pos < promptTemplate.length()) {
            int ifStart = promptTemplate.indexOf("{% if style ==", pos);
            if (ifStart == -1) {
                result.append(promptTemplate.substring(pos));
                break;
            }

            // 添加if之前的内容
            result.append(promptTemplate, pos, ifStart);

            // 找到对应的endif
            int endIfPos = promptTemplate.indexOf("{% endif %}", ifStart);
            if (endIfPos == -1) {
                result.append(promptTemplate.substring(ifStart));
                break;
            }

            // 提取整个条件块
            String condBlock = promptTemplate.substring(ifStart, endIfPos + 11);
            String resolved = resolveConditionBlock(condBlock, style);
            result.append(resolved);

            pos = endIfPos + 11;
        }

        return result.toString();
    }

    private String resolveConditionBlock(String block, String style) {
        // 解析所有分支
        List<String[]> branches = new ArrayList<>(); // [condition, content]

        String current = block;
        // 找到第一个if
        int firstIfEnd = current.indexOf("%}");
        if (firstIfEnd == -1) return "";

        String firstCondLine = current.substring(0, firstIfEnd + 2);
        String firstCondValue = extractStyleValue(firstCondLine);
        current = current.substring(firstIfEnd + 2);

        // 寻找elif和else块
        String currentCond = firstCondValue;
        int searchPos = 0;
        int blockStart = 0;

        while (searchPos < current.length()) {
            int elifPos = current.indexOf("{%", searchPos);
            if (elifPos == -1) {
                branches.add(new String[]{currentCond, current.substring(blockStart)});
                break;
            }
            int blockEnd = current.indexOf("%}", elifPos);
            if (blockEnd == -1) break;

            String tag = current.substring(elifPos, blockEnd + 2).trim();

            if (tag.startsWith("{% elif") || tag.startsWith("{%elif") || tag.startsWith("{% else") || tag.startsWith("{%else")) {
                branches.add(new String[]{currentCond, current.substring(blockStart, elifPos)});

                if (tag.contains("style ==")) {
                    currentCond = extractStyleValue(tag);
                } else {
                    currentCond = "else";
                }
                blockStart = blockEnd + 2;
                searchPos = blockEnd + 2;
            } else if (tag.startsWith("{% endif") || tag.startsWith("{%endif")) {
                branches.add(new String[]{currentCond, current.substring(blockStart, elifPos)});
                break;
            } else {
                searchPos = blockEnd + 2;
            }
        }

        // 找到匹配的分支
        for (String[] branch : branches) {
            if (style.equals(branch[0]) || "else".equals(branch[0])) {
                return branch[1].strip();
            }
        }

        return "";
    }

    private String extractStyleValue(String condLine) {
        Pattern p = Pattern.compile("style\\s*==\\s*'([^']+)'");
        Matcher m = p.matcher(condLine);
        if (m.find()) {
            return m.group(1);
        }
        return "else";
    }

    @Override
    public String detectCodeLanguage(String text) {
        if (text == null || text.isBlank()) {
            return AutoCodeReviewConstants.DEFAULT_LANGUAGE;
        }
        Map<String, Integer> languageCounts = new HashMap<>();

        String[] filePatterns = {
                ".*\\+\\+\\+ b/(.+)$",
                ".*\\+\\+\\+ (.+)$",
                ".*--- a/(.+)$",
                ".*--- (.+)$"
        };

        String[] lines = text.split("\n");
        for (String line : lines) {
            for (String pattern : filePatterns) {
                Matcher matcher = Pattern.compile(pattern).matcher(line);
                if (matcher.find()) {
                    String filePath = matcher.group(1);
                    String extension = getExtension(filePath);
                    if (extension == null) {
                        continue;
                    }
                    String language = extensionToLanguage.get(extension.toLowerCase());
                    if (language == null) {
                        continue;
                    }
                    languageCounts.merge(language, 1, Integer::sum);
                    break;
                }
            }

            Matcher diffGitMatcher = Pattern.compile("^diff --git a/(.+) b/(.+)$").matcher(line);
            if (diffGitMatcher.find()) {
                String filePath = diffGitMatcher.group(1);
                String extension = getExtension(filePath);
                if (extension == null) {
                    continue;
                }
                String language = extensionToLanguage.get(extension.toLowerCase());
                if (language != null) {
                    languageCounts.merge(language, 1, Integer::sum);
                }
            }
        }

        if (languageCounts.isEmpty()) {
            String diffsLower = text.toLowerCase();
            if (containsAny(diffsLower, "<template>", "<script>", "<style>", ".vue")) {
                languageCounts.put(LanguageConstants.VUE, 1);
            } else if (containsAny(diffsLower, "function", "var ", "let ", "const ", "console.log", "document.")) {
                languageCounts.put(LanguageConstants.JAVASCRIPT, 1);
            } else if (containsAny(diffsLower, "def ", "import ", "from ", "class ", "if __name__", "self.")) {
                languageCounts.put(LanguageConstants.PYTHON, 1);
            }
        }

        if (!languageCounts.isEmpty()) {
            String primaryLang = languageCounts.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(LanguageConstants.DEFAULT);
            log.info("检测到主要编程语言: {}", primaryLang);
            return primaryLang;
        }

        return LanguageConstants.DEFAULT;
    }

    private String getExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filePath.length() - 1) {
            return null;
        }
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash > lastDot) {
            return null;
        }
        return filePath.substring(lastDot);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int parseCodeReviewScore(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return 0;
        }
        Pattern pattern = Pattern.compile("总分:\\s*(\\d+)分?");
        Matcher matcher = pattern.matcher(reviewText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

}