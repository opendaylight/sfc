/*
 * Copyright (c) 2016 Ericsson Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.ofrenderer.processors;


import com.google.common.base.Preconditions;

import java.util.List;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetDpidFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressInstructionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.GetTunnelInterfaceNameOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

public class SfcRendererRpcProviderAPI implements BindingAwareConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(SfcRendererRpcProviderAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static ConsumerContext sessionData;
    private ItmRpcService itmRpcService;
    private OdlInterfaceRpcService interfaceManagerRpcService;
    private static SfcRendererRpcProviderAPI sfcRendererRpcProvider;

    private SfcRendererRpcProviderAPI() {}

        public static SfcRendererRpcProviderAPI getInstance() {
            if (sfcRendererRpcProvider != null)
                return sfcRendererRpcProvider;
            else {
                sfcRendererRpcProvider = new SfcRendererRpcProviderAPI();
                sfcRendererRpcProvider.setSessionHelper();
                return sfcRendererRpcProvider;
            }
        }

    private void setSessionHelper()  {
        printTraceStart(LOG);
        try {
            if(odlSfc.getBroker()!=null) {
                if(sessionData==null) {
                    sessionData = odlSfc.getBroker().registerConsumer(this);
                    Preconditions.checkState(sessionData != null,"SfcRendererRpcProvider register is not available.");
                }
            }
        } catch (Exception e) {
            LOG.warn("failed to ...." , e);
        }
        printTraceStop(LOG);
    }

    protected void setSession()  {
        setSessionHelper();
    }

    public List<Instruction> getEgressInstructionsFromGeniusRPC(String targetInterfaceName) {
        printTraceStart(LOG);
        List<Instruction> result = null;
        GetEgressInstructionsForInterfaceInputBuilder builder = new GetEgressInstructionsForInterfaceInputBuilder();
        builder.setIntfName(targetInterfaceName);
        // TODO tunnel type?
        GetEgressInstructionsForInterfaceInput input = builder.build();
        try {
            OdlInterfaceRpcService service = getInterfaceManagerRpcService();
            if (service != null) {
                RpcResult<GetEgressInstructionsForInterfaceOutput> output = service
                        .getEgressInstructionsForInterface(input).get();
                if (output.isSuccessful()) {
                    result = output.getResult().getInstruction();
                    LOG.info("getEgressInstructionsForInterface({}) succeeded",
                            input);
                } else {
                    LOG.error("getEgressInstructionsForInterface({}) failed",
                            input);
                }
            } else {
                LOG.error(
                        "getEgressInstructionsForInterface({}) failed (service couldn't be retrieved)",
                        input);
            }
        } catch (Exception e) {
            LOG.error("failed to retrieve egress instructions: ", e);
        }
        printTraceStop(LOG);
        return result;
    }

    public String getTargetInterfaceFromGeniusRPC(DpnIdType srcDpid,
            DpnIdType dstDpid) {
        String interfaceName = null;
        printTraceStart(LOG);
        GetTunnelInterfaceNameInputBuilder builder = new GetTunnelInterfaceNameInputBuilder();
        builder.setSourceDpid(srcDpid.getValue());
        builder.setDestinationDpid(dstDpid.getValue());
        // TODO set tunnel type depending on RSP transport ?
        // builder.setTunnelType(null);
        GetTunnelInterfaceNameInput input = builder.build();
        try {
            ItmRpcService service = getItmRpcService();
            if (service != null) {
                RpcResult<GetTunnelInterfaceNameOutput> output = service
                        .getTunnelInterfaceName(input).get();
                if (output.isSuccessful()) {
                    interfaceName = output.getResult().getInterfaceName();
                    LOG.info("getTargetInterfaceFromGeniusRPC({}) succeeded",
                            input);
                } else {
                    LOG.error("getTargetInterfaceFromGeniusRPC({}) failed",
                            input);
                }
            } else {
                LOG.error(
                        "getTargetInterfaceFromGeniusRPC({}) failed (service couldn't be retrieved)",
                        input);
            }
        } catch (Exception e) {
            LOG.error("failed to retrieve target interface name: ", e);
        }
        printTraceStop(LOG);
        return interfaceName;
    }

    public DpnIdType getDpnIdFromInterfaceNameFromGeniusRPC(String logicalInterfaceName) {
        DpnIdType dpnid = null;
        printTraceStart(LOG);

        GetDpidFromInterfaceInputBuilder builder = new GetDpidFromInterfaceInputBuilder();
        builder.setIntfName(logicalInterfaceName);

        GetDpidFromInterfaceInput input = builder.build();
        LOG.info("getTargetInterfaceFromGeniusRPC(): starting (logical interface={})",
                logicalInterfaceName);
        try {
            OdlInterfaceRpcService service = getInterfaceManagerRpcService();
            if (service != null) {
                LOG.info("getTargetInterfaceFromGeniusRPC(): service is not null, invoking rpc");
                RpcResult<GetDpidFromInterfaceOutput> output = service
                        .getDpidFromInterface(input).get();
                if (output.isSuccessful()) {
                    dpnid = new DpnIdType(output.getResult().getDpid());
                    LOG.info("getTargetInterfaceFromGeniusRPC({}) succeeded: {}",
                            input, output);
                } else {
                    LOG.error("getTargetInterfaceFromGeniusRPC({}) failed: {}",
                            input, output);
                }
            } else {
                LOG.error(
                        "getTargetInterfaceFromGeniusRPC({}) failed (service couldn't be retrieved)",
                        input);
            }
        } catch (Exception e) {
            LOG.error("failed to retrieve target interface name: ", e);
        }
        printTraceStop(LOG);
        return dpnid;
    }



    public ItmRpcService getItmRpcService() {
        return itmRpcService;
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        itmRpcService = session.getRpcService(ItmRpcService.class);
        interfaceManagerRpcService = session.getRpcService(OdlInterfaceRpcService.class);
    }

    public OdlInterfaceRpcService getInterfaceManagerRpcService() {
        LOG.debug("  ediegra:getInterfaceManagerRpcService: sessionData={}", sessionData);
        return interfaceManagerRpcService;
    }

    public void close() {
        LOG.info("close(): terminating sfc renderer rpc services");
    }
}
