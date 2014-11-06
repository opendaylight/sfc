/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import java.util.concurrent.ExecutionException;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.ServiceFunctionClassifiers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.scf.rev140701.service.function.classifiers.ServiceFunctionClassifierKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.lists.AccessListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class has the APIs to operate on the ACL
 * datastore.
 *
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 * @see org.opendaylight.sfc.provider.SfcProviderSfEntryDataListener
 *
 *
 * <p>
 * @author Andrej Kincel (akincel@cisco.com)
 * @version 0.1
 * @since       2014-11-04
 */
public class SfcProviderAclAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderAclAPI.class);

    SfcProviderAclAPI(Object[] params, String m) {
        super(params, m);
    }

    SfcProviderAclAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderAclAPI getPut(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "putAcl");
    }

    public static SfcProviderAclAPI getRead(Object[] params, Class[] paramsTypes) {
        return new SfcProviderAclAPI(params, paramsTypes, "readAcl");
    }

    protected boolean putAcl(AccessList acl) {
        boolean ret = false;
        printTraceStart(LOG);
        if (dataBroker != null) {

            InstanceIdentifier<AccessList> aclEntryIID =
                    InstanceIdentifier.builder(AccessLists.class)
                            .child(AccessList.class, acl.getKey())
                            .toInstance();

            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION,
                    aclEntryIID, acl, true);
            writeTx.commit();
            ret = true;
        }
        printTraceStop(LOG);
        return ret;
    }

    protected AccessList readAcl(String aclName) {
        printTraceStart(LOG);
        AccessList acl = null;
        InstanceIdentifier<AccessList> aclIID;
        AccessListKey aclKey =
                new AccessListKey(aclName);
        aclIID = InstanceIdentifier.builder(AccessLists.class)
                .child(AccessList.class, aclKey).build();

        if (dataBroker != null) {
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            Optional<AccessList> aclDataObject;
            try {
                aclDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, aclIID).get();
                if (aclDataObject != null
                        && aclDataObject.isPresent()) {
                    acl = aclDataObject.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not read ACL " +
                        "configuration {}", aclName);
            }
        }
        printTraceStop(LOG);
        return acl;
    }

}
