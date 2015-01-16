package org.opendaylight.sfc.sbrest.provider;

import org.opendaylight.sfc.sbrest.json.ExporterFactory;
import org.opendaylight.sfc.sbrest.json.SfcExporterFactory;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;

import java.util.concurrent.Callable;

public class SbRestPutSfcTask implements Callable {
    private static final String ACCEPT = "application/json";
    private static final String HTTP_ERROR_MSG = "Failed, HTTP error code : ";
    private static final int HTTP_OK = 200;

    private ServiceFunctionChain serviceFunctionChain;
    private String urlMgmt;

    public SbRestPutSfcTask(ServiceFunctionChain serviceFunctionChain, String urlMgmt) {
        this.serviceFunctionChain = serviceFunctionChain;
        this.urlMgmt = urlMgmt;
    }

    @Override
    public Object call() throws Exception {
        putServiceFunctionChain(serviceFunctionChain);
        return null;
    }


    private void putServiceFunctionChain(ServiceFunctionChain serviceFunctionChain) {

        ExporterFactory ef = new SfcExporterFactory();
        String jsonOutput = ef.getExporter().exportJson(serviceFunctionChain);

        System.out.println("[:SFC:]" + jsonOutput + "\n");

    }

}
