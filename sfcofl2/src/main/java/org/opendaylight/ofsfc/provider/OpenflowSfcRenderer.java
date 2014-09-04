package org.opendaylight.ofsfc.provider;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChains;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.ServiceFunctionTypes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OpenflowSfcRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(OpenflowSfcRenderer.class);

    protected ExecutorService executor;
    protected DataBroker dataBroker;
    private static OpenflowSfcRenderer openflowSfcRendererObj;
    private OpenflowSfpDataListener openflowSfpDataListener;
    private OpenflowAclDataListener openflowAclDataListener;

    private Future<RpcResult<Void>> currentTask;

    public OpenflowSfcRenderer(DataBroker dataBroker) {
        executor = Executors.newFixedThreadPool(1);
        openflowSfcRendererObj = this;
        setDataBroker(dataBroker);
        openflowSfpDataListener = new OpenflowSfpDataListener(dataBroker);
        openflowAclDataListener = new OpenflowAclDataListener(dataBroker);
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public DataBroker getDataProvider() {
        return dataBroker;
    }

    public static OpenflowSfcRenderer getOpendaylightSfcObj() {
        return OpenflowSfcRenderer.openflowSfcRendererObj;
    }

    /**
     * Implemented from the AutoCloseable interface.
     */
    @Override
    public void close() throws ExecutionException, InterruptedException {
        // When we close this service we need to shutdown our executor!
    }
}