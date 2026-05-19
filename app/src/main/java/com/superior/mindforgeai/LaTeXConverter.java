package com.superior.mindforgeai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaTeXConverter {

    private static final Map<String, String> CMDS = new LinkedHashMap<>();

    static {
        CMDS.put("\\alpha","α");   CMDS.put("\\beta","β");   CMDS.put("\\gamma","γ");
        CMDS.put("\\delta","δ");   CMDS.put("\\epsilon","ε"); CMDS.put("\\zeta","ζ");
        CMDS.put("\\eta","η");     CMDS.put("\\theta","θ");   CMDS.put("\\iota","ι");
        CMDS.put("\\kappa","κ");   CMDS.put("\\lambda","λ");  CMDS.put("\\mu","μ");
        CMDS.put("\\nu","ν");      CMDS.put("\\xi","ξ");      CMDS.put("\\pi","π");
        CMDS.put("\\rho","ρ");     CMDS.put("\\sigma","σ");   CMDS.put("\\tau","τ");
        CMDS.put("\\upsilon","υ"); CMDS.put("\\phi","φ");     CMDS.put("\\chi","χ");
        CMDS.put("\\psi","ψ");     CMDS.put("\\omega","ω");
        CMDS.put("\\Gamma","Γ");   CMDS.put("\\Delta","Δ");   CMDS.put("\\Theta","Θ");
        CMDS.put("\\Lambda","Λ");  CMDS.put("\\Xi","Ξ");      CMDS.put("\\Pi","Π");
        CMDS.put("\\Sigma","Σ");   CMDS.put("\\Phi","Φ");     CMDS.put("\\Psi","Ψ");
        CMDS.put("\\Omega","Ω");

        CMDS.put("\\times","×");  CMDS.put("\\cdot","·");   CMDS.put("\\div","÷");
        CMDS.put("\\pm","±");     CMDS.put("\\mp","∓");     CMDS.put("\\ast","∗");
        CMDS.put("\\star","⋆");   CMDS.put("\\circ","∘");   CMDS.put("\\bullet","•");
        CMDS.put("\\oplus","⊕");  CMDS.put("\\otimes","⊗"); CMDS.put("\\odot","⊙");

        CMDS.put("\\leq","≤");    CMDS.put("\\geq","≥");    CMDS.put("\\neq","≠");
        CMDS.put("\\approx","≈"); CMDS.put("\\equiv","≡");  CMDS.put("\\propto","∝");
        CMDS.put("\\sim","∼");    CMDS.put("\\simeq","≃");  CMDS.put("\\gg","≫");
        CMDS.put("\\ll","≪");

        CMDS.put("\\rightarrow","→");  CMDS.put("\\to","→");   CMDS.put("\\leftarrow","←");
        CMDS.put("\\uparrow","↑");      CMDS.put("\\downarrow","↓");
        CMDS.put("\\leftrightarrow","↔"); CMDS.put("\\Rightarrow","⇒");
        CMDS.put("\\Leftarrow","⇐");    CMDS.put("\\Leftrightarrow","⇔");

        CMDS.put("\\forall","∀");  CMDS.put("\\exists","∃");  CMDS.put("\\in","∈");
        CMDS.put("\\notin","∉");   CMDS.put("\\ni","∋");      CMDS.put("\\subset","⊂");
        CMDS.put("\\supset","⊃");  CMDS.put("\\subseteq","⊆"); CMDS.put("\\supseteq","⊇");
        CMDS.put("\\cup","∪");    CMDS.put("\\cap","∩");     CMDS.put("\\emptyset","∅");
        CMDS.put("\\mathbb{R}","ℝ"); CMDS.put("\\mathbb{N}","ℕ"); CMDS.put("\\mathbb{Z}","ℤ");
        CMDS.put("\\mathbb{C}","ℂ"); CMDS.put("\\mathbb{Q}","ℚ");

        CMDS.put("\\infty","∞");   CMDS.put("\\partial","∂");  CMDS.put("\\nabla","∇");
        CMDS.put("\\angle","∠");   CMDS.put("\\perp","⊥");       CMDS.put("\\parallel","∥");
        CMDS.put("\\ldots","…");   CMDS.put("\\cdots","⋯");      CMDS.put("\\vdots","⋮");
        CMDS.put("\\ddots","⋱");   CMDS.put("\\therefore","∴");
        CMDS.put("\\because","∵"); CMDS.put("\\degree","°");

        CMDS.put("\\sum","Σ");     CMDS.put("\\prod","Π");     CMDS.put("\\int","∫");
        CMDS.put("\\iint","∬");    CMDS.put("\\oint","∮");     CMDS.put("\\sqrt","√");

        CMDS.put("\\lim","lim");   CMDS.put("\\log","log");    CMDS.put("\\ln","ln");
        CMDS.put("\\sin","sin");   CMDS.put("\\cos","cos");    CMDS.put("\\tan","tan");
        CMDS.put("\\csc","csc");   CMDS.put("\\sec","sec");    CMDS.put("\\cot","cot");
        CMDS.put("\\max","max");   CMDS.put("\\min","min");    CMDS.put("\\det","det");
        CMDS.put("\\gcd","gcd");   CMDS.put("\\mod","mod");
        CMDS.put("\\exp","exp");   CMDS.put("\\dim","dim");    CMDS.put("\\hom","hom");
        CMDS.put("\\ker","ker");   CMDS.put("\\Pr","Pr");      CMDS.put("\\sup","sup");
        CMDS.put("\\inf","inf");

        CMDS.put("\\left(","(");   CMDS.put("\\right)",")");    CMDS.put("\\left[","[");
        CMDS.put("\\right]","]");  CMDS.put("\\left.{","{");    CMDS.put("\\right.}","}");
        CMDS.put("\\left|","|");   CMDS.put("\\right|","|");
        CMDS.put("\\langle","⟨");  CMDS.put("\\rangle","⟩");
        CMDS.put("\\lfloor","⌊");  CMDS.put("\\rfloor","⌋");
        CMDS.put("\\lceil","⌈");   CMDS.put("\\rceil","⌉");
        CMDS.put("\\{","{");       CMDS.put("\\}","}");
        CMDS.put("\\%","%");       CMDS.put("\\#","#");
        CMDS.put("\\_","_");       CMDS.put("\\&","&");
        CMDS.put("\\\\","\n");     CMDS.put("\\newline","\n");

        CMDS.put("\\text{","");    CMDS.put("\\displaystyle","");
        CMDS.put("\\tfrac","\\frac"); CMDS.put("\\dfrac","\\frac");
        CMDS.put("\\quad"," ");    CMDS.put("\\qquad","  ");
        CMDS.put("\\space"," ");   CMDS.put("\\not","¬");
    }

    // Patterns for simple (no-nested-brace) sub/superscript — applied after brace-aware frac/sqrt
    private static final Pattern SUB = Pattern.compile("_\\{([^{}]*)\\}");
    private static final Pattern SUP = Pattern.compile("\\^\\{([^{}]*)\\}");

    /**
     * Safe conversion of LaTeX to Unicode plain-text.
     * Uses bracket-aware extraction for \frac and \sqrt so nested braces
     * (e.g. \sqrt{x^{2}+y^{2}}) do not cause infinite loops.
     */
    public static String convert(String latex) {
        if (latex == null || latex.isEmpty()) return "";

        String s = latex;
        // Normalize double-backslashes (\\frac, \\ln, etc.) to single-backslash (\frac, \ln)
        // The AI sometimes double-escapes LaTeX commands in JSON output.
        // In Java: "\\\\" is the two-char string \\, and "\\" is the one-char string \
        if (s.contains("\\\\")) {
            s = s.replace("\\\\", "\\");
        }
        s = s.replace("\\n", "\n");

        // Replace \sqrt[n]{...} optional-index form first (no nested brace issue)
        s = s.replaceAll("\\\\sqrt\\[([^\\]]+)\\]\\{([^{}]*)\\}", "$2^(1/$1)");

        // Replace \frac{num}{den} — bracket-aware, max 30 iterations to avoid infinite loop
        for (int guard = 0; guard < 30 && s.contains("\\frac{"); guard++) {
            int idx = s.indexOf("\\frac{");
            if (idx < 0) break;
            int[] numRange = extractBraceContent(s, idx + 5); // skip "\frac"
            if (numRange == null) break; // malformed — bail out safely
            String num = s.substring(numRange[0], numRange[1]);
            int afterNum = numRange[2];
            if (afterNum >= s.length() || s.charAt(afterNum) != '{') break;
            int[] denRange = extractBraceContent(s, afterNum);
            if (denRange == null) break;
            String den = s.substring(denRange[0], denRange[1]);
            s = s.substring(0, idx) + "(" + num + ")/(" + den + ")" + s.substring(denRange[2]);
        }

        // Replace \sqrt{...} — bracket-aware, max 30 iterations
        for (int guard = 0; guard < 30 && s.contains("\\sqrt{"); guard++) {
            int idx = s.indexOf("\\sqrt{");
            if (idx < 0) break;
            int[] range = extractBraceContent(s, idx + 5); // skip "\sqrt"
            if (range == null) break;
            String inner = s.substring(range[0], range[1]);
            s = s.substring(0, idx) + "√(" + inner + ")" + s.substring(range[2]);
        }

        // Superscripts ^{...}
        s = replaceAll(s, SUP, m -> toSuperScript(m.group(1)));

        // Subscripts _{...}
        s = replaceAll(s, SUB, m -> toSubScript(m.group(1)));

        // \text{...} — strip command, keep content
        s = s.replaceAll("\\\\text\\{([^{}]*)\\}", "$1");

        // Handle \left. and \right. — remove them
        s = s.replaceAll("\\\\left\\.", "").replaceAll("\\\\right\\.", "");

        // Replace all known symbol commands
        for (Map.Entry<String, String> e : CMDS.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }

        s = s.replace("^", "");
        s = s.replace("_{}", "");
        s = s.replace("^{}", "");
        s = s.replaceAll("[{}]", "");
        s = s.trim();

        return s;
    }

    /**
     * Finds the content inside the first '{...}' starting at or after position start.
     * Handles nested braces correctly.
     *
     * @return int[3] = {contentStart, contentEnd, posAfterClosingBrace},
     *         or null if no valid '{...}' found.
     */
    private static int[] extractBraceContent(String s, int start) {
        int open = s.indexOf('{', start);
        if (open < 0) return null;
        int depth = 1;
        int i = open + 1;
        while (i < s.length() && depth > 0) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }
        if (depth != 0) return null; // unmatched brace
        // content is from open+1 to i-1; i is position after closing '}'
        return new int[]{open + 1, i - 1, i};
    }

    private static String toSuperScript(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0': sb.append('⁰'); break; case '1': sb.append('¹'); break;
                case '2': sb.append('²'); break; case '3': sb.append('³'); break;
                case '4': sb.append('⁴'); break; case '5': sb.append('⁵'); break;
                case '6': sb.append('⁶'); break; case '7': sb.append('⁷'); break;
                case '8': sb.append('⁸'); break; case '9': sb.append('⁹'); break;
                case '+': sb.append('⁺'); break; case '-': sb.append('⁻'); break;
                case '=': sb.append('⁼'); break; case '(': sb.append('⁽'); break;
                case ')': sb.append('⁾'); break; case 'a': sb.append('ᵃ'); break;
                case 'b': sb.append('ᵇ'); break; case 'c': sb.append('ᶜ'); break;
                case 'd': sb.append('ᵈ'); break; case 'e': sb.append('ᵉ'); break;
                case 'f': sb.append('ᶠ'); break; case 'g': sb.append('ᵍ'); break;
                case 'h': sb.append('ʰ'); break; case 'i': sb.append('ⁱ'); break;
                case 'j': sb.append('ʲ'); break; case 'k': sb.append('ᵏ'); break;
                case 'l': sb.append('ˡ'); break; case 'm': sb.append('ᵐ'); break;
                case 'n': sb.append('ⁿ'); break; case 'o': sb.append('ᵒ'); break;
                case 'p': sb.append('ᵖ'); break; case 'r': sb.append('ʳ'); break;
                case 's': sb.append('ˢ'); break; case 't': sb.append('ᵗ'); break;
                case 'u': sb.append('ᵘ'); break; case 'v': sb.append('ᵛ'); break;
                case 'w': sb.append('ʷ'); break; case 'x': sb.append('ˣ'); break;
                case 'y': sb.append('ʸ'); break; case 'z': sb.append('ᶻ'); break;
                case 'A': sb.append('ᴬ'); break; case 'B': sb.append('ᴮ'); break;
                case 'D': sb.append('ᴰ'); break; case 'E': sb.append('ᴱ'); break;
                case 'G': sb.append('ᴳ'); break; case 'H': sb.append('ᴴ'); break;
                case 'I': sb.append('ᴵ'); break; case 'J': sb.append('ᴶ'); break;
                case 'K': sb.append('ᴷ'); break; case 'L': sb.append('ᴸ'); break;
                case 'M': sb.append('ᴹ'); break; case 'N': sb.append('ᴺ'); break;
                case 'O': sb.append('ᴼ'); break; case 'P': sb.append('ᴾ'); break;
                case 'R': sb.append('ᴿ'); break; case 'T': sb.append('ᵀ'); break;
                case 'U': sb.append('ᵁ'); break; case 'V': sb.append('ⱽ'); break;
                case 'W': sb.append('ᵂ'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static String toSubScript(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0': sb.append('₀'); break; case '1': sb.append('₁'); break;
                case '2': sb.append('₂'); break; case '3': sb.append('₃'); break;
                case '4': sb.append('₄'); break; case '5': sb.append('₅'); break;
                case '6': sb.append('₆'); break; case '7': sb.append('₇'); break;
                case '8': sb.append('₈'); break; case '9': sb.append('₉'); break;
                case '+': sb.append('₊'); break; case '-': sb.append('₋'); break;
                case '=': sb.append('₌'); break; case '(': sb.append('₍'); break;
                case ')': sb.append('₎'); break; case 'a': sb.append('ₐ'); break;
                case 'e': sb.append('ₑ'); break; case 'h': sb.append('ₕ'); break;
                case 'i': sb.append('ᵢ'); break; case 'j': sb.append('ⱼ'); break;
                case 'k': sb.append('ₖ'); break; case 'l': sb.append('ₗ'); break;
                case 'm': sb.append('ₘ'); break; case 'n': sb.append('ₙ'); break;
                case 'o': sb.append('ₒ'); break; case 'p': sb.append('ₚ'); break;
                case 'r': sb.append('ᵣ'); break; case 's': sb.append('ₛ'); break;
                case 't': sb.append('ₜ'); break; case 'u': sb.append('ᵤ'); break;
                case 'v': sb.append('ᵥ'); break; case 'x': sb.append('ₓ'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface MatchFunc {
        String apply(Matcher m);
    }

    private static String replaceAll(String input, Pattern p, MatchFunc fn) {
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(m)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String replaceFirst(String input, Pattern p, MatchFunc fn) {
        Matcher m = p.matcher(input);
        if (m.find()) {
            return input.substring(0, m.start()) + fn.apply(m) + input.substring(m.end());
        }
        return input;
    }
}
