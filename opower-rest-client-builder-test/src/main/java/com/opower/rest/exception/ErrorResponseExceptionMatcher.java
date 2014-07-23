package com.opower.rest.exception;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows for easy checking of ErrorResponseException.getServiceErrorCode() in tests.
 *
 * @author spencer.firestone
 */
public class ErrorResponseExceptionMatcher extends TypeSafeMatcher<ErrorResponseException> {
    private Object foundErrorCode;
    private final Object expectedErrorCode;

    /**
     * Constructor.
     * @param expectedErrorCode the error code to check for.
     */
    public ErrorResponseExceptionMatcher(Object expectedErrorCode) {
        this.expectedErrorCode = expectedErrorCode;
    }

    /**
     * Pass this method to ExpectedException.expect() to test that the exception has the given error code.
     * @param expectedErrorCode the error code to check for.
     * @return the matcher.
     */
    public static ErrorResponseExceptionMatcher hasCode(Object expectedErrorCode) {
        checkNotNull(expectedErrorCode);
        return new ErrorResponseExceptionMatcher(expectedErrorCode);
    }

    @Override
    public boolean matchesSafely(ErrorResponseException foundException) {
        checkNotNull(foundException, "foundException cannot be null");
        this.foundErrorCode = foundException.getServiceErrorCode();
        return this.expectedErrorCode.equals(this.foundErrorCode);
    }

    @Override
    public void describeTo(Description description) {
        checkNotNull(description, "description cannot be null");
        description.appendText("Expected error code ").appendValue(this.expectedErrorCode)
                .appendText(" but found ").appendValue(this.foundErrorCode);
    }
}
