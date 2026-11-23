package com.chirag.nhbresidex.util;

import java.io.IOException;
import java.util.Map;

public interface HttpTransport extends AutoCloseable {
    String get(String url, Map<String, String> headers) throws IOException;

    String postForm(String url, Map<String, String> headers, Map<String, String> form) throws IOException;

    @Override
    default void close() {
    }
}
