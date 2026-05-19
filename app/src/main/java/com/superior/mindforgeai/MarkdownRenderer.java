package com.superior.mindforgeai;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;

public class MarkdownRenderer {

    /**
     * Render markdown-flavored text to Spanned for display in TextViews.
     *
     * If the text contains no markdown syntax tokens, it is returned as plain
     * Spanned (no transformation) to avoid accidentally styling user-typed content.
     */
    public static Spanned render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return new SpannableString("");
        }

        // If no markdown tokens are present, skip all processing to avoid
        // transforming plain user-typed text into bold/colored spans accidentally.
        boolean hasMarkdown = markdown.contains("**") || markdown.contains("*")
                || markdown.contains("##") || markdown.contains("# ")
                || markdown.contains("~~") || markdown.contains("`")
                || markdown.contains("__");

        if (!hasMarkdown) {
            // Plain text — just convert newlines to <br> for line breaks, no other transforms
            String plain = markdown.replace("\n", "<br/>");
            return Html.fromHtml(plain, Html.FROM_HTML_MODE_LEGACY);
        }

        String html = markdown;

        // Headings (process most specific first)
        html = html.replaceAll("(?m)^#### (.+)$", "<b>$1</b>");
        html = html.replaceAll("(?m)^### (.+)$", "<b>$1</b>");
        html = html.replaceAll("(?m)^## (.+)$", "<b><big>$1</big></b>");
        html = html.replaceAll("(?m)^# (.+)$", "<b><big>$1</big></b>");

        // Bold and italic
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        html = html.replaceAll("__(.+?)__", "<b>$1</b>");

        // Strikethrough and code
        html = html.replaceAll("~~(.+?)~~", "<strike>$1</strike>");
        html = html.replaceAll("`([^`]+)`", "<tt>$1</tt>");

        // Bullet points
        html = html.replaceAll("(?m)^[\\-\\*] (.+)$", "&#8226; $1");

        // Newlines
        html = html.replace("\n", "<br/>");

        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }
}
