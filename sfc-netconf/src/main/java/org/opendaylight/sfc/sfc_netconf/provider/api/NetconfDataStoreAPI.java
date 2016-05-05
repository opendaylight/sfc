/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_netconf.provider.api;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePathKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;


public class NetconfDataStoreAPI implements Callable {

    private final Logger LOG = LoggerFactory.getLogger(NetconfDataStoreAPI.class);

    private final DataBroker mountpoint;
    private final Object data;
    private final Transaction currentTransaction;
    private LogicalDatastoreType datastoreType;

    public enum Transaction {
        WRITE_FUNCTION, READ_FUNCTION,DELETE_FUNCTION,
        WRITE_LOCAL, READ_LOCAL, DELETE_LOCAL,
        WRITE_REMOTE, READ_REMOTE, DELETE_REMOTE,
        WRITE_PATH, DELETE_PATH }

    public NetconfDataStoreAPI(DataBroker mountPoint, Object data, Transaction transaction) {
        this.mountpoint = mountPoint;
        this.data = data;
        currentTransaction = transaction;
        datastoreType = CONFIGURATION;
    }

    @Override
    public Object call() {
        switch (currentTransaction) {
            case WRITE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunction serviceFunction = (ServiceFunction) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfUtils
                            .createSfIid(serviceFunction.getKey());
                    writeMergeTransaction(serviceFunctionIid, serviceFunction);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case READ_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    SfName serviceFunction = (SfName) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfUtils
                            .createSfIid(new ServiceFunctionKey(serviceFunction.getValue()));
                    datastoreType = OPERATIONAL;
                    return readTransaction(serviceFunctionIid);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case DELETE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunctionKey serviceFunctionKey = (ServiceFunctionKey) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfUtils
                            .createSfIid(serviceFunctionKey);
                    deleteTransaction(serviceFunctionIid);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case WRITE_LOCAL: {
                try {
                    Preconditions.checkNotNull(data);
                    Local localSff = (Local) data;
                    InstanceIdentifier<Local> localIid = SfcNetconfUtils.createLocalSffIid();
                    writeMergeTransaction(localIid, localSff);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of Local", data);
                }
                break;
            }
            case READ_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcNetconfUtils.createLocalSffIid();
                datastoreType = OPERATIONAL;
                return readTransaction(localIid);
            }
            case DELETE_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcNetconfUtils.createLocalSffIid();
                deleteTransaction(localIid);
                break;
            }
            case WRITE_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFfName remoteSff = (ServiceFfName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfUtils.createRemoteSffIid(remoteSff);
                    writeMergeTransaction(remoteIid, remoteSff);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFfName", data);
                }
                break;
            }
            case DELETE_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFfName remoteSff = (ServiceFfName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfUtils.createRemoteSffIid(remoteSff);
                    deleteTransaction(remoteIid);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFfName", data);
                }
                break;
            }
            case READ_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    SffName remoteSffName = (SffName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfUtils.createRemoteSffIid(remoteSffName);
                    datastoreType = OPERATIONAL;
                    return readTransaction(remoteIid);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of SffName", data);
                }
                break;
            }
            case WRITE_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePath path = (ServicePath) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcNetconfUtils.createServicePathIid(path
                            .getKey());
                    writeMergeTransaction(pathIid, path);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePath", data);
                }
                break;
            }
            case DELETE_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePathKey pathKey = (ServicePathKey) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcNetconfUtils.createServicePathIid(pathKey);
                    return deleteTransaction(pathIid);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePathKey", data);
                }
                break;
            }
        }
        return null;
    }

    private <U extends DataObject> void writeMergeTransaction(InstanceIdentifier<U> addIID, U data)  {
        WriteTransaction writeTransaction = mountpoint.newWriteOnlyTransaction();
        writeTransaction.merge(Preconditions.checkNotNull(datastoreType), addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTransaction.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
        }
    }

    private <U extends DataObject> boolean deleteTransaction(InstanceIdentifier<U> deleteIID)  {
        WriteTransaction writeTx = mountpoint.newWriteOnlyTransaction();
        writeTx.delete(Preconditions.checkNotNull(datastoreType), deleteIID);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Delete transaction failed to {}", e.getMessage());
            return false;
        }
    }

    private <U extends DataObject> U readTransaction(InstanceIdentifier<U> readIID)  {
        ReadOnlyTransaction readTx = mountpoint.newReadOnlyTransaction();
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readTx.read(Preconditions.checkNotNull(datastoreType), readIID);
        try {
            Optional<U> optional = submitFuture.checkedGet();
            if (optional != null && optional.isPresent()) {
                return optional.get();
            } else {
                LOG.debug("Failed to read. {}", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read transaction failed to {} " , e);
        }
        return null;
    }
}
