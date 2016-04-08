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
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308.Native;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.ServiceChain;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.ServicePath;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.Local;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfName;
import org.opendaylight.yang.gen.v1.urn.ios.rev160308._native.service.chain.service.function.forwarder.ServiceFfNameKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


public class SfcNetconfDataStoreAPI implements Callable {

    private Logger LOG = LoggerFactory.getLogger(SfcNetconfDataStoreAPI.class);

    private DataBroker mountpoint;
    private Object data;
    private Transaction currentTransaction;

    public enum Transaction { UPDATE_FUNCTION, DELETE_FUNCTION, UPDATE_LOCAL, UPDATE_REMOTE, DELETE_LOCAL, DELETE_REMOTE,
    READ_LOCAL, READ_FUNCTION, WRITE_PATH, READ_REMOTE}

    public SfcNetconfDataStoreAPI(DataBroker mountPoint, Object data, Transaction transaction) {
        this.mountpoint = mountPoint;
        this.data = data;
        currentTransaction = transaction;
    }

    @Override
    public Object call() {
        switch (currentTransaction) {
            case UPDATE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunction serviceFunction = (ServiceFunction) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfServiceFunctionAPI
                            .createSfIid(serviceFunction.getKey());
                    LOG.info("Writing service function {} with IID {}", serviceFunction, serviceFunctionIid);   // DELETE
                    writeMergeTransactionAPI(serviceFunctionIid, serviceFunction, LogicalDatastoreType.CONFIGURATION);
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
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfServiceFunctionAPI
                            .createSfIid(new ServiceFunctionKey(serviceFunction.getValue()));
                    LOG.info("Reading service function {} with IID {}", serviceFunction, serviceFunctionIid);  // DELETE
                    return readTransactionAPI(serviceFunctionIid, LogicalDatastoreType.OPERATIONAL);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case DELETE_FUNCTION: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFunction serviceFunction = (ServiceFunction) data;
                    InstanceIdentifier<ServiceFunction> serviceFunctionIid = SfcNetconfServiceFunctionAPI
                            .createSfIid(serviceFunction.getKey());
                    LOG.info("Removing service function {} with IID {}", serviceFunction, serviceFunctionIid);  // DELETE
                    deleteTransactionAPI(serviceFunctionIid, LogicalDatastoreType.CONFIGURATION);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFunction", data);
                }
                break;
            }
            case UPDATE_LOCAL: {
                try {
                    Preconditions.checkNotNull(data);
                    Local localSff = (Local) data;
                    InstanceIdentifier<Local> localIid = SfcNetconfServiceFunctionForwarderAPI.createLocalSffIid();
                    LOG.info("Writing local forwarder {} with IID {}", localSff, localIid);     // DELETE
                    writeMergeTransactionAPI(localIid, localSff, LogicalDatastoreType.CONFIGURATION);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of Local", data);
                }
                break;
            }
            case READ_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcNetconfServiceFunctionForwarderAPI.createLocalSffIid();
                LOG.info("Reading local forwarder {} with IID {}", localIid);     // DELETE
                return readTransactionAPI(localIid, LogicalDatastoreType.OPERATIONAL);
            }
            case DELETE_LOCAL: {
                InstanceIdentifier<Local> localIid = SfcNetconfServiceFunctionForwarderAPI.createLocalSffIid();
                LOG.info("Removing local forwarder with IID {}", localIid);     // DELETE
                deleteTransactionAPI(localIid, LogicalDatastoreType.CONFIGURATION);
                break;
            }
            case UPDATE_REMOTE: {
                try {
                    Preconditions.checkNotNull(data);
                    ServiceFfName remoteSff = (ServiceFfName) data;
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfServiceFunctionForwarderAPI
                            .createRemoteSffIid(remoteSff);
                    LOG.info("Writing remote forwarder {} with IID {}", remoteSff, remoteIid);  // DELETE
                    writeMergeTransactionAPI(remoteIid, remoteSff, LogicalDatastoreType.CONFIGURATION);
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
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfServiceFunctionForwarderAPI
                            .createRemoteSffIid(remoteSff);
                    LOG.info("Removing local forwarder {} with IID {}", remoteSff, remoteIid);  // DELETE
                    deleteTransactionAPI(remoteIid, LogicalDatastoreType.CONFIGURATION);
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
                    InstanceIdentifier<ServiceFfName> remoteIid = SfcNetconfServiceFunctionForwarderAPI
                            .createRemoteSffIid(remoteSffName);
                    LOG.info("Reading local forwarder {} with IID {}", remoteSffName, remoteIid);  // DELETE
                    return readTransactionAPI(remoteIid, LogicalDatastoreType.CONFIGURATION);
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServiceFfName", data);
                }
                break;
            }
            case WRITE_PATH: {
                try {
                    Preconditions.checkNotNull(data);
                    ServicePath path = (ServicePath) data;
                    InstanceIdentifier<ServicePath> pathIid = SfcNetconfServicePathAPI.createServicePathIid(path
                            .getKey());
                    LOG.info("Writing path {} with IID {}", path, pathIid);  // DELETE
                }
                catch (ClassCastException e) {
                    LOG.error("Argument data {} is not an instance of ServicePath", data);
                }
                break;
            }
        }
        return null;
    }

    private <U extends DataObject> void writeMergeTransactionAPI
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType)  {
        WriteTransaction writeTransaction = mountpoint.newWriteOnlyTransaction();
        writeTransaction.merge(logicalDatastoreType, addIID, data, true);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTransaction.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("writePutTransactionAPI: Transaction failed. Message: {}", e.getMessage());
        }
    }

    private <U extends DataObject> void deleteTransactionAPI
            (InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType)  {
        WriteTransaction writeTx = mountpoint.newWriteOnlyTransaction();
        writeTx.delete(logicalDatastoreType, deleteIID);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        try {
            submitFuture.checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("deleteTransactionAPI: Transaction failed. Message: {}", e.getMessage());
        }
    }

    private <U extends DataObject> U readTransactionAPI (InstanceIdentifier<U> readIID,
                                                               LogicalDatastoreType logicalDatastoreType)  {
        ReadOnlyTransaction readTx = mountpoint.newReadOnlyTransaction();
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readTx.read(logicalDatastoreType, readIID);
        try {
            Optional<U> optional = submitFuture.checkedGet();
            if (optional != null && optional.isPresent()) {
                return optional.get();
            } else {
                LOG.debug("{}: Failed to read", Thread.currentThread().getStackTrace()[1]);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to {} " , e);
        }
        return null;
    }
}
