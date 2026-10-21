package com.backend.event.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public final class EventUrlNormalizer {

    private static final Set<String> TRACKING_PARAMS = Set.of("fbclid", "gclid", "igshid", "mc_cid", "mc_eid");

    private EventUrlNormalizer() {
    }

    public static String normalize(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            return trimmed;
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            return trimmed;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(scheme).append("://").append(uri.getHost().toLowerCase(Locale.ROOT));

        int port = uri.getPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        if (port != -1 && !defaultPort) {
            normalized.append(':').append(port);
        }

        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!"/".equals(path)) {
            normalized.append(path);
        }

        String query = removeTrackingParams(uri.getRawQuery());
        if (!query.isEmpty()) {
            normalized.append('?').append(query);
        }
        return normalized.toString();
    }

    private static String removeTrackingParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(param -> !param.isEmpty())
                .filter(param -> !isTracking(param))
                .collect(Collectors.joining("&"));
    }

    private static boolean isTracking(String param) {
        int equals = param.indexOf('=');
        String key = (equals < 0 ? param : param.substring(0, equals)).toLowerCase(Locale.ROOT);
        return key.startsWith("utm_") || TRACKING_PARAMS.contains(key);
    }
}
