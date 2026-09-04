package com.kairo.app.data;

/** A provider-neutral web result that can be opened or inserted into a prompt as context. */
public final class SearchResult {
    private final String title;
    private final String url;
    private final String snippet;
    private final String source;

    public SearchResult(String title, String url, String snippet, String source) {
        this.title = title == null ? "Untitled result" : title;
        this.url = url == null ? "" : url;
        this.snippet = snippet == null ? "" : snippet;
        this.source = source == null ? "Web" : source;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getSnippet() { return snippet; }
    public String getSource() { return source; }
}
