/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.json.JSONObject;
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;


/**
 * This class provides a REST client implementation to
 * manage SB devices that provide a REST server interface.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderRestAPI extends SfcProviderAbstractRestAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRestAPI.class);
    private static final String HTTP_ERROR_MSG = "Failed : HTTP error code : ";

    SfcProviderRestAPI (Object[] params, Class[] paramsTypes, String m) {
        super(params, paramsTypes, m);
    }

    private String getServiceFunctionForwarderURI(ServiceFunctionForwarder serviceFunctionForwarder) {
        return  "http://localhost:8181/restconf/config/" +
                "service-function-forwarder:service-function-forwarders/" +
                "service-function-forwarder/" + serviceFunctionForwarder.getName();
    }

    public void putServiceFunctionForwarder (ServiceFunctionForwarder serviceFunctionForwarder) {

        printTraceStart(LOG);
        JSONObject jsonObject = null;
        String sffURI;

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        /*
        {  <== topJsonObj
             "service-function-forwarder": [  <== sffJsonArray
            { <== sffJsonElement, sffJsonEntryobj
                "name": "SFF-bootstrap",
                   "rest-uri": "http://198.18.134.23", <== sffRestUriElement, restUri
                    "sff-data-plane-locator": [
                {



        JsonParser parser = new JsonParser();
        JsonObject topJsonObj = parser.parse(sffJSON).getAsJsonObject();

        JsonArray sffJsonArray =  topJsonObj.getAsJsonArray("service-function-forwarder");
        JsonElement sffJsonElement = sffJsonArray.get(0);
        JsonObject sffJsonEntryobj = sffJsonElement.getAsJsonObject();
        JsonElement sffRestUriElement = sffJsonEntryobj.get("rest-uri");
        String restUri = sffRestUriElement.toString();
        */

        String sffJSON = getRESTObj(getServiceFunctionForwarderURI(serviceFunctionForwarder));
        String restURI = serviceFunctionForwarder.getRestUri().getValue();
        //restURI = "http://127.0.0.1:5000";

        sffURI = restURI + "/config/service" +
                "-function-forwarder:service-function-forwarders/service" +
                "-function-forwarder/" +
                serviceFunctionForwarder.getName();

        ClientResponse putClientRemoteResponse;

        putClientRemoteResponse = client
                .resource(sffURI).type(MediaType.APPLICATION_JSON_TYPE)
                .put(ClientResponse.class, sffJSON);

        if (putClientRemoteResponse.getStatus() >= 300)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + putClientRemoteResponse.getStatus(),
                    putClientRemoteResponse);
        }

        putClientRemoteResponse.close();
        printTraceStop(LOG);

    }


    public void deleteServiceFunctionForwarder (ServiceFunctionForwarder serviceFunctionForwarder)
    {

        printTraceStart(LOG);
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse deleteClientRemoteResponse;

        String restURI = serviceFunctionForwarder.getRestUri().getValue();

        String sffURI = restURI + "/config/service" +
                "-function-forwarder:service-function-forwarders/service" +
                "-function-forwarder/" +
                serviceFunctionForwarder.getName();
        deleteClientRemoteResponse = client
                .resource(sffURI).type(MediaType.APPLICATION_JSON_TYPE)
                .delete(ClientResponse.class);

        if (deleteClientRemoteResponse.getStatus() >= 300)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + deleteClientRemoteResponse.getStatus(),
                    deleteClientRemoteResponse);
        }

        deleteClientRemoteResponse.close();
        printTraceStop(LOG);
    }

    /**
     * GETs a Generic JSON REST Object and returns it to the caller
     * <p>
     * @param restURI URI String
     * @return The SFP in JSON format
     */
    private String getRESTObj(String restURI) {

        printTraceStart(LOG);
        final HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter("admin", "admin");
        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse;
        client.addFilter(basicAuthFilter);
        getClientResponse = client
                .resource(restURI)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get(ClientResponse.class);
        if (getClientResponse.getStatus() != 200)
        {
            throw new UniformInterfaceException(HTTP_ERROR_MSG
                    + getClientResponse.getStatus(),
                    getClientResponse);
        }
        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();
        printTraceStop(LOG);
        return jsonOutput;
    }

    private String getRenderedServicePathURI(RenderedServicePath renderedServicePath) {
        return  "http://localhost:8181/restconf/operational/rendered-service-path:rendered-service-paths/" +
                "rendered-service-path/" + renderedServicePath.getName();
    }


    /**
     * Communicates SFP to REST URI found in SFF configuration Server.
     * It sends SFP information to each SFF present in the service-hop list.
     * <p>
     * @param renderedServicePath Service Function Path object
     */
    public void putRenderedServicePath (RenderedServicePath renderedServicePath) {

        printTraceStart(LOG);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        String sfpURI;
        String restURI;

        String sfpJSON = getRESTObj(getRenderedServicePathURI(renderedServicePath));

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        Set<String> sffNameSet = new HashSet<>();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            String sffName = renderedServicePathHop.getServiceFunctionForwarder();
            // We send the SFP message to each SFF only once
            if (sffNameSet.add(sffName))
            {
                Object[] serviceForwarderObj = {sffName};
                Class[] serviceForwarderClass = {String.class};
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI =
                        SfcProviderServiceForwarderAPI.getRead(serviceForwarderObj, serviceForwarderClass);

                Future<Object> future = odlSfc.executor.submit
                        (sfcProviderServiceForwarderAPI);
                ClientResponse putClientRemoteResponse;

                try
                {
                    ServiceFunctionForwarder serviceFunctionForwarder =
                            (ServiceFunctionForwarder) future
                                    .get();
                    restURI = serviceFunctionForwarder.getRestUri().getValue();
                    // Testing
                    //restURI = "http://127.0.0.1:5000";

                    sfpURI = restURI + "/operational/rendered-service-path:" +
                        "rendered-service-paths/rendered-service-path/" + renderedServicePath.getName();
                    putClientRemoteResponse = client.resource(sfpURI).type(MediaType
                            .APPLICATION_JSON_TYPE).put(ClientResponse.class, sfpJSON);
                    if (putClientRemoteResponse.getStatus() >= 300)
                    {
                        throw new UniformInterfaceException(HTTP_ERROR_MSG
                                + putClientRemoteResponse.getStatus(),
                                putClientRemoteResponse);
                    }
                    putClientRemoteResponse.close();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                } catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
            }
        }


        printTraceStop(LOG);

    }

    /**
     * Communicates SFP to REST URI found in SFF configuration Server.
     * It sends SFP information to each SFF present in the service-hop list.
     * <p>
     * @param renderedServicePath Service Function Path object
     */
    public void deleteServiceFunctionPath (RenderedServicePath renderedServicePath) {

        printTraceStart(LOG);

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);
        String sfpURI;
        String restURI;

        List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
        Set<String> sffNameSet = new HashSet<>();
        for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
            String sffName = renderedServicePathHop.getServiceFunctionForwarder();
            // We send the SFP message to each SFF only once
            if (sffNameSet.add(sffName))
            {
                Object[] serviceForwarderObj = {sffName};
                Class[] serviceForwarderClass = {String.class};
                SfcProviderServiceForwarderAPI sfcProviderServiceForwarderAPI =
                        SfcProviderServiceForwarderAPI.getRead(serviceForwarderObj, serviceForwarderClass);

                Future<Object> future = odlSfc.executor.submit
                        (sfcProviderServiceForwarderAPI);
                ClientResponse deleteClientRemoteResponse;

                try
                {
                    ServiceFunctionForwarder serviceFunctionForwarder =
                            (ServiceFunctionForwarder) future
                                    .get();
                    restURI = serviceFunctionForwarder.getRestUri().getValue();
                    // Testing
                    //restURI = "http://127.0.0.1:5000";

                    sfpURI = restURI + "/operational/rendered-service-path:" +
                            "rendered-service-paths/rendered-service-path/" + renderedServicePath.getName();
                    deleteClientRemoteResponse = client.resource(sfpURI).type(MediaType
                            .APPLICATION_JSON_TYPE).delete(ClientResponse.class);
                    if (deleteClientRemoteResponse.getStatus() >= 300)
                    {
                        throw new UniformInterfaceException(HTTP_ERROR_MSG
                                + deleteClientRemoteResponse.getStatus(),
                                deleteClientRemoteResponse);
                    }
                    deleteClientRemoteResponse.close();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                } catch (ExecutionException e)
                {
                    e.printStackTrace();
                }
            }
        }


        printTraceStop(LOG);

    }

    public static  SfcProviderRestAPI getPutServiceFunctionForwarder (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionForwarder");
    }

    public static  SfcProviderRestAPI getDeleteServiceFunctionForwarder (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "deleteServiceFunctionForwarder");
    }

    public static  SfcProviderRestAPI getPutRenderedServicePath (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putRenderedServicePath");
    }

    public static  SfcProviderRestAPI getDeleteRenderedServicePath (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "deleteRenderedServicePathh");
    }
}
