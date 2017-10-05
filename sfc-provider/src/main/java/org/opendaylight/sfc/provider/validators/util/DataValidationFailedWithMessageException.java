/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators.util;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@SuppressWarnings("serial")
/**
 * Specific data validation exception for SFP validation. Not working yet
 * (pending custom error message support on other projects)
 */
public class DataValidationFailedWithMessageException extends DataValidationFailedException {

    private final List<RpcError> myErrorList = new ArrayList<>();

    public <P extends Path<P>> DataValidationFailedWithMessageException(final Class<P> pathType, final P path,
            final String message, final String appTag) {
        super(pathType, path, message);
        myErrorList.add(RpcResultBuilder.newError(ErrorType.APPLICATION, "invalid-value", message, appTag,
                                                  path.toString(), null));
    }

    @Override
    public List<RpcError> getErrorList() {
        return myErrorList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("message", getMessage()).add("errorList", this.myErrorList)
                .toString();
    }
}
