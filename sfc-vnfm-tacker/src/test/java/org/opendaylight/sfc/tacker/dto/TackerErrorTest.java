/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.dto;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TackerErrorTest {

    private static final String ERROR_MESSAGE = "The resource could not be found.";
    private static final int ERROR_CODE = 404;
    private static final String ERROR_TITLE = "Not Found";
    private static TackerError error;

    @Before
    public void init() {
        error = new TackerError(
                Error.builder().setMessage(ERROR_MESSAGE).setCode(ERROR_CODE).setTitle(ERROR_TITLE).build());
    }

    @Test
    public void simpleConversionTest() {
        TackerError testError = new TackerError(" ");
        // init with empty json should result with null object
        Assert.assertNull(testError.getError());

        testError = new TackerError(
                Error.builder().setMessage(ERROR_MESSAGE).setCode(ERROR_CODE).setTitle(ERROR_TITLE).build());

        Assert.assertTrue(testError.getMessage().equals(ERROR_MESSAGE));
        Assert.assertTrue(testError.getCode() == ERROR_CODE);
        Assert.assertTrue(testError.getTitle().equals(ERROR_TITLE));

        String jsonError = testError.toJson();

        testError = new TackerError(jsonError);

        Assert.assertNotNull(testError);
        Assert.assertTrue(testError.getMessage().equals(error.getMessage()));
        Assert.assertTrue(testError.getCode() == error.getCode());
        Assert.assertTrue(testError.getTitle().equals(error.getTitle()));
    }
}
