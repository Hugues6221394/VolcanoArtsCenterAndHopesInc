package com.volcanoartscenter.platform.shared.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

/**
 * Strips dangerous markup from staff-supplied rich content (blog posts, talent
 * bios) before persisting it. Built on the OWASP Java HTML Sanitizer with a
 * permissive blog-friendly policy: text formatting, headings, links, images,
 * lists, code blocks, blockquotes — but no inline JavaScript, no
 * {@code <script>}/{@code <iframe>}/{@code <style>}, no {@code on*} attrs.
 */
@Service
public class HtmlSanitizerService {

    private final PolicyFactory blogPolicy;

    public HtmlSanitizerService() {
        this.blogPolicy = Sanitizers.FORMATTING
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.LINKS)
                .and(Sanitizers.IMAGES)
                .and(Sanitizers.STYLES)
                .and(new HtmlPolicyBuilder()
                        .allowElements("h1", "h2", "h3", "h4", "h5", "h6",
                                "p", "ul", "ol", "li", "blockquote",
                                "code", "pre", "hr", "br")
                        .allowAttributes("class").onElements("code", "pre")
                        .allowAttributes("href").onElements("a")
                        .requireRelNofollowOnLinks()
                        .toFactory());
    }

    /** Returns sanitized HTML — never null; empty input yields {@code ""}. */
    public String sanitizeBlog(String html) {
        if (html == null || html.isBlank()) return "";
        return blogPolicy.sanitize(html);
    }
}
