package com.kairo.app;

import com.kairo.app.core.TokenEstimator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TokenEstimatorTest {
    @Test
    public void estimatesFourCharsPerToken() {
        assertEquals(0, TokenEstimator.estimate(null));
        assertEquals(0, TokenEstimator.estimate(""));
        assertEquals(1, TokenEstimator.estimate("abcd"));
        assertEquals(5, TokenEstimator.estimate("abcdefghijabcdefghij"));
        assertTrue(TokenEstimator.label(1500).contains("1.5k"));
        assertTrue(TokenEstimator.label(0).contains("~0"));
    }
}
