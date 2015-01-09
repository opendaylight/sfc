package org.opendaylight.ofsfc.provider.utils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import com.google.common.base.Optional;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ofsfc.provider.OpenflowSfcRenderer;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarderKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SfcOfL2APIUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SfcOfL2APIUtil.class);
    private static final String LOG_STR_FAILED_TO = "failed to .... {}";
    private static final String LOG_STR_START = "\n########## Start: {}";
    private static final String LOG_STR_STOP  = "\n########## Stop: {}";

    // Hiding the implicit public constructor
    private SfcOfL2APIUtil() {
    }

    public static ServiceFunctionForwarder readServiceFunctionForwarder(DataBroker databroker,
            String serviceFunctionForwarderName) {
        LOG.debug(LOG_STR_START, Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionForwarder sff = null;
        InstanceIdentifier<ServiceFunctionForwarder> sffIID;
        ServiceFunctionForwarderKey serviceFunctionForwarderKey = new ServiceFunctionForwarderKey(
                serviceFunctionForwarderName);
        sffIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                .child(ServiceFunctionForwarder.class, serviceFunctionForwarderKey).build();

        if (databroker != null) {
            ReadOnlyTransaction readTx = databroker.newReadOnlyTransaction();
            Optional<ServiceFunctionForwarder> serviceFunctionForwarderDataObject = null;
            try {
                serviceFunctionForwarderDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sffIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn(LOG_STR_FAILED_TO, e);
            }
            if (serviceFunctionForwarderDataObject != null && serviceFunctionForwarderDataObject.isPresent()) {
                sff = serviceFunctionForwarderDataObject.get();
            }
        } else {
            LOG.debug("Databroker is null", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.debug(LOG_STR_STOP, Thread.currentThread().getStackTrace()[1]);
        return sff;
    }

    public static ServiceFunction readServiceFunction(String serviceFunctionName) {
        LOG.debug(LOG_STR_START, Thread.currentThread().getStackTrace()[1]);
        ServiceFunction sf = null;
        InstanceIdentifier<ServiceFunction> sfIID;
        ServiceFunctionKey serviceFunctionKey = new ServiceFunctionKey(serviceFunctionName);
        sfIID = InstanceIdentifier.builder(ServiceFunctions.class).child(ServiceFunction.class, serviceFunctionKey)
                .build();
        OpenflowSfcRenderer ofSfcRenderer = OpenflowSfcRenderer.getOpendaylightSfcObj();
        if (ofSfcRenderer.getDataProvider() != null) {
            ReadOnlyTransaction readTx = ofSfcRenderer.getDataProvider().newReadOnlyTransaction();
            Optional<ServiceFunction> serviceFunctionDataObject = null;
            try {
                serviceFunctionDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn(LOG_STR_FAILED_TO, e);
            }
            if (serviceFunctionDataObject != null && serviceFunctionDataObject.isPresent()) {
                sf = serviceFunctionDataObject.get();
            }
        }
        LOG.debug(LOG_STR_STOP, Thread.currentThread().getStackTrace()[1]);
        return sf;
    }

    public static ServiceFunctionPath readServiceFunctionPath(DataBroker databroker, String serviceFunctionPathName) {
        LOG.debug(LOG_STR_START, Thread.currentThread().getStackTrace()[1]);
        ServiceFunctionPath sff = null;
        InstanceIdentifier<ServiceFunctionPath> sfpIID;
        ServiceFunctionPathKey serviceFunctionPathKey = new ServiceFunctionPathKey(serviceFunctionPathName);
        sfpIID = InstanceIdentifier.builder(ServiceFunctionPaths.class)
                .child(ServiceFunctionPath.class, serviceFunctionPathKey).build();

        if (databroker != null) {
            ReadOnlyTransaction readTx = databroker.newReadOnlyTransaction();
            Optional<ServiceFunctionPath> serviceFunctionPathDataObject = null;
            try {
                serviceFunctionPathDataObject = readTx.read(LogicalDatastoreType.CONFIGURATION, sfpIID).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn(LOG_STR_FAILED_TO, e);
            }
            if (serviceFunctionPathDataObject != null && serviceFunctionPathDataObject.isPresent()) {
                sff = serviceFunctionPathDataObject.get();
            }
        } else {
            LOG.debug("Databroker is null", Thread.currentThread().getStackTrace()[1]);
        }
        LOG.debug(LOG_STR_STOP, Thread.currentThread().getStackTrace()[1]);
        return sff;
    }
}
