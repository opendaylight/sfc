/*
 * Copyright (c) 2015 Cisco Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_lisp.provider;

import com.google.common.net.InetAddresses;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.opendaylight.lispflowmapping.lisp.util.LispAddressStringifier;
import org.opendaylight.lispflowmapping.lisp.util.LispAddressUtil;
import org.opendaylight.sfc.provider.api.SfcProviderAclAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceClassifierAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.Ip;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.data.plane.locator.locator.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.ApplicationData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.eid.container.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.mapping.record.container.MappingRecord.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.mapping.record.container.MappingRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.lisp.proto.rev151105.rloc.container.Rloc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.AddMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.AddMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.EidUri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.GetMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.GetMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.MappingDatabase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.MappingOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.RemoveMappingInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.RemoveMappingInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.VniUri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.db.instance.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.db.instance.MappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.mapping.database.VirtualNetworkIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.lfm.mappingservice.rev150906.mapping.database.VirtualNetworkIdentifierKey;
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

    public static Ip createLocator(ApplicationData applicationData) {
        IpAddress ip = new IpAddress(new Ipv4Address(InetAddresses.fromInteger(
                applicationData.getApplicationData().getIpTos()).getHostAddress()));
        Ip locatorType = new IpBuilder().setIp(ip).setPort(applicationData.getApplicationData().getLocalPortLow())
                .build();
        return locatorType;
    }

    public static GetMappingInput buildGetMappingInput(Eid eid) {
        return new GetMappingInputBuilder().setEid(eid).build();
    }

    public static AddMappingInput buildAddMappingInput(Eid eid, List<Rloc> locators) {
        MappingRecordBuilder record = new MappingRecordBuilder();

        record.setAction(Action.NoAction).setAuthoritative(true).setEid(eid)
                .setLocatorRecord(LispAddressUtil.asLocatorRecords(locators)).setMapVersion((short) 0)
                .setRecordTtl(1440);
        return new AddMappingInputBuilder().setMappingRecord(record.build()).build();
    }

    public static RemoveMappingInput buildRemoveMappingInput(Eid eid, int mask) {
        RemoveMappingInputBuilder rmib = new RemoveMappingInputBuilder();
        rmib.setEid(eid);
        return rmib.build();
    }

    public static InstanceIdentifier<Mapping> buildMappingIid(Mapping mapping) {
        Eid eid = mapping.getMappingRecord().getEid();

        VirtualNetworkIdentifierKey vniKey = new VirtualNetworkIdentifierKey(
                new VniUri(Long.toString(eid.getVirtualNetworkId().getValue())));
        MappingKey eidKey = new MappingKey(new EidUri(LispAddressStringifier.getURIString(eid)), MappingOrigin.Northbound);
        return InstanceIdentifier.create(MappingDatabase.class)
                .child(VirtualNetworkIdentifier.class, vniKey).child(Mapping.class, eidKey);
    }

    public static Acl getServiceFunctionAcl(SfpName sfPathName) {
        ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(sfPathName);
        String classifierName = serviceFunctionPath.getClassifier();
        Acl acl = null;
        if (classifierName != null) {
            ServiceFunctionClassifier classifier =
                    SfcProviderServiceClassifierAPI.readServiceClassifier(classifierName);
            if (classifier != null && classifier.getAcl() != null) {
                acl = SfcProviderAclAPI.readAccessList(classifier.getAcl().getName(), classifier.getAcl().getType());
            }
        }
        return acl;
    }
}
