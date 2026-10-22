package com.backend.crawler.common;

import java.io.IOException;

/** 200이 아닌 응답을 받았을 때 던진다. */
public class CrawlHttpException extends IOException {
    private final int status;

    public CrawlHttpException(int status, String url) {
        super("HTTP " + status + " url=" + url);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
