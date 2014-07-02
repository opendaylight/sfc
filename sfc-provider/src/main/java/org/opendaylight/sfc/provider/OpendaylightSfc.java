/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140629.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140630.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140626.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140630.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140630.ServiceFunctionForwarders;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This the main SFC Provider class. It is instantiated from the
 * SFCProviderModule class.
 *
 * <p>
 * @author Konstantin Blagov ()
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 * @see org.opendaylight.controller.config.yang.config.sfc_provider.impl.SfcProviderModule
 */

public class OpendaylightSfc implements AutoCloseable {


   private static final Logger LOG = LoggerFactory.getLogger(OpendaylightSfc.class);
   public static final InstanceIdentifier<ServiceFunctions>  sfIID =
           InstanceIdentifier.builder(ServiceFunctions.class).build();
   public static final InstanceIdentifier<ServiceNodes>  snIID =
           InstanceIdentifier.builder(ServiceNodes.class).build();
   public static final InstanceIdentifier<ServiceFunctionPaths>  sfpIID =
           InstanceIdentifier.builder(ServiceFunctionPaths.class).build();
   public static final InstanceIdentifier<ServiceFunctionChains>  sfcIID =
           InstanceIdentifier.builder(ServiceFunctionChains.class).build();
   public static final InstanceIdentifier<ServiceFunctionForwarders>  sffIID =
           InstanceIdentifier.builder(ServiceFunctionForwarders.class).build();

   private ExecutorService executor;
   protected DataBrokerService dataProvider;
   private static OpendaylightSfc opendaylightSfcObj;

   private Future<RpcResult<Void>> currentTask;

   public OpendaylightSfc() {

       executor = Executors.newFixedThreadPool(1);
       opendaylightSfcObj = this;
   }

   private Future<RpcResult<Void>> inProgressError() {
       RpcResult<Void> result = Rpcs.<Void> getRpcResult(false, null, Collections.<RpcError> emptySet());
       return Futures.immediateFuture(result);
   }

   public void setDataProvider(DataBrokerService salDataProvider) {
       this.dataProvider = salDataProvider;
   }

    public DataBrokerService getDataProvider(DataBrokerService salDataProvider) {
        return this.dataProvider;
    }

    public static OpendaylightSfc getOpendaylightSfcObj () {
        return OpendaylightSfc.opendaylightSfcObj;
    }

   /**
    * Implemented from the AutoCloseable interface.
    */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
        executor.shutdown();

        if (dataProvider != null) {
            final DataModificationTransaction t = dataProvider.beginTransaction();
            t.removeOperationalData(sfIID);
            t.removeOperationalData(sfcIID);
            t.removeOperationalData(snIID);
            t.removeOperationalData(sffIID);
            t.removeOperationalData(sfpIID);
            t.commit().get();
        }
    }
}
