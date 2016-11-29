/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.pot.provider;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.DisableSfcIoamPotRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.DisableSfcIoamPotRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.DisableSfcIoamPotRenderedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.EnableSfcIoamPotRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.EnableSfcIoamPotRenderedPathOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.EnableSfcIoamPotRenderedPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.nb.pot.rev161122.SfcIoamNbPotService;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * This class holds all RPCs/methods for enabling/disabling SFC
 * iOAM Proof of Transit (https://github.com/CiscoDevNet/iOAM).
 * <p>
 *
 * @author Srihari Raghavan (srihari@cisco.com)
 * @version 0.1
 * @since 2016-06-01
 */
public class SfcPotRpc implements SfcIoamNbPotService{

    private static final Logger LOG = LoggerFactory.getLogger(SfcPotRpc.class);

    public static SfcPotRpc getSfcPotRpc() {
        return new SfcPotRpc();
    }

    /**
     * This method triggers enabling of Proof of Transit feature on the
     * selected SFC/RSP and passes in iOAM PoT parameters as well.
     * <p>
     *
     * @param input RPC input including a Rendered Service Path name
     * @return RPC output including a success or failure code.
     */
    @Override
    public Future<RpcResult<EnableSfcIoamPotRenderedPathOutput>> enableSfcIoamPotRenderedPath(
            EnableSfcIoamPotRenderedPathInput input) {
        boolean ret;
        RpcResultBuilder<EnableSfcIoamPotRenderedPathOutput> rpcResultBuilder;
        RspName rspName = new RspName(input.getSfcIoamPotRspName());

        /* Enable iOAM Proof of Transit on the SFC/RSP */
        ret = SfcPotRspProcessor.enableSfcPot(rspName,
                                              input.getRefreshPeriodTimeUnits(),
                                              input.getRefreshPeriodValue(),
                                              input.getIoamPotBitMask(),
                                              input.getIoamPotNumProfiles());
        if (ret) {
            /* success */
            EnableSfcIoamPotRenderedPathOutputBuilder enableSfcIoamPotRenderedPathOutputBuilder =
                    new EnableSfcIoamPotRenderedPathOutputBuilder();
            enableSfcIoamPotRenderedPathOutputBuilder.setResult(true);
            rpcResultBuilder =
                    RpcResultBuilder.success(enableSfcIoamPotRenderedPathOutputBuilder.build());
        } else {
            String message = "Error enabling SFC Proof of Transit for Rendered Service Path: " +
                              rspName.getValue();
            rpcResultBuilder = RpcResultBuilder.<EnableSfcIoamPotRenderedPathOutput>failed().
                               withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }


    /**
     * This method triggers disabling of Proof of Transit feature on the
     * selected SFC/RSP.
     * <p>
     *
     * @param input RPC input including a Rendered Service Path name
     * @return RPC output including a success or failure code.
     */
    @Override
    public Future<RpcResult<DisableSfcIoamPotRenderedPathOutput>> disableSfcIoamPotRenderedPath(
            DisableSfcIoamPotRenderedPathInput input) {
        boolean ret;
        RpcResultBuilder<DisableSfcIoamPotRenderedPathOutput> rpcResultBuilder;
        RspName rspName = new RspName(input.getSfcIoamPotRspName());

        /* Disable iOAM Proof of transit for this SFC/RSP */
        ret = SfcPotRspProcessor.disableSfcPot(rspName);
        if (ret) {
            /* success */
            DisableSfcIoamPotRenderedPathOutputBuilder disableSfcIoamPotRenderedPathOutputBuilder =
                                                  new DisableSfcIoamPotRenderedPathOutputBuilder();
            disableSfcIoamPotRenderedPathOutputBuilder.setResult(true);
            rpcResultBuilder =
                    RpcResultBuilder.success(disableSfcIoamPotRenderedPathOutputBuilder.build());
        } else {
            String message = "Error disabling SFC Proof of Transit for Rendered Service Path: " +
                              rspName.getValue();
            rpcResultBuilder =
                    RpcResultBuilder.<DisableSfcIoamPotRenderedPathOutput>failed().
                    withError(ErrorType.APPLICATION, message);
        }

        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
