/*
 * Copyright (c) 2014 ConteXtream Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

public class RegularExpressionMatcher extends TypeSafeMatcher<String> {

    private final Pattern pattern;

    public RegularExpressionMatcher(String pattern) {
        this(Pattern.compile(pattern));
    }

    public RegularExpressionMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches regular expression ").appendValue(pattern);
    }

    @Override
    public boolean matchesSafely(String item) {
        return pattern.matcher(item).matches();
    }

    @Factory
    public static RegularExpressionMatcher matchesPattern(Pattern pattern) {
        return new RegularExpressionMatcher(pattern);
    }

    @Factory
    public static RegularExpressionMatcher matchesPattern(String pattern) {
        return new RegularExpressionMatcher(pattern);
    }

}
