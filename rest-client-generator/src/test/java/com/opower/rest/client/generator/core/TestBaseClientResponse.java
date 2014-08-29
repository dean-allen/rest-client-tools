package com.opower.rest.client.generator.core;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author chris.phillips
 */
public class TestBaseClientResponse {

    /**
     * When resetting the stream if an exception is thrown, it should be caught and resetStream should return false.
     */
    @Test
    public void exceptionOnResetStreamReturnsFalse() {

        BaseClientResponse response = new BaseClientResponse(new BaseClientResponse.BaseClientResponseStreamFactory() {
            @Override
            public InputStream getInputStream() throws IOException {
                // InputStream by default throws an exception when reset is called
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return 0;
                    }
                };
            }

            @Override
            public void performReleaseConnection() {

            }
        });

        assertThat(response.resetStream(), is(false));
    }

    /**
     * If the underlying stream is successfully reset, then resetStream should return true.
     */
    @Test
    public void successfulStreamResetReturnsTrue() {
        BaseClientResponse response = new BaseClientResponse(new BaseClientResponse.BaseClientResponseStreamFactory() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return 0;
                    }

                    @Override
                    public synchronized void reset() throws IOException {
                        // making sure this doesn't throw
                    }
                };
            }

            @Override
            public void performReleaseConnection() {

            }
        });

        assertThat(response.resetStream(), is(true));
    }
}
