package com.taxipot.global.error;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ErrorCode enum 이 명세(static/api-spec/taxipot-api.yaml) '공통 에러 코드'와 이름·HTTP 상태가 같은지 검사.
 * 누가 ErrorCode 를 고치거나 명세가 바뀌면 이 테스트가 깨진다 → 둘을 맞춘다.
 */
class ErrorCodeSpecTest {

    @Test
    void errorCodesMatchSpec() throws IOException {
        Map<String, Integer> spec = readSpecCodes();
        assertEquals(77, spec.size(), "명세 공통 에러 코드 개수");

        Map<String, Integer> enumCodes = Arrays.stream(ErrorCode.values())
                .filter(c -> !c.isServerOnly())
                .collect(Collectors.toMap(Enum::name, c -> c.getStatus().value()));

        Set<String> missing = new TreeSet<>(spec.keySet());
        missing.removeAll(enumCodes.keySet());
        Set<String> extra = new TreeSet<>(enumCodes.keySet());
        extra.removeAll(spec.keySet());
        assertTrue(missing.isEmpty(), "ErrorCode 에 없는 명세 코드: " + missing);
        assertTrue(extra.isEmpty(), "명세에 없는 ErrorCode (서버 전용이면 isServerOnly 에 추가): " + extra);
        spec.forEach((code, status) -> assertEquals(status, enumCodes.get(code), code + " HTTP 상태"));
    }

    private Map<String, Integer> readSpecCodes() throws IOException {
        String yaml;
        try (InputStream in = getClass().getResourceAsStream("/static/api-spec/taxipot-api.yaml")) {
            yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String section = yaml.substring(yaml.indexOf("## 공통 에러 코드"));
        section = section.substring(section.indexOf('\n'), section.indexOf("\n  ## ", 1) > 0 ? section.indexOf("\n  ## ", 1) : section.length());
        Map<String, Integer> codes = new LinkedHashMap<>();
        Pattern statusLine = Pattern.compile("^\\s*- (\\d{3}): (.*)$");
        Pattern codeWord = Pattern.compile("\\b[A-Z][A-Z_]{2,}\\b");
        Integer current = null;
        for (String line : section.split("\n")) {
            Matcher m = statusLine.matcher(line);
            String rest;
            if (m.matches()) {
                current = Integer.valueOf(m.group(1));
                rest = m.group(2);
            } else if (current != null && line.startsWith("      ") && !line.trim().startsWith("-")) {
                rest = line;
            } else {
                if (current != null && !line.isBlank()) {
                    break;
                }
                continue;
            }
            Matcher w = codeWord.matcher(rest);
            while (w.find()) {
                codes.put(w.group(), current);
            }
        }
        return codes;
    }
}
