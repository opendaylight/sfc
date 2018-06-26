*
 * Copyright (c) 2016, 2017 Inspur . All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.concurrent.ExecutionException;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRpcClient;
import org.opendaylight.sfc.ofrenderer.openflow.SfcOfFlowProgrammerInterface;
import org.opendaylight.sfc.ofrenderer.processors.SfcRenderingException;
import org.opendaylight.sfc.ofrenderer.utils.SfcOfBaseProviderUtils;
import org.opendaylight.sfc.util.openflow.SfcOpenflowUtils;
import org.opendaylight.sfc.util.openflow.writer.SfcOfFlowWriterInterface;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfDataPlaneLocatorName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.function.base.SfDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Other;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.DpnIdType;
import org.opendaylight.yang.gen.v1.urn.ericsson.params.xml.ns.yang.sfc.sff.logical.rev160620.service.functions.service.function.sf.data.plane.locator.locator.type.LogicalInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;

/**
 * This class listens to changes (addition, update, removal) in Service
 * Functions taking the appropriate actions.
 *
 * @author David Surez (david.suarez.fuentes@gmail.com)
 *
 */
public class SfcOfServiceFunctionListener implements DataTreeChangeListener<ServiceFunction>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOfServiceFunctionListener.class);

    private final DataBroker dataBroker;

    private ListenerRegistration<SfcOfServiceFunctionListener> listenerRegistration;

    public static final int FLOW_PRIORITY_PROXY_FLOW = 3;

    public static final short PROXY_TABLE = 0;

    private SfcOfFlowWriterInterface sfcOfFlowWriter;

    private final SfcOfFlowProgrammerInterface sfcOfFlowProgrammer;

    private final SfcOfBaseProviderUtils sfcOfProviderUtils;

    private final SfcGeniusRpcClient theGeniusRpcClient;

    private OdlInterfaceRpcService interfaceManagerRpcService;

    private static final short TABLE0 = 0;

    public SfcOfServiceFunctionListener(final DataBroker dataBroker,
            SfcOfFlowProgrammerInterface sfcOfFlowProgrammer,SfcOfBaseProviderUtils sfcOfProviderUtils,
            RpcProviderRegistry rpcProviderRegistry,SfcOfFlowWriterInterface sfcOfFlowWriter) {
        this.dataBroker = dataBroker;
        this.sfcOfFlowProgrammer = sfcOfFlowProgrammer;
        this.sfcOfProviderUtils = sfcOfProviderUtils;
        this.sfcOfFlowWriter = sfcOfFlowWriter;
        this.theGeniusRpcClient = new SfcGeniusRpcClient(rpcProviderRegistry);
        if (rpcProviderRegistry != null) {
            interfaceManagerRpcService = rpcProviderRegistry.getRpcService(OdlInterfaceRpcService.class);
        }
   
        registerListeners();
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Closing listener...");
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    private void registerListeners() {
        final DataTreeIdentifier<ServiceFunction> treeId = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(ServiceFunctions.class).child(ServiceFunction.class));

        listenerRegistration = dataBroker.registerDataTreeChangeListener(treeId, this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<ServiceFunction>> collection) {
        for (DataTreeModification<ServiceFunction> modification : collection) {
            DataObjectModification<ServiceFunction> rootNode = modification.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    if (rootNode.getDataBefore() == null && rootNode.getDataAfter() != null) {
                        LOG.info("rqz add: storing name [{}] type [{}]", rootNode.getDataAfter().getName().getValue(),
                                       rootNode.getDataAfter().getType().getValue());
                        configureProxyFlow(rootNode.getDataAfter());
                    } else if (rootNode.getDataAfter().equals(rootNode.getDataBefore())) {
                        LOG.info("rqz SfcOfRspDataListener.onDataTreeChanged update RSP Before:{} After:{}");
                    }
                    break;
                case DELETE:
                    if (rootNode.getDataBefore() != null) {
                        removeProxyFlow(rootNode.getDataBefore());               
                        LOG.info("rqz SfcOfRspDataListener.onDataTreeChanged delete sf {}");
                    }
                    break;
                default:
                    break;
            }
        }
    }



    protected void configureProxyFlow(ServiceFunction sf) {

        Long proxyPort = null;
        Long vnfInport = null;
        Long vnfOutport = null;
        String sfLogicalInterface = SfcGeniusDataUtils.getSfLogicalInterface(sf);
        String vnfLogicalInterfaceInput = getProxyLogicalInterfaceNameInput(sf);
        String vnfLogicalInterfaceOutput = getProxyLogicalInterfaceNameOutput(sf);
        proxyPort = getPortFromInterfaceFromGeniusRPC(sfLogicalInterface);
        vnfInport = getPortFromInterfaceFromGeniusRPC(vnfLogicalInterfaceInput);
        vnfOutport = getPortFromInterfaceFromGeniusRPC(vnfLogicalInterfaceOutput);
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();
        SfDataPlaneLocator sfDpl = sfDplList.get(0);
               Optional<DpnIdType> dpnid = getGeniusRpcClient()
                 .getDpnIdFromInterfaceNameFromGeniusRPC(sfLogicalInterface);
        if (!dpnid.isPresent()) {
            throw new SfcRenderingException("rqz populateSffGraph:failed.dpnid for interface ["
                     + sfLogicalInterface + "] was not returned by genius. "
                     + "Rendered service path cannot be generated at this time");
        }
        LOG.info("rqz populateSffGraph: retrieved dpn id for SF {} :[{}] ", sf.getName(), dpnid.get());

        String dpid = dpnid.get().getValue().toString();
        String sffNodeName = "openflow:" + dpid;
        String proxyName = getLogicalNameFromSf(sf);
        String vnfName = getOtherNameFromSf(sf);
        String flowName = "from_proxy" + proxyName;
        String flowName1 = "from_vnf" + vnfName ;
        configureInportFlow(dpid,proxyPort,vnfInport,3,sffNodeName,flowName);
        configureInportFlow(dpid,vnfOutport,proxyPort,200,sffNodeName,flowName1);
        this.sfcOfFlowProgrammer.flushFlows();

    }

    protected void removeProxyFlow(ServiceFunction sf) {
        String proxyName = getLogicalNameFromSf(sf);
        String vnfName = getOtherNameFromSf(sf);
        String sfLogicalInterface = SfcGeniusDataUtils.getSfLogicalInterface(sf);
        String vnfLogicalInterfaceInput = getProxyLogicalInterfaceNameInput(sf);
        String vnfLogicalInterfaceOutput = getProxyLogicalInterfaceNameOutput(sf);
        Long proxyPort = getPortFromInterfaceFromGeniusRPC(sfLogicalInterface);
        Long vnfInport = getPortFromInterfaceFromGeniusRPC(vnfLogicalInterfaceInput);
        Long vnfOutport = getPortFromInterfaceFromGeniusRPC(vnfLogicalInterfaceOutput);
        Optional<DpnIdType> dpnid = getGeniusRpcClient()
               .getDpnIdFromInterfaceNameFromGeniusRPC(sfLogicalInterface);
        if (!dpnid.isPresent()) {
            throw new SfcRenderingException("rqz populateSffGraph:failed.dpnid for interface ["
                   + sfLogicalInterface + "] was not returned by genius. "
                   + "Rendered service path cannot be generated at this time");
        }
        String dpid = dpnid.get().getValue().toString();
        String flowName = "from_proxy" + proxyName;
        String flowName1 = "from_vnf" + vnfName;
        removeFlowFromName(flowName,dpid);
        removeFlowFromName(flowName1,dpid);
    }

    private void configureInportFlow(String dpnid,Long inport,Long outport,int priority,String sffNodeName,
                        String flowName) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpnid + ":" + inport);
        MatchBuilder match = new MatchBuilder();
        match.setInPort(ncid);
        String out = outport.toString();
        List<Action> actionList = new ArrayList<>();
        actionList.add(SfcOpenflowUtils.createActionOutPort(out, actionList.size()));
        InstructionsBuilder isb = SfcOpenflowUtils.wrapActionsIntoApplyActionsInstruction(actionList);
      
        // Create and return the flow
        FlowBuilder flow = SfcOpenflowUtils.createFlowBuilder(PROXY_TABLE, priority,
                          flowName, match, isb);
               Long flowRspId = 1000000000000000L;
        sfcOfFlowWriter.writeFlow(flowRspId, sffNodeName, flow);
    }


    private OdlInterfaceRpcService getInterfaceManagerRpcService() {
        return interfaceManagerRpcService;
    }

    private SfcGeniusRpcClient getGeniusRpcClient() {
        return theGeniusRpcClient;
    }

    private Long getPortFromInterfaceFromGeniusRPC(String logicalInterfaceName) {

        Optional<Long> port = Optional.empty();
        Long port1 = null;
        boolean successful = false;

        GetPortFromInterfaceInputBuilder builder = new GetPortFromInterfaceInputBuilder();
        builder.setIntfName(logicalInterfaceName);
        GetPortFromInterfaceInput input = builder.build();
        try {
            OdlInterfaceRpcService service = getInterfaceManagerRpcService();
            if (service != null) {
                RpcResult<GetPortFromInterfaceOutput> output = service.getPortFromInterface(input).get();
                if (output.isSuccessful()) {
                    port = Optional.of(new Long(output.getResult().getPortno()));
                    port1 = port.get();
                    successful = true;
                } else {
                    LOG.error(" getportFromInterfaceNameFromGeniusRPC({}) failed: {}", input, output);
                }
            } else {
                LOG.error(" getportFromInterfaceNameFromGeniusRPC({}) failed", input);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("failed to retrieve target interface name: ", e);
        }
        if (!successful) {
            port = Optional.empty();
            port1 = null;
        }
        return port1;

    }


    private String getProxyLogicalInterfaceNameInput(ServiceFunction sf) {
        String interfaceName = null;
        String allinterfaceName = null;
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfdpl:sfDplList) {
            
            if (sfdpl.getLocatorType() != null && sfdpl.getLocatorType()
                      .getImplementedInterface() == Other.class) {
                Other logicalInterface = (Other) sfdpl.getLocatorType();
                allinterfaceName = logicalInterface.getOtherName();
                
                if (allinterfaceName.substring(0,1).equals("i")) {
                    interfaceName = allinterfaceName.substring(2);
                    
                }
            }
        }

        return interfaceName;
    }

    private String getProxyLogicalInterfaceNameOutput(ServiceFunction sf) {
        String interfaceName = null;
        String allinterfaceName = null;
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();

        for (SfDataPlaneLocator sfdpl:sfDplList) {
                            
            if (sfdpl.getLocatorType() != null && sfdpl.getLocatorType()
                  .getImplementedInterface() == Other.class) {
                Other logicalInterface = (Other) sfdpl.getLocatorType();
                allinterfaceName = logicalInterface.getOtherName();
                if (allinterfaceName.substring(0,1).equals("o")) {

                    interfaceName = allinterfaceName.substring(2);
                 
                }
            }

        }
        return interfaceName;
    }

    private String getLogicalNameFromSf(ServiceFunction sf) {
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();
        String logicalDplName = null;
        for (SfDataPlaneLocator sfdpl:sfDplList) {
            if (sfdpl.getLocatorType().getImplementedInterface().equals(LogicalInterface.class)) {
                LogicalInterface logicalInterface = (LogicalInterface) sfdpl.getLocatorType();
                SfDataPlaneLocatorName logical = sfdpl.getName();
                logicalDplName = logical.getValue();
                return logicalDplName;
            }
        }
        return null;
    }

    private String getOtherNameFromSf(ServiceFunction sf) {
        List<SfDataPlaneLocator> sfDplList = sf.getSfDataPlaneLocator();
        String otherDplName = null;
        for (SfDataPlaneLocator sfdpl:sfDplList) {
            if (sfdpl.getLocatorType().getImplementedInterface().equals(Other.class)) {
                Other logicalInterface = (Other) sfdpl.getLocatorType();
                SfDataPlaneLocatorName other = sfdpl.getName();
                otherDplName = other.getValue();
                return otherDplName;
            }
        }
        return null;  	  	
    }

    protected void removeFlowFromName(String flowName,String dpid) {
      
         String nodeId="openflow"+":"+dpid;
        NodeBuilder nodeBuilder = createNodeBuilder(nodeId);
        Table table0 = getTable(nodeBuilder,
                   TABLE0,
               dataBroker.newReadOnlyTransaction(),
               LogicalDatastoreType.CONFIGURATION);       
              if (table0 != null) {
            List<Flow> flows0 = table0.getFlow();
            for (Flow opFlow : flows0) {
                FlowId flowId = opFlow.getId();
                String flowName1 = opFlow.getFlowName();
                
                if (flowName1.indexOf(flowName) != -1) {
                    InstanceIdentifier<Flow> flowInst0 = InstanceIdentifier.builder(Nodes.class)
                           .child(Node.class, nodeBuilder.getKey())
                           .augmentation(FlowCapableNode.class)
                           .child(Table.class, new TableKey(TABLE0))
                           .child(Flow.class, new FlowKey(flowId))
                           .build();
                    WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
                    modification.delete(LogicalDatastoreType.CONFIGURATION, flowInst0);
                    CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
                    try {
                        commitFuture.get();  // TODO: Make it async (See bug 1362)
                        LOG.info("Transaction success for deletion of Flow : {}", flowName);
                    } catch (Exception e) {
                        LOG.error("Failed to remove flow : {}", flowName, e);
                        modification.cancel();
                    }
                }
                else {
                    continue;
                }
            }
        }	   
    }

    public static Table getTable(NodeBuilder nodeBuilder, short table,
           ReadOnlyTransaction readTx, final LogicalDatastoreType store) {
        try {
            com.google.common.base.Optional<Table> data = readTx.read(store, createTablePath(nodeBuilder, table)).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            LOG.error("Failed to get table {}", table, e);
        }

        LOG.info("Cannot find data for table {} in {}", table, store);
        return null;
    }

    public static InstanceIdentifier<Table> createTablePath(NodeBuilder nodeBuilder, short table) {
        return InstanceIdentifier.builder(Nodes.class)
               .child(Node.class, nodeBuilder.getKey())
               .augmentation(FlowCapableNode.class)
               .child(Table.class, new TableKey(table)).build();
    }

    public static String getNodeName(long dpidLong) {
        return "openflow" + ":" + dpidLong;
    }

    public static NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    public static NodeBuilder createNodeBuilder(long dpidLong) {
        return createNodeBuilder(getNodeName(dpidLong));
    }
}
