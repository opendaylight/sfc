/*
 * Copyright (c) 2016 Ericsson Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.rpc;

import com.google.common.base.Preconditions;

import java.util.List;

import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
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

/**
 * This class provides SFC OF renderer with access to Genius RPCs needed in order
 * to support Logical SFF feature
 * @author Diego Granados (diego.jesus.granados.lopez@ericsson.com)
 *
 */
public class SfcGeniusRpcClient implements BindingAwareConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusRpcClient.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static ConsumerContext sessionData;
    private ItmRpcService itmRpcService;
    private OdlInterfaceRpcService interfaceManagerRpcService;
    private static SfcGeniusRpcClient sfcGeniusRpcClient;
    /*
     * Tunnel key used in the transport zone created to support the logical SFF
     */
    private static final long LOGICAL_SFF_TZ__DEFAULT_TUNNEL_KEY = 0;

    private SfcGeniusRpcClient() {}

    public static SfcGeniusRpcClient getInstance() {

        if (sfcGeniusRpcClient == null) {
            sfcGeniusRpcClient = new SfcGeniusRpcClient();
            sfcGeniusRpcClient.setSessionHelper();
        }

        return sfcGeniusRpcClient;
    }

    private void setSessionHelper()  {
        printTraceStart(LOG);
        try {
            if(odlSfc.getBroker()!=null) {
                if(sessionData==null) {
                    sessionData = odlSfc.getBroker().registerConsumer(this);
                    Preconditions.checkState(sessionData != null,"SfcGeniusRpcClient register is not available.");
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

    /**
     * Retrieve egress instructions from Genius
     *
     * @param targetInterfaceName the interface to use
     * @param interfaceIsPartOfTheTransportZone true when the interface is part of the transport zone (i.e. it is
     *        an interface between switching elements in different compute nodes); false when it is the
     *        neutron interface of a SF
     * @return The egress instructions to use, or null when the RPC invokation failed
     */
    public List<Instruction> getEgressInstructionsFromGeniusRPC(String targetInterfaceName, boolean interfaceIsPartOfTheTransportZone) {

        printTraceStart(LOG);

        List<Instruction> result = null;
        boolean successful = false;
        GetEgressInstructionsForInterfaceInputBuilder builder = new GetEgressInstructionsForInterfaceInputBuilder();
        builder.setIntfName(targetInterfaceName);
        if (interfaceIsPartOfTheTransportZone) {
            builder.setTunnelKey(LOGICAL_SFF_TZ__DEFAULT_TUNNEL_KEY);
        }

        GetEgressInstructionsForInterfaceInput input = builder.build();
        try {
            OdlInterfaceRpcService service = getInterfaceManagerRpcService();
            if (service != null) {
                RpcResult<GetEgressInstructionsForInterfaceOutput> output = service
                        .getEgressInstructionsForInterface(input).get();
                if (output.isSuccessful()) {
                    result = output.getResult().getInstruction();
                    LOG.debug("getEgressInstructionsFromGeniusRPC({}) succeeded",
                            input);
                    successful = true;
                } else {
                    LOG.error("getEgressInstructionsFromGeniusRPC({}) failed",
                            input);
                }
            } else {
                LOG.error(
                        "getEgressInstructionsFromGeniusRPC({}) failed (service couldn't be retrieved)",
                        input);
            }
        } catch (Exception e) {
            LOG.error("failed to retrieve egress instructions: {}, input was ", e, input);
        }
        printTraceStop(LOG);

        if (!successful) {
            result = null;
        }
        return result;
    }

    /**
     * Given a pair of data plane node identifiers, the method returns the interface to
     * use for sending traffic from the first dpn to the second. This method assumes
     * that a Genius' transport zone has been created including all the dataplane nodes
     * involved in the SFC chain, so vxlan-gpe tunnels exist beforehand between all
     * data plane nodes.
     *
     * @param srcDpid    DPN ID for the source dataplane node
     * @param dstDpid    DPN ID for the target dataplane node
     * @return  The interface to use for traffic steering between the given dataplane
     *          nodes(null when some problem arises during the retrieval)
     */
    public String getTargetInterfaceFromGeniusRPC(DpnIdType srcDpid, DpnIdType dstDpid) {

        printTraceStart(LOG);

        String interfaceName = null;
        boolean successful = false;

        GetTunnelInterfaceNameInputBuilder builder = new GetTunnelInterfaceNameInputBuilder();
        builder.setSourceDpid(srcDpid.getValue());
        builder.setDestinationDpid(dstDpid.getValue());
        builder.setTunnelType(TunnelTypeVxlanGpe.class);

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
                    successful = true;
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
            LOG.error("failed to retrieve target interface name: {} ", e);
        }
        printTraceStop(LOG);

        if (!successful) {
            interfaceName = null;
        }

        return interfaceName;
    }

    /**Given a neutron interface to which a VM (hosting a SF instance) is attached, the method
     * returns the DPN ID for the dataplane node in the compute node where the VM is running
     *
     * @param logicalInterfaceName    the neutron interface that the SF is attached to
     * @return     the DPN ID for the dataplane node in the compute node hosting the SF, or
     *             null when the dpn id cannot be resolved
     */
    public DpnIdType getDpnIdFromInterfaceNameFromGeniusRPC(String logicalInterfaceName) {

        DpnIdType dpnid = null;
        boolean successful = false;
        printTraceStart(LOG);

        GetDpidFromInterfaceInputBuilder builder = new GetDpidFromInterfaceInputBuilder();
        builder.setIntfName(logicalInterfaceName);

        GetDpidFromInterfaceInput input = builder.build();
        LOG.info("getDpnIdFromInterfaceNameFromGeniusRPC: starting (logical interface={})",
                logicalInterfaceName);
        try {
            OdlInterfaceRpcService service = getInterfaceManagerRpcService();
            if (service != null) {
                LOG.info("getDpnIdFromInterfaceNameFromGeniusRPC: service is not null, invoking rpc");
                RpcResult<GetDpidFromInterfaceOutput> output = service
                        .getDpidFromInterface(input).get();
                if (output.isSuccessful()) {
                    dpnid = new DpnIdType(output.getResult().getDpid());
                    LOG.info("getDpnIdFromInterfaceNameFromGeniusRPC({}) succeeded: {}",
                            input, output);
                    successful = true;
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
            LOG.error("failed to retrieve target interface name: {} ", e);
        }
        printTraceStop(LOG);
        if (!successful) {
            dpnid = null;
        }
        return dpnid;
    }

    private ItmRpcService getItmRpcService() {
        return itmRpcService;
    }

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        itmRpcService = session.getRpcService(ItmRpcService.class);
        interfaceManagerRpcService = session.getRpcService(OdlInterfaceRpcService.class);
    }

    private OdlInterfaceRpcService getInterfaceManagerRpcService() {
        LOG.debug("getInterfaceManagerRpcService: sessionData={}", sessionData);
        return interfaceManagerRpcService;
    }

    public void close() {
        LOG.info("close: terminating sfc genius rpc services");
        sfcGeniusRpcClient = null;
    }
}
