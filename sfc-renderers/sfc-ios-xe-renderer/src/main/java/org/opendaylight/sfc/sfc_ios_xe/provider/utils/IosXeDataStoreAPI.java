/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sfc_ios_xe.provider.utils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.netconf.api.NetconfDocumentedException;
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


public class IosXeDataStoreAPI implements Callable {

    private final Logger LOG = LoggerFactory.getLogger(IosXeDataStoreAPI.class);

    private final DataBroker mountpoint;
    private final Object data;
    private final Transaction currentTransaction;
    private final LogicalDatastoreType datastoreType;

    public enum Transaction {
        WRITE_FUNCTION, READ_FUNCTION, DELETE_FUNCTION,
        WRITE_LOCAL, READ_LOCAL, DELETE_LOCAL,
        WRITE_REMOTE, READ_REMOTE, DELETE_REMOTE,
        WRITE_PATH, READ_PATH, DELETE_PATH
    }

    public IosXeDataStoreAPI(DataBroker mountPoint, Object data, Transaction transaction,
                             LogicalDatastoreType datastoreType) {
        this.mountpoint = mountPoint;
        this.data = data;
        currentTransaction = transaction;
        this.datastoreType = datastoreType;
    }

    @Override
    public Object call() {
        switch (currentTransaction) {
            case WRITE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunction serviceFunction = (ServiceFunction) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcIosXeUtils
                            .createSfIid(serviceFunction.getKey());
                    return writeMergeTransaction(serviceFunctionIid, serviceFunction);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case READ_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    SfName serviceFunction = (SfName) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcIosXeUtils
                            .createSfIid(new ServiceFunctionKey(serviceFunction.getValue()));
                    return readTransaction(serviceFunctionIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case DELETE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunctionKey serviceFunctionKey = (ServiceFunctionKey) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcIosXeUtils
                            .createSfIid(serviceFunctionKey);
                    return deleteTransaction(serviceFunctionIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case WRITE_LOCAL: {
                try {
                    Preconditions.checkNotNull(data);
                    Local localSff = (Local) data;
                    InstanceIdentifier<Local> localIid = SfcIosXeUtils.createLocalSffIid();
                    return writeMergeTransaction(localIid, localSff);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of Local", data);
                }
                break;
            }
            case READ_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcIosXeUtils.createLocalSffIid();
                return readTransaction(localIid);
            }
            case DELETE_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcIosXeUtils.createLocalSffIid();
                return deleteTransaction(localIid);
            }
            case WRITE_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFfName remoteSff = (ServiceFfName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcIosXeUtils.createRemoteSffIid(remoteSff);
                    return writeMergeTransaction(remoteIid, remoteSff);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFfName", data);
                }
                break;
            }
            case DELETE_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFfName remoteSff = (ServiceFfName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcIosXeUtils.createRemoteSffIid(remoteSff);
                    return deleteTransaction(remoteIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFfName", data);
                }
                break;
            }
            case READ_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    SffName remoteSffName = (SffName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcIosXeUtils.createRemoteSffIid(remoteSffName);
                    return readTransaction(remoteIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of SffName", data);
                }
                break;
            }
            case WRITE_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePath path = (ServicePath) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcIosXeUtils.createServicePathIid(path
                            .getKey());
                    return writeMergeTransaction(pathIid, path);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePath", data);
                }
                break;
            }
            case READ_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePathKey pathKey = (ServicePathKey) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcIosXeUtils.createServicePathIid(pathKey);
                    return readTransaction(pathIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePath", data);
                }
                break;
            }
            case DELETE_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePathKey pathKey = (ServicePathKey) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcIosXeUtils.createServicePathIid(pathKey);
                    return deleteTransaction(pathIid);
                } catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePathKey", data);
                }
                break;
            }
        }
        return null;
    }

    private <U extends DataObject> boolean writeMergeTransaction(InstanceIdentifier<U> addIID, U data) {
        long timeout = 5000L;
        int attempt = 0;
        WriteTransaction transaction = null;
        do {
            attempt++;
            try {
                transaction = mountpoint.newWriteOnlyTransaction();
            } catch (RuntimeException e) {
                if (e.getCause().getClass().equals(NetconfDocumentedException.class)) {
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                        timeout += 1000L;
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage());
                }
            }
        } while (attempt <= 5 && transaction == null);
        if (transaction == null) {
            LOG.error("Maximum number of attempts reached");
            return false;
        }
        try {
            transaction.merge(Preconditions.checkNotNull(datastoreType), addIID, data);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Write transaction failed to {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
            return false;
        }
    }

    private <U extends DataObject> boolean deleteTransaction(InstanceIdentifier<U> deleteIID) {
        long timeout = 5000L;
        int attempt = 0;
        WriteTransaction transaction = null;
        do {
            attempt++;
            try {
                transaction = mountpoint.newWriteOnlyTransaction();
            } catch (RuntimeException e) {
                if (e.getCause().getClass().equals(NetconfDocumentedException.class)) {
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                        timeout += 1000L;
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage());
                }
            }
        } while (attempt <= 5 && transaction == null);
        if (transaction == null) {
            LOG.error("Maximum number of attempts reached");
            return false;
        }
        try {
            transaction.delete(Preconditions.checkNotNull(datastoreType), deleteIID);
            CheckedFuture<Void, TransactionCommitFailedException> submitFuture = transaction.submit();
            submitFuture.checkedGet();
            return true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Delete transaction failed to {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
            return false;
        }
    }

    private <U extends DataObject> U readTransaction(InstanceIdentifier<U> readIID) {
        long timeout = 5000L;
        int attempt = 0;
        ReadTransaction transaction = null;
        do {
            attempt++;
            try {
                transaction = mountpoint.newReadOnlyTransaction();
            } catch (RuntimeException e) {
                if (e.getCause().getClass().equals(NetconfDocumentedException.class)) {
                    LOG.warn("NetconfDocumentedException thrown, retrying ({})...", attempt);
                    try {
                        Thread.sleep(timeout);
                        timeout += 1000L;
                    } catch (InterruptedException i) {
                        LOG.error("Thread interrupted while waiting ... {} ", i);
                    }
                } else {
                    LOG.error("Runtime exception ... {}", e.getMessage());
                }
            }
        } while (attempt <= 5 && transaction == null);
        if (transaction == null) {
            LOG.error("Maximum number of attempts reached");
            return null;
        }
        try {
            CheckedFuture<Optional<U>, ReadFailedException> submitFuture =
                    transaction.read(Preconditions.checkNotNull(datastoreType), readIID);
            Optional<U> optional = submitFuture.checkedGet();
            if (optional != null && optional.isPresent()) {
                return optional.get();
            } else {
                LOG.debug("Failed to read. {}", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read transaction failed to {} ", e);
        } catch (Exception e) {
            LOG.error("Failed to .. {}", e.getMessage());
        }
        return null;
    }
}
