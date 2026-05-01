package com.tiv.auto.code.review.service.impl;

import com.tiv.auto.code.review.constants.AutoCodeReviewConstants;
import com.tiv.auto.code.review.constants.ExtensionConstants;
import com.tiv.auto.code.review.constants.LanguageConstants;
import com.tiv.auto.code.review.service.CodeReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CodeReviewServiceImpl implements CodeReviewService {

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

}