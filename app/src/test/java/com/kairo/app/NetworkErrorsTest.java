package com.kairo.app;

import com.kairo.app.core.NetworkErrors;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NetworkErrorsTest {
    @Test
    public void mapsHttpAndTransportFailuresToFriendlyCopy() {
        assertTrue(NetworkErrors.friendly(new SocketTimeoutException()).contains("too long"));
        assertTrue(NetworkErrors.friendly(new IOException("boom")).contains("Could not reach"));
        assertTrue(NetworkErrors.friendly(new RuntimeException("HTTP 401 unauthorized")).contains("401"));
        assertTrue(NetworkErrors.friendly(new RuntimeException("http 429 rate limit")).contains("429"));
        assertTrue(NetworkErrors.friendly(new RuntimeException("HTTP 403 forbidden")).contains("403"));
        assertTrue(NetworkErrors.friendly(new RuntimeException("HTTP 404")).contains("404"));
    }
}
