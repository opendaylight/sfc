/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.NetflocService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.CreateServiceChainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.CreateServiceChainOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.CreateServiceChainOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.DeleteServiceChainInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.ListServiceChainsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.ListServiceChainsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.Chains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.ChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.chains.Chain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.chains.ChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netfloc.rev150105.chains.ChainKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.ILinkPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.INetworkPath;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IServiceChain;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IHostPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IServiceChainListener;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetflocServiceImpl implements NetflocService, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NetflocServiceImpl.class);
    private final ExecutorService executor;
    private Map<String,IServiceChain> activeChains = new HashMap<String, IServiceChain>();
    private List<IServiceChainListener> serviceChainListeners = new LinkedList<IServiceChainListener>();
    private NetworkGraph graph;
    private int chainNumber = 0;
    private String chainID;
    private DataBroker dataBroker;
    //Static for the moment
    private String CHAIN_OWNER = "ZHAW";

    public NetflocServiceImpl(NetworkGraph graph, DataBroker dataBroker) {
        this.graph = graph;
        this.executor = Executors.newFixedThreadPool(1);
        this.dataBroker = dataBroker;
    }

    public void close() {
        this.executor.shutdown();
    }

    public void registerServiceChainListener(IServiceChainListener nsl) {
        this.serviceChainListeners.add(nsl);
    }

    private RpcError idNotFoundError() {
        return RpcResultBuilder.newError( ErrorType.APPLICATION, "not-found",
            "Service Chain Id not found", null, null, null );
    }

    private RpcError wrongAmoutOfPortsError() {
        return RpcResultBuilder.newError( ErrorType.APPLICATION, "input-condition",
            "Service Chain Input cannot have an odd number of Neutron Ports", null, null, null );
    }

    private RpcError portNotFoundError(List<String> ports) {
        return RpcResultBuilder.newError( ErrorType.APPLICATION, "graph-state",
            "Did not find all Neutron Ports in the Network Graph " + ports.toString(), null, null, null );
    }

    private RpcError pathNotClosedError() {
        return RpcResultBuilder.newError( ErrorType.APPLICATION, "graph-state",
            "Path is not closed in the Network Graph", null, null, null );
    }
    /*
    * Create service chain:
    * POST http://localhost:8181/restconf/operations/netfloc:create-service-chain
    * Headers: Content-Type: application/json
    * { "input" : { "neutron-ports" : "2a6f9ea7-dd2f-4ce1-8030-d999856fb558","5ec846bb-faf3-4f4e-83c1-fe253ff75ccb" } }
    * Response: 200 OK (application/json)
    * Body: {"output“: { "service-chain-id":$chain_id }
    */
    @Override
    public Future<RpcResult<CreateServiceChainOutput>> createServiceChain(CreateServiceChainInput input) {

        if (Arrays.asList(input.getNeutronPorts().split(",")).size() % 2 != 0) {
            logger.error("Service Chain Input cannot have an odd number of Neutron Ports");
            RpcError error = wrongAmoutOfPortsError();
            return null;
        }
        List<INetworkPath> chainNetworkPaths = new LinkedList<INetworkPath>();
        List<IHostPort> chainPorts = new LinkedList<IHostPort>();
        List<String> portsNotFound = new LinkedList<String>();
        List<String> chainPortIDs = new LinkedList<String>();
        List<String> neutronPortIDs = new LinkedList<String>();

        logger.info("NetflocServiceImpl createServiceChain: {}", input);
        for (String portID : Arrays.asList(input.getNeutronPorts().split(","))) {
            boolean found = false;
            for (IHostPort port : graph.getHostPorts()) {
                if (portID.equals(port.getNeutronUuid())) {
                    chainPorts.add(port);
                    chainPortIDs.add(port.getNeutronUuid());
                    neutronPortIDs.add(port.getNeutronUuid());
                    found = true;
                    break;
                }
            }
            if (!found) {
                portsNotFound.add(portID);
            }
        }
        logger.info("NetflocServiceImpl chainPortIDs: {}", chainPortIDs);

        if (portsNotFound.size() > 0) {
            RpcError error = portNotFoundError(portsNotFound);
            logger.error("Did not find all Neutron Ports in the Network Graph " + portsNotFound.toString());
            return Futures.immediateFuture( RpcResultBuilder.<CreateServiceChainOutput> failed().withRpcError(error).build() );
        }

        for (int i = 0; i < chainPorts.size(); i = i + 2) {
            INetworkPath path = this.graph.getNetworkPath(chainPorts.get(i), chainPorts.get(i+1));
            if (path == null) {
                logger.error("NetflocServiceImpl Path is not closed between {} and {}", chainPorts.get(i).getMacAddress(), chainPorts.get(i+1).getMacAddress());
                return Futures.immediateFuture( RpcResultBuilder.<CreateServiceChainOutput> failed().withRpcError(pathNotClosedError()).build() );
            }
            chainNetworkPaths.add(path);
        }

        chainNumber = chainNumber+1;
        // Instantiate ServiceChain
        ServiceChain chainInstance = new ServiceChain(chainNetworkPaths, chainNumber);
        chainInstance.setNeutronPortsList(neutronPortIDs);
        logger.info("NetflocServiceImpl Neutron ports list: {} ", chainInstance.getNeutronPortsList());

        for (IServiceChainListener scl : this.serviceChainListeners) {
            scl.serviceChainCreated(chainInstance);
        }
        this.activeChains.put("" + chainNumber, chainInstance);
        // Add chain data to OPERATIONAL datastore
        this.addChainData(new FutureCallback<Void>() {
            public void onSuccess(Void result) {
                logger.info("NetflocServiceImpl New service chain stored in OPERATIONAL data store");
            }
            public void onFailure(Throwable t) {
                logger.info("NetflocServiceImpl New service chain failed to store");
            }
        });
        chainID = "Chain_" + chainNumber + ": " + neutronPortIDs;
        logger.info("NetflocServiceImpl Created chain ID: {}", chainID);
        return Futures.immediateFuture(RpcResultBuilder.<CreateServiceChainOutput> success(new CreateServiceChainOutputBuilder().setServiceChainId("" + chainNumber).build()).build());
    }

    private void addChainData(FutureCallback<Void> cb) {
        ReadWriteTransaction wt = this.dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Chains> CHAIN_IID = InstanceIdentifier.builder(Chains.class).build();
        List<Chain> listChain = new LinkedList<Chain>();

        for (Map.Entry<String, IServiceChain> chain : activeChains.entrySet() )  {
            String chainID = Integer.toString(chain.getValue().getChainId());
            List<String> neutronPortIDs = chain.getValue().getNeutronPortsList();
            Chain chainBuilder = new ChainBuilder().setOwnerId(CHAIN_OWNER).setId(chainID).setChainConnectionPoint(neutronPortIDs).build();
            listChain.add(chainBuilder);
        }
        Chains chain = new ChainsBuilder().setChain(listChain).build();
        wt.merge(LogicalDatastoreType.OPERATIONAL, CHAIN_IID, chain);
        this.commitWriteTransaction(wt, cb, 3, 3);
    }

    private void removeChainData(String chainID, FutureCallback<Void> cb) {
        ReadWriteTransaction wt = this.dataBroker.newReadWriteTransaction();
        InstanceIdentifier<Chain> CHAIN_IID = InstanceIdentifier.builder(Chains.class).child(Chain.class, new ChainKey(chainID)).toInstance();
        wt.delete(LogicalDatastoreType.OPERATIONAL, CHAIN_IID);
        this.commitWriteTransaction(wt, cb, 3, 3);
    }

    private void commitWriteTransaction(final ReadWriteTransaction wt, final FutureCallback<Void> cb, final int totalTries, final int tries) {
        Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
            public void onSuccess(Void result) {
                logger.info("Transaction success after {} tries for {}", totalTries - tries + 1, wt);
                cb.onSuccess(result);
            }
            public void onFailure(Throwable t) {
                if (t instanceof OptimisticLockFailedException) {
                    if((tries - 1) > 0) {
                        logger.warn("Transaction retry {} for {}", totalTries - tries + 1, wt);
                        commitWriteTransaction(wt, cb, totalTries, tries - 1);
                    } else {
                        logger.error("Transaction out of retries: ", wt);
                        cb.onFailure(t);
                    }
                } else {
                    if (t instanceof DataValidationFailedException) {
                        logger.error("Transaction validation failed {}", t.getMessage());
                    } else {
                        logger.error("Transaction failed {}", t.getMessage());
                    }
                    cb.onFailure(t);
                }
            }
        });
    }
    /**
     * Delete Service Chain
     * POST http://localhost/restconf/operations/netfloc:delete-service-chain
     * Headers: Content-Type: application/json
     * Body: {"input": {service-chain-id :$chain_id }}
     */
    @Override
    public Future<RpcResult<java.lang.Void>> deleteServiceChain(DeleteServiceChainInput input) {
        String id = input.getServiceChainId();
        IServiceChain sc = activeChains.get(id);
        if (sc == null) {
            return Futures.immediateFuture( RpcResultBuilder.<Void> failed().withRpcError(idNotFoundError()).build() );
        }
        for (IServiceChainListener scl : this.serviceChainListeners) {
            scl.serviceChainDeleted(sc);
        }
        this.activeChains.remove(id);
        this.removeChainData(id, new FutureCallback<Void>() {
            public void onSuccess(Void result) {
                logger.info("NetflocServiceImpl Service chain was removed from the OPERATIONAL data store");
            }
            public void onFailure(Throwable t) {
                logger.info("NetflocServiceImpl Service chain failed to remove form data store");
            }
        });
        logger.info("NetflocServiceImpl Deleted chain with ID: {}", id);
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }
    /**
     * List Service Chain
     * POST http://localhost/restconf/operations/netfloc:list-service-chains
     * Headers: Content-Type: application/json
     * Response: 200 OK (application/json)
     * {output": {
     * "service-chains": "Chain_2: [3194c1ba-d264-4a1d-a4f1-272aec9a4d4c, f707e65d-51b9-4c9f-b9cf-a6ed616dec1c, 5900d654-ca89-47c9-81cd-7fc808b2a141, 55560526-1b65-4e41-9e60-25374e58a282], Chain_1: [88cf9740-b029-4542-85de-c28535769023, 34079251-5787-47cc-9dba-c1952dd2863d, 3bf7e97f-8cf9-448c-a815-6ac6dcbe3a0b, 2e89343b-2f08-4d1f-9467-80fc97174f31], "}}
     */
    @Override
    public Future<RpcResult<ListServiceChainsOutput>> listServiceChains() {
        int chainNumber = 0;
        String chainID = "";
        List<String> chainsList = new LinkedList<String>();
        List<String> neutronPortIDs;

        for (Map.Entry<String, IServiceChain> chain : activeChains.entrySet()) {
            chainNumber = chain.getValue().getChainId();
            neutronPortIDs = chain.getValue().getNeutronPortsList();
            chainID = "Chain_" + chainNumber + ": " + neutronPortIDs;
            chainsList.add(chainID);
        }
        logger.info("NetflocServiceImpl Service Chains list: {}", chainsList);
        return Futures.immediateFuture(RpcResultBuilder.<ListServiceChainsOutput> success(new ListServiceChainsOutputBuilder().setServiceChains(chainsList).build()).build());
    }
}
