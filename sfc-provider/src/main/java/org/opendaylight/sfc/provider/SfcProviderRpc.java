/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.ServiceFunctionChainService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.service.functions.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.ServiceFunctionChainsBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.PutServiceFunctionChainsInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * This class holds all RPCs methods for SFC Provider.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov ()
 * @version 0.1
 * @since       2014-06-30
 */

public class SfcProviderRpc implements ServiceFunctionService, ServiceFunctionChainService {

    private static final Logger LOG = LoggerFactory
            .getLogger(SfcProviderRpc.class);
    private OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private SfcProviderRpc sfcRpcObj;

    /*
    public SfcProviderRpc () {
        this.sfcRpcObj = this;
    }
    */

    public static SfcProviderRpc getSfcProviderRpc() {
        return new SfcProviderRpc();
    }


    private ServiceFunctions buildServiceFunctions(List<ServiceFunction> list) {

        ServiceFunctionsBuilder builder = new ServiceFunctionsBuilder();
        builder.setServiceFunction(list);
        return builder.build();
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunction(PutServiceFunctionInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("\n####### Input: " + input);


        if (odlSfc.dataProvider != null) {
            ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            ServiceFunction sf = sfbuilder.setName(input.getName()).setType(input.getType())
                    .setKey(sfkey).setIpMgmtAddress(input.getIpMgmtAddress()).build();

            InstanceIdentifier<ServiceFunction>  sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();

            t.putConfigurationData(sfIID, sf);
            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("Failed to update-function, operational otherwise", e);
            }

        } else {
            LOG.warn("\n####### Data Provider is NULL : {}", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<ReadServiceFunctionOutput>> readServiceFunction(ReadServiceFunctionInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("Input: " + input);

        if (odlSfc.dataProvider != null) {
            String name = input.getName();
            ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
            InstanceIdentifier<ServiceFunction> sfIID;
            sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sfkey).toInstance();
            DataObject dataObject = odlSfc.dataProvider.readConfigurationData(sfIID);
            if (dataObject instanceof ServiceFunction) {
                LOG.info("readServiceFunction Success: {}", ((ServiceFunction) dataObject).getName());
                ServiceFunction sf = (ServiceFunction) dataObject;
                ReadServiceFunctionOutput rsfoutput = null;
                ReadServiceFunctionOutputBuilder outputBuilder = new ReadServiceFunctionOutputBuilder();
                outputBuilder.setName(sf.getName())
                        .setIpMgmtAddress(sf.getIpMgmtAddress())
                        .setType(sf.getType());
                rsfoutput = outputBuilder.build();
                LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
                return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, rsfoutput, Collections.<RpcError>emptySet()));
            }
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        } else {
            LOG.warn("dataProvider is null");
            LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
            return Futures.immediateFuture(Rpcs.<ReadServiceFunctionOutput>getRpcResult(true, null, Collections.<RpcError>emptySet()));
        }
    }

    @Override
    public Future<RpcResult<Void>> deleteAllServiceFunction() {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.dataProvider != null) {
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();
            t.removeConfigurationData(odlSfc.sfIID);
            try {
                t.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("deleteServiceFunction failed", e);
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> deleteServiceFunction(DeleteServiceFunctionInput input) {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        LOG.info("Input: " + input);
        if (odlSfc.dataProvider != null) {
            DataObject dataObject = odlSfc.dataProvider.readConfigurationData(odlSfc.sfIID);
            if (dataObject instanceof ServiceFunctions) {

                ServiceFunctionKey sfkey = new ServiceFunctionKey(input.getName());
                InstanceIdentifier<ServiceFunction> sfIID;
                sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                        child(ServiceFunction.class, sfkey).toInstance();

                final DataModificationTransaction t = odlSfc.dataProvider
                        .beginTransaction();
                t.removeConfigurationData(sfIID);
                try {
                    t.commit().get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("deleteServiceFunction failed", e);
                }
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    @Override
    public Future<RpcResult<Void>> putServiceFunctionChains(PutServiceFunctionChainsInput input) {
        LOG.info("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionChainsBuilder builder = new ServiceFunctionChainsBuilder();
        builder = builder.setServiceFunctionChain(input.getServiceFunctionChain());

        ServiceFunctionChains sfcs = builder.build();

        if (odlSfc.dataProvider != null) {
            final DataModificationTransaction t = odlSfc.dataProvider.beginTransaction();
            t.putConfigurationData(odlSfc.sfcIID, sfcs);

            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("putServiceFunctionChain failed, operational otherwise", e);
            }
        } else {
            LOG.warn("dataProvider is null");
        }
        return Futures.immediateFuture(Rpcs.<Void>getRpcResult(true,
                Collections.<RpcError>emptySet()));
    }

    public Future<RpcResult<Void>> updateFunctionDpiWa20()  {
        LOG.info("\n########## Start: {}", Thread.currentThread().getStackTrace()[1]);
        IpAddress ip = new IpAddress("10.0.0.11".toCharArray());
        ServiceFunctionBuilder sfbuilder = new ServiceFunctionBuilder();
        ServiceFunctionKey sfkey = new ServiceFunctionKey("fw-wa");
        sfbuilder.setKey(sfkey);
        ServiceFunction sf = sfbuilder.setName("fw-wa")
                .setType(Firewall.class)
                .setIpMgmtAddress(ip).build();


        LOG.info("updateFunctionDpiWa20: bbb\n");
        if (odlSfc.dataProvider != null) {
            LOG.info("updateFunctionDpiWa20: dataProvider not null\n");
            final DataModificationTransaction t = odlSfc.dataProvider
                    .beginTransaction();

            InstanceIdentifier<ServiceFunction>  sfIID = InstanceIdentifier.builder(ServiceFunctions.class).
                    child(ServiceFunction.class, sf.getKey()).toInstance();

            t.putConfigurationData(sfIID, sf);
            try {
                t.commit().get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.warn("Failed to update-function, operational otherwise", e);
            }
        }
        else{
            LOG.info("updateFunctionDpiWa20: dataProvider not null\n");
        }
        LOG.info("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return Futures.immediateFuture(Rpcs.<Void> getRpcResult(true,
                Collections.<RpcError> emptySet()));
    }
}
