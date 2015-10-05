/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev150317.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev150820.lispaddress.LispAddressContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.EidUri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.IidUri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.MappingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.MappingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.db.instance.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.db.instance.MappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.mapping.database.InstanceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.mapping.database.InstanceIdKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcLispUtil {
    private final static Logger LOG = LoggerFactory.getLogger(SfcLispUtil.class);

    public static Object submitCallable(Callable<Object> callable, ExecutorService executor) {
        Future<Object> future = null;
        Object result = null;

        future = executor.submit(callable);

        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("{} failed to: {}", callable.toString(), e);
        }

        return result;
    }

    public static InstanceIdentifier<Mapping> buildMappingIid(Mapping mapping) {
        LispAddressContainer eid = mapping.getLispAddressContainer();
        InstanceIdKey iidKey = new InstanceIdKey(new IidUri(Long.toString(LispUtil.getLispInstanceId(eid))));
        MappingKey eidKey = new MappingKey(new EidUri(LispUtil.getAddressString(eid)),MappingOrigin.Northbound);
        return InstanceIdentifier.create(MappingDatabase.class)
                .child(InstanceId.class, iidKey).child(Mapping.class, eidKey);
    }

    public static Acl getServiceFunctionAcl(String sfPathName) {
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(sfPathName);
        String classifierName = serviceFunctionPath.getClassifier();
        Acl acl = null;
        if(classifierName != null) {
            ServiceFunctionClassifier classifier = SfcProviderServiceClassifierAPI.readServiceClassifierExecutor(classifierName);
            String aclName = classifier.getAccessList();
            acl = SfcProviderAclAPI.readAccessListExecutor(aclName);
        }
        return acl;
    }
}
