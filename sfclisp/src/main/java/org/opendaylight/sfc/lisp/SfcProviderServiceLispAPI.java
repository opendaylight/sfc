package org.opendaylight.sfc.lisp;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.SfcProviderAbstractAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcProviderServiceLispAPI extends SfcProviderAbstractAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderServiceLispAPI.class);
    private static LispUpdater lispUpdater = new LispUpdater();

    SfcProviderServiceLispAPI(Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    public static SfcProviderServiceLispAPI getUpdateServiceFunction(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceLispAPI(params, paramsTypes, "updateServiceFunction");
    }

    public static SfcProviderServiceLispAPI getUpdateServiceFunctionForwarder(Object[] params, Class[] paramsTypes) {
        return new SfcProviderServiceLispAPI(params, paramsTypes, "updateServiceFunctionForwarder");
    }

    protected static boolean updateServiceFunction(ServiceFunction sf) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {
            sf = lispUpdater.updateLispData(sf);

            InstanceIdentifier<ServiceFunction> sfEntryIID = InstanceIdentifier.builder(ServiceFunctions.class)
                    .child(ServiceFunction.class, sf.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.CONFIGURATION, sfEntryIID, sf, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

    protected static boolean updateServiceFunctionForwarder(ServiceFunctionForwarder sff) {
        boolean ret = false;
        LOG.debug("\n####### Start: {}", Thread.currentThread().getStackTrace()[1]);
        if (odlSfc.getDataProvider() != null) {

            sff = lispUpdater.updateLispData(sff);

            InstanceIdentifier<ServiceFunctionForwarder> sffEntryIID = InstanceIdentifier.builder(ServiceFunctionForwarders.class)
                    .child(ServiceFunctionForwarder.class, sff.getKey()).toInstance();

            WriteTransaction writeTx = odlSfc.getDataProvider().newWriteOnlyTransaction();
            writeTx.merge(LogicalDatastoreType.CONFIGURATION, sffEntryIID, sff, true);
            writeTx.commit();

            ret = true;
        }
        LOG.debug("\n########## Stop: {}", Thread.currentThread().getStackTrace()[1]);
        return ret;
    }

}
