package com.lsx.workorder.ai.provider.impl;

import com.lsx.workorder.ai.dto.AiWorkOrderAnalyzeResult;
import com.lsx.workorder.ai.provider.AiWorkOrderAnalysisProvider;
import com.lsx.workorder.repair.entity.Repair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleAiWorkOrderAnalysisProvider implements AiWorkOrderAnalysisProvider {
    private static final String PROVIDER = "RULE";
    private static final String PROVIDER_VERSION = "rule-v1.1";

    private static final List<CategoryRule> CATEGORY_RULES = Arrays.asList(
            new CategoryRule(
                    "ELEVATOR",
                    "\u7535\u68af\u7ef4\u4fdd\u7ec4",
                    5,
                    3,
                    "\u4f4f\u6237\u53cd\u9988\u7591\u4f3c\u7535\u68af\u6545\u969c\u6216\u8fd0\u884c\u5f02\u5e38\u3002",
                    "\u5efa\u8bae\u7acb\u5373\u8054\u7cfb\u7535\u68af\u7ef4\u4fdd\u7ec4\u6838\u67e5\u6545\u969c\u72b6\u6001\uff0c\u5e76\u7531\u540e\u53f0\u4eba\u5458\u540c\u6b65\u5b89\u629a\u53d7\u5f71\u54cd\u4f4f\u6237\u3002",
                    "\u4fdd\u6301\u4e0e\u88ab\u56f0\u4eba\u5458\u901a\u8bdd\uff0c\u63d0\u9192\u4e0d\u8981\u5f3a\u884c\u6252\u95e8\u3002",
                    "\u540e\u53f0\u4eba\u5458\u5e94\u7acb\u5373\u8054\u7cfb\u7535\u68af\u7ef4\u4fdd\u548c\u503c\u73ed\u7269\u4e1a\u3002",
                    "elevator", "lift", "stuck", "\u7535\u68af\u6545\u969c", "\u7535\u68af\u505c\u8fd0",
                    "\u7535\u68af\u56f0\u4eba", "\u56f0\u4eba", "\u88ab\u56f0", "\u8f7f\u53a2", "\u5347\u964d", "\u68af\u95e8"
            ),
            new CategoryRule(
                    "ELECTRICAL",
                    "\u7535\u5de5\u7ef4\u4fee\u7ec4",
                    4,
                    3,
                    "\u4f4f\u6237\u53cd\u9988\u7591\u4f3c\u7535\u6c14\u6545\u969c\u6216\u7528\u7535\u98ce\u9669\u3002",
                    "\u5efa\u8bae\u5148\u63d0\u9192\u4f4f\u6237\u8fdc\u79bb\u7591\u4f3c\u6545\u969c\u7535\u5668\u548c\u63d2\u5ea7\uff0c\u5fc5\u8981\u65f6\u5173\u95ed\u76f8\u5173\u7535\u6e90\uff0c\u518d\u5b89\u6392\u7535\u5de5\u7ef4\u4fee\u7ec4\u5904\u7406\u3002",
                    "\u4e0d\u8981\u89e6\u78b0\u7591\u4f3c\u6f0f\u7535\u8bbe\u5907\u6216\u6f6e\u6e7f\u63d2\u5ea7\u3002",
                    "\u5fc5\u8981\u65f6\u5148\u5173\u95ed\u76f8\u5173\u7a7a\u5f00\uff0c\u7b49\u5f85\u7ef4\u4fee\u4eba\u5458\u5230\u573a\u3002",
                    "electrical", "electric", "power", "socket", "wire", "short circuit", "light",
                    "\u505c\u7535", "\u8df3\u95f8", "\u77ed\u8def", "\u63d2\u5ea7", "\u7535\u7ebf",
                    "\u7535\u95f8", "\u7167\u660e", "\u706f\u4e0d\u4eae", "\u6f0f\u7535", "\u706b\u82b1",
                    "\u914d\u7535\u623f", "\u70e7\u7126"
            ),
            new CategoryRule(
                    "WATER",
                    "\u6c34\u6696\u7ef4\u4fee\u7ec4",
                    3,
                    3,
                    "\u4f4f\u6237\u53cd\u9988\u7591\u4f3c\u6f0f\u6c34\u3001\u6392\u6c34\u6216\u7ba1\u9053\u95ee\u9898\u3002",
                    "\u5efa\u8bae\u5148\u8054\u7cfb\u4f4f\u6237\u786e\u8ba4\u6f0f\u6c34\u8303\u56f4\uff0c\u63d0\u9192\u5173\u95ed\u5c31\u8fd1\u9600\u95e8\uff0c\u5e76\u5b89\u6392\u6c34\u6696\u7ef4\u4fee\u7ec4\u4e0a\u95e8\u5904\u7406\u3002",
                    "\u63d0\u9192\u4f4f\u6237\u5148\u5173\u95ed\u5c31\u8fd1\u9600\u95e8\u3002",
                    "\u79fb\u5f00\u9644\u8fd1\u7535\u5668\u3001\u8d35\u91cd\u7269\u54c1\u548c\u6613\u53d7\u6f6e\u7269\u54c1\u3002",
                    "water", "pipe", "leak", "drain", "toilet", "\u6f0f\u6c34", "\u6e17\u6c34",
                    "\u7206\u7ba1", "\u6c34\u7ba1", "\u4e0b\u6c34\u9053", "\u5835\u585e",
                    "\u9a6c\u6876", "\u6392\u6c34", "\u79ef\u6c34", "\u6c34\u69fd", "\u5730\u6f0f"
            ),
            new CategoryRule(
                    "PUBLIC_FACILITY",
                    "\u516c\u5171\u8bbe\u65bd\u7ef4\u4fee\u7ec4",
                    2,
                    2,
                    "\u4f4f\u6237\u53cd\u9988\u7591\u4f3c\u516c\u5171\u8bbe\u65bd\u3001\u901a\u884c\u6216\u5b89\u9632\u8bbe\u5907\u95ee\u9898\u3002",
                    "\u5efa\u8bae\u6838\u5bf9\u8bbe\u5907\u70b9\u4f4d\u548c\u5f71\u54cd\u8303\u56f4\uff0c\u4f18\u5148\u6062\u590d\u901a\u884c\u3001\u5b89\u9632\u6216\u6d88\u9632\u76f8\u5173\u80fd\u529b\u3002",
                    "\u82e5\u5f71\u54cd\u51fa\u5165\uff0c\u5148\u5b89\u6392\u73b0\u573a\u5f15\u5bfc\u6216\u4e34\u65f6\u901a\u884c\u65b9\u6848\u3002",
                    "\u6d89\u53ca\u6d88\u9632\u6216\u5b89\u9632\u8bbe\u5907\u65f6\uff0c\u9700\u540c\u6b65\u901a\u77e5\u503c\u73ed\u8d1f\u8d23\u4eba\u3002",
                    "access control", "gate", "camera", "monitor", "fire alarm", "\u95e8\u7981",
                    "\u9053\u95f8", "\u76d1\u63a7", "\u6444\u50cf\u5934", "\u6d88\u9632",
                    "\u70df\u611f", "\u62a5\u8b66", "\u5c97\u4ead", "\u5355\u5143\u95e8"
            ),
            new CategoryRule(
                    "BUILDING",
                    "\u571f\u5efa\u7ef4\u4fee\u7ec4",
                    1,
                    2,
                    "\u4f4f\u6237\u53cd\u9988\u7591\u4f3c\u5899\u9762\u3001\u95e8\u7a97\u6216\u697c\u5185\u6784\u4ef6\u635f\u574f\u3002",
                    "\u5efa\u8bae\u5148\u6838\u5b9e\u73b0\u573a\u7834\u635f\u8303\u56f4\uff0c\u786e\u8ba4\u662f\u5426\u5b58\u5728\u6389\u843d\u3001\u5212\u4f24\u6216\u901a\u884c\u9690\u60a3\u3002",
                    "\u63d0\u9192\u4f4f\u6237\u6216\u73b0\u573a\u4eba\u5458\u8fdc\u79bb\u53ef\u80fd\u6389\u843d\u7684\u5899\u9762\u3001\u73bb\u7483\u6216\u6784\u4ef6\u3002",
                    "\u5982\u5f71\u54cd\u516c\u5171\u901a\u884c\uff0c\u5e94\u5148\u8bbe\u7f6e\u4e34\u65f6\u63d0\u793a\u6216\u9694\u79bb\u3002",
                    "building", "wall", "tile", "crack", "window", "glass", "\u5899\u9762",
                    "\u5730\u7816", "\u95e8\u7a97", "\u73bb\u7483", "\u88c2\u7f1d", "\u8131\u843d",
                    "\u697c\u9053", "\u6276\u624b"
            )
    );

    private static final String[] HIGH_RISK_KEYWORDS = {
            "trapped", "smoke", "fire", "spark", "gas", "electric shock", "electric leak", "burst",
            "flooding", "cannot enter", "\u56f0\u4eba", "\u88ab\u56f0", "\u5192\u70df",
            "\u8d77\u706b", "\u706b\u707e", "\u706b\u82b1", "\u71c3\u6c14", "\u7164\u6c14",
            "\u6f0f\u7535", "\u7206\u7ba1", "\u5927\u9762\u79ef", "\u65e0\u6cd5\u8fdb\u51fa",
            "\u79ef\u6c34\u4e25\u91cd"
    };

    private static final String[] UNCERTAIN_KEYWORDS = {
            "maybe", "not sure", "unknown", "unclear", "possible", "suspect",
            "\u4e0d\u786e\u5b9a", "\u4e0d\u6e05\u695a", "\u53ef\u80fd", "\u7591\u4f3c",
            "\u597d\u50cf", "\u4e5f\u8bb8"
    };

    private static final List<Pattern> LOCATION_PATTERNS = Arrays.asList(
            Pattern.compile("([0-9\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341]+(?:\u53f7\u697c|\u680b|\u5e62)(?:[0-9\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341]+\u5355\u5143)?(?:[0-9]{2,4}(?:\u5ba4|\u6237)?)?(?:[^\uff0c\u3002,.\uff1b;\\s]{0,12})?)"),
            Pattern.compile("((?:\u5730\u4e0b\u8f66\u5e93|\u8d1f\u4e00\u5c42\u8f66\u5e93|\u5730\u5e93)[A-Za-z0-9\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\\-]*\u533a?(?:[^\uff0c\u3002,.\uff1b;\\s]{0,8})?)"),
            Pattern.compile("((?:\u4e1c\u95e8|\u897f\u95e8|\u5357\u95e8|\u5317\u95e8)(?:\u5c97\u4ead|\u95e8\u7981|\u9053\u95f8)?(?:\u65c1|\u9644\u8fd1)?)"),
            Pattern.compile("((?:[0-9\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341]+\u53f7\u697c)?(?:\u5927\u5385|\u7535\u68af\u53e3|\u697c\u9053|\u8d70\u5eca|\u5783\u573e\u623f|\u914d\u7535\u623f|\u6c34\u6cf5\u623f|\u5355\u5143\u95e8)(?:\u65c1|\u9644\u8fd1)?)")
    );

    private static final String[] LOCATION_STOP_WORDS = {
            "\u6f0f\u6c34", "\u6e17\u6c34", "\u79ef\u6c34", "\u505c\u7535", "\u8df3\u95f8",
            "\u77ed\u8def", "\u6f0f\u7535", "\u5192\u70df", "\u8d77\u706b", "\u6253\u4e0d\u5f00",
            "\u635f\u574f", "\u6545\u969c", "\u574f\u4e86", "\u4e0d\u4eae", "\u6709"
    };

    @Override
    public AiWorkOrderAnalyzeResult analyze(Repair repair) {
        String sourceText = buildText(repair);
        String normalizedText = sourceText.toLowerCase(Locale.ROOT);
        Set<String> matchedKeywords = new LinkedHashSet<>();

        boolean highRisk = matchAny(normalizedText, matchedKeywords, HIGH_RISK_KEYWORDS);
        boolean uncertain = matchAny(normalizedText, matchedKeywords, UNCERTAIN_KEYWORDS);

        List<CategoryMatch> categoryMatches = new ArrayList<>();
        for (CategoryRule rule : CATEGORY_RULES) {
            Set<String> categoryMatchedKeywords = new LinkedHashSet<>();
            if (matchAny(normalizedText, categoryMatchedKeywords, rule.keywords)) {
                matchedKeywords.addAll(categoryMatchedKeywords);
                categoryMatches.add(new CategoryMatch(rule, categoryMatchedKeywords.size()));
            }
        }

        CategoryMatch selectedMatch = selectCategory(categoryMatches);
        CategoryRule selectedRule = selectedMatch == null ? defaultRule() : selectedMatch.rule;
        boolean crossCategory = categoryMatches.size() > 1;

        AiWorkOrderAnalyzeResult result = base(selectedRule, matchedKeywords);
        result.setExtractedLocation(extractLocation(sourceText));
        result.setSummary(buildSummary(selectedRule, result.getExtractedLocation()));
        result.setSuggestedAction(buildSuggestedAction(selectedRule, highRisk, uncertain, crossCategory));
        result.setSafetyTips(buildSafetyTips(selectedRule, highRisk, crossCategory));
        result.setConfidence(resolveConfidence(selectedMatch, highRisk, uncertain, crossCategory));
        result.setManualReviewNeeded(selectedMatch == null || highRisk || uncertain || crossCategory);

        if (highRisk) {
            applyPriority(result, 4);
        }

        return result;
    }

    private AiWorkOrderAnalyzeResult base(CategoryRule rule, Set<String> matchedKeywords) {
        AiWorkOrderAnalyzeResult result = new AiWorkOrderAnalyzeResult();
        result.setCategory(rule.category);
        applyPriority(result, rule.basePriority);
        result.setRecommendedTeam(rule.recommendedTeam);
        result.setMatchedKeywords(new ArrayList<>(matchedKeywords));
        result.setProvider(PROVIDER);
        result.setProviderVersion(PROVIDER_VERSION);
        return result;
    }

    private void applyPriority(AiWorkOrderAnalyzeResult result, int priority) {
        result.setPriority(priority);
        result.setPriorityDesc(priorityDesc(priority));
        result.setUrgencyLevel(urgencyLevel(priority));
        result.setRiskLevel(riskLevel(priority));
        result.setSuggestedResponseMinutes(suggestedResponseMinutes(priority));
    }

    private CategoryMatch selectCategory(List<CategoryMatch> matches) {
        CategoryMatch selected = null;
        for (CategoryMatch match : matches) {
            if (selected == null || match.rule.rank > selected.rule.rank) {
                selected = match;
            }
        }
        return selected;
    }

    private int resolveConfidence(CategoryMatch selectedMatch,
                                  boolean highRisk,
                                  boolean uncertain,
                                  boolean crossCategory) {
        int confidence;
        if (selectedMatch == null) {
            confidence = 50;
        } else {
            confidence = selectedMatch.matchedKeywordCount > 1 ? 88 : 72;
        }
        if (crossCategory) {
            confidence = Math.min(confidence, 85);
        }
        if (uncertain) {
            confidence = Math.min(confidence, 65);
        }
        if (highRisk) {
            confidence = Math.max(confidence, 92);
        }
        return confidence;
    }

    private String buildSummary(CategoryRule rule, String extractedLocation) {
        if (StringUtils.hasText(extractedLocation)) {
            return rule.summary + "\u63d0\u53d6\u4f4d\u7f6e\uff1a" + extractedLocation + "\u3002";
        }
        return rule.summary;
    }

    private String buildSuggestedAction(CategoryRule rule,
                                        boolean highRisk,
                                        boolean uncertain,
                                        boolean crossCategory) {
        List<String> parts = new ArrayList<>();
        if (highRisk) {
            parts.add("\u9ad8\u5371\u7279\u6025\uff1a\u8bf7\u540e\u53f0\u4eba\u5458\u7acb\u5373\u7535\u8bdd\u786e\u8ba4\u73b0\u573a\u5b89\u5168\uff0c\u4f18\u5148\u5b89\u6392\u5e94\u6025\u5904\u7406\u3002");
        }
        if (uncertain) {
            parts.add("\u63cf\u8ff0\u5b58\u5728\u4e0d\u786e\u5b9a\u4fe1\u606f\uff0c\u6d3e\u5de5\u524d\u9700\u5148\u4e0e\u4f4f\u6237\u6838\u5b9e\u5173\u952e\u7ec6\u8282\u3002");
        }
        parts.add(rule.suggestedAction);
        if (crossCategory) {
            parts.add("\u7591\u4f3c\u6d89\u53ca\u591a\u4e2a\u4e13\u4e1a\u7ef4\u4fee\u65b9\u5411\uff0c\u5efa\u8bae\u540e\u53f0\u4eba\u5de5\u590d\u6838\uff0c\u5fc5\u8981\u65f6\u5b89\u6392\u73ed\u7ec4\u534f\u540c\u5904\u7406\u3002");
        }
        return String.join("", parts);
    }

    private List<String> buildSafetyTips(CategoryRule rule, boolean highRisk, boolean crossCategory) {
        List<String> tips = new ArrayList<>();
        if (highRisk) {
            tips.add("\u4f18\u5148\u786e\u8ba4\u73b0\u573a\u662f\u5426\u5b58\u5728\u4eba\u8eab\u5b89\u5168\u98ce\u9669\uff0c\u5fc5\u8981\u65f6\u5148\u9694\u79bb\u73b0\u573a\u3002");
        }
        tips.add(rule.safetyTipOne);
        tips.add(rule.safetyTipTwo);
        if (crossCategory) {
            tips.add("\u6d89\u53ca\u591a\u4e13\u4e1a\u98ce\u9669\u65f6\uff0c\u5148\u4eba\u5de5\u590d\u6838\u518d\u6d3e\u5de5\u3002");
        }
        return tips;
    }

    private String extractLocation(String sourceText) {
        if (!StringUtils.hasText(sourceText)) {
            return null;
        }
        for (Pattern pattern : LOCATION_PATTERNS) {
            Matcher matcher = pattern.matcher(sourceText);
            if (matcher.find()) {
                String location = cleanLocation(matcher.group(1));
                if (StringUtils.hasText(location)) {
                    return location;
                }
            }
        }
        return null;
    }

    private String cleanLocation(String location) {
        String cleaned = location == null ? null : location.trim();
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        for (String stopWord : LOCATION_STOP_WORDS) {
            int index = cleaned.indexOf(stopWord);
            if (index > 0) {
                cleaned = cleaned.substring(0, index);
            }
        }
        return cleaned.trim();
    }

    private CategoryRule defaultRule() {
        return new CategoryRule(
                "OTHER",
                "\u7efc\u5408\u7ef4\u4fee\u7ec4",
                0,
                1,
                "\u4f4f\u6237\u53cd\u9988\u7684\u95ee\u9898\u6682\u65f6\u65e0\u6cd5\u6839\u636e\u89c4\u5219\u7a33\u5b9a\u5206\u7c7b\u3002",
                "\u5efa\u8bae\u5148\u8054\u7cfb\u4f4f\u6237\u786e\u8ba4\u95ee\u9898\u7c7b\u578b\u3001\u5177\u4f53\u4f4d\u7f6e\u548c\u5f71\u54cd\u8303\u56f4\uff0c\u518d\u7531\u540e\u53f0\u4eba\u5458\u4eba\u5de5\u6d3e\u5de5\u3002",
                "\u4e0a\u95e8\u524d\u7535\u8bdd\u786e\u8ba4\u62a5\u4fee\u4f4d\u7f6e\u548c\u53ef\u8054\u7cfb\u65f6\u95f4\u3002",
                "\u4fe1\u606f\u4e0d\u8db3\u65f6\u4e0d\u5efa\u8bae\u76f4\u63a5\u81ea\u52a8\u6d3e\u5de5\u3002"
        );
    }

    private String buildText(Repair repair) {
        if (repair == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        append(sb, repair.getFaultType());
        append(sb, repair.getFaultDesc());
        return sb.toString();
    }

    private void append(StringBuilder sb, String value) {
        if (StringUtils.hasText(value)) {
            sb.append(value).append(' ');
        }
    }

    private boolean matchAny(String text, Set<String> matched, String... keywords) {
        boolean found = false;
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                found = true;
                matched.add(keyword);
            }
        }
        return found;
    }

    private String priorityDesc(int priority) {
        switch (priority) {
            case 4:
                return "\u9ad8\u5371\u7279\u6025";
            case 3:
                return "\u7d27\u6025";
            case 2:
                return "\u8f83\u6025";
            default:
                return "\u666e\u901a";
        }
    }

    private String urgencyLevel(int priority) {
        switch (priority) {
            case 4:
                return "CRITICAL";
            case 3:
                return "HIGH";
            case 2:
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    private String riskLevel(int priority) {
        if (priority >= 4) {
            return "HIGH";
        }
        if (priority >= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Integer suggestedResponseMinutes(int priority) {
        switch (priority) {
            case 4:
                return 15;
            case 3:
                return 30;
            case 2:
                return 60;
            default:
                return 120;
        }
    }

    private static class CategoryRule {
        private final String category;
        private final String recommendedTeam;
        private final int rank;
        private final int basePriority;
        private final String summary;
        private final String suggestedAction;
        private final String safetyTipOne;
        private final String safetyTipTwo;
        private final String[] keywords;

        private CategoryRule(String category,
                             String recommendedTeam,
                             int rank,
                             int basePriority,
                             String summary,
                             String suggestedAction,
                             String safetyTipOne,
                             String safetyTipTwo,
                             String... keywords) {
            this.category = category;
            this.recommendedTeam = recommendedTeam;
            this.rank = rank;
            this.basePriority = basePriority;
            this.summary = summary;
            this.suggestedAction = suggestedAction;
            this.safetyTipOne = safetyTipOne;
            this.safetyTipTwo = safetyTipTwo;
            this.keywords = keywords;
        }
    }

    private static class CategoryMatch {
        private final CategoryRule rule;
        private final int matchedKeywordCount;

        private CategoryMatch(CategoryRule rule, int matchedKeywordCount) {
            this.rule = rule;
            this.matchedKeywordCount = matchedKeywordCount;
        }
    }
}
