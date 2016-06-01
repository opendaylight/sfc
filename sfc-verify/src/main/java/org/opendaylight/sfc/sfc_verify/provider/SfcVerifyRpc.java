/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_verify.provider;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.ServiceFunctionChainVerificationService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.EnableSfcVerifyRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.DisableSfcVerifyRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.EnableSfcVerifyRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.EnableSfcVerifyRenderedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.DisableSfcVerifyRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfcv.rev150717.DisableSfcVerifyRenderedPathOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * This class holds all RPCs methods for SFC Verify Provider.
 * <p>
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-06-01
 */

public class SfcVerifyRpc implements ServiceFunctionChainVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVerifyRpc.class);
    //private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    //private DataBroker dataBroker = odlSfc.getDataProvider();

    public static SfcVerifyRpc getSfcVerifyRpc() {
        return new SfcVerifyRpc();
    }

    @Override
    public Future<RpcResult<EnableSfcVerifyRenderedPathOutput>> enableSfcVerifyRenderedPath(
            EnableSfcVerifyRenderedPathInput input) {
        boolean ret;
        RpcResultBuilder<EnableSfcVerifyRenderedPathOutput> rpcResultBuilder;
        RspName rspName = new RspName(input.getSfcVerifyRspName());

        ret = SfcVerifyRspProcessor.enableSfcVerification(rspName,
                                                          input.getSfcVerifyNetconfNode(),
                                                          input.getSfcVerifyNumProfiles(),
                                                          input.getSfcVerifyProfilesValidator());
        if (ret) {
            EnableSfcVerifyRenderedPathOutputBuilder enableSfcVerifyRenderedPathOutputBuilder = new EnableSfcVerifyRenderedPathOutputBuilder();
            enableSfcVerifyRenderedPathOutputBuilder.setResult(ret);
            rpcResultBuilder =
                    RpcResultBuilder.success(enableSfcVerifyRenderedPathOutputBuilder.build());
        } else {
            String message = "Error enabling SFC verification for Rendered Service Path: " + input.getSfcVerifyRspName();
            rpcResultBuilder =
                    RpcResultBuilder.<EnableSfcVerifyRenderedPathOutput>failed().withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public Future<RpcResult<DisableSfcVerifyRenderedPathOutput>> disableSfcVerifyRenderedPath(
            DisableSfcVerifyRenderedPathInput input) {
        boolean ret;
        RpcResultBuilder<DisableSfcVerifyRenderedPathOutput> rpcResultBuilder;
        RspName rspName = new RspName(input.getSfcVerifyRspName());

        ret = SfcVerifyRspProcessor.disableSfcVerification(rspName);
        if (ret) {
            DisableSfcVerifyRenderedPathOutputBuilder disableSfcVerifyRenderedPathOutputBuilder = new DisableSfcVerifyRenderedPathOutputBuilder();
            disableSfcVerifyRenderedPathOutputBuilder.setResult(ret);
            rpcResultBuilder =
                    RpcResultBuilder.success(disableSfcVerifyRenderedPathOutputBuilder.build());
        } else {
            String message = "Error disabling SFC verification for Rendered Service Path: " + input.getSfcVerifyRspName();
            rpcResultBuilder =
                    RpcResultBuilder.<DisableSfcVerifyRenderedPathOutput>failed().withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
