/*
 * Copyright (c) 2017 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.validators.util;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.mdsal.common.api.PostCanCommitStep;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public final class ValidationConstants {

    private static final String SFC_VALIDATION_ERROR = "SFC validation error";

    /**
     * Yang instance identifiers.
     */
    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier SF_PATH_YII =
            YangInstanceIdentifier
            .builder().node(ServiceFunctions.QNAME).node(ServiceFunction.QNAME).build();

    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier SFP_PATH_YII =
            YangInstanceIdentifier
            .builder().node(ServiceFunctionPaths.QNAME).node(ServiceFunctionPath.QNAME).build();

    /**
     * DOM data tree identifiers.
     */
    public static final DOMDataTreeIdentifier SF_ID = new DOMDataTreeIdentifier(
            org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, SF_PATH_YII);

    public static final DOMDataTreeIdentifier SFP_ID = new DOMDataTreeIdentifier(
            org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, SFP_PATH_YII);

    /*
     * Exceptions
     */
    public static final DataValidationFailedWithMessageException SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_MISSING =
            new DataValidationFailedWithMessageException(YangInstanceIdentifier.class,
                    SFP_PATH_YII, "The SF referenced in the SF Path does not exist", SFC_VALIDATION_ERROR);

    public static final DataValidationFailedWithMessageException SFP_FAILED_CAN_COMMIT_EXCEPTION_SFC_MISSING =
            new DataValidationFailedWithMessageException(YangInstanceIdentifier.class, SFP_PATH_YII,
                    "The SF chain referenced in the SF Path does not exist", SFC_VALIDATION_ERROR);

    public static final DataValidationFailedWithMessageException SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_CONSISTENCY =
            new DataValidationFailedWithMessageException(YangInstanceIdentifier.class, SFP_PATH_YII,
                    "SF/ SF types are not consistent with those defined in the SFC", SFC_VALIDATION_ERROR);
    /*
     * Futures
     */
    public static final FluentFuture<PostCanCommitStep> FAILED_CAN_COMMIT_SFP_FUTURE =
            FluentFutures.immediateFailedFluentFuture(SFP_FAILED_CAN_COMMIT_EXCEPTION_SF_CONSISTENCY);

    private ValidationConstants() {
    }
}
