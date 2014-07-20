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
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;


/**
 * This class is the DataListener for SFF changes.
 *
 * <p>
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * @since       2014-06-30
 */
public class SfcProviderRestAPI implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderRestAPI.class);
    private static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;

    SfcProviderRestAPI (Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    public void putServiceFunctionForwarders (ServiceFunctionForwarders serviceFunctionForwarders) {

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse = client
                .resource("http://localhost:8080/restconf/config/service-function-forwarder:service-function-forwarders/")
                .accept("application/json")
                .get(ClientResponse.class);


        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientResponse= client
                .resource("http://localhost:5000/paths").type("application/json")
                .put(ClientResponse.class, jsonOutput);

        putClientResponse.close();

    }

    public void putServiceFunctionPaths (ServiceFunctionPaths serviceFunctionPaths) {

        ClientConfig clientConfig = new DefaultClientConfig();
        Client client = Client.create(clientConfig);

        ClientResponse getClientResponse = client
                .resource("http://localhost:8080/restconf/config/service-function-path:service-function-paths/")
                .accept("application/json")
                .get(ClientResponse.class);


        String jsonOutput = getClientResponse.getEntity(String.class);
        getClientResponse.close();

        ClientResponse putClientResponse= client
                .resource("http://localhost:5000/paths").type("application/json")
                .put(ClientResponse.class, jsonOutput);

        putClientResponse.close();

    }

    public static  SfcProviderRestAPI getPutServiceFunctionForwarders (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionForwarders");
    }

    public static  SfcProviderRestAPI getPutServiceFunctionPaths (Object[] params, Class[] paramsTypes) {
        return new SfcProviderRestAPI(params, paramsTypes, "putServiceFunctionPaths");
    }

    @Override
    public void run() {
        if (methodName != null) {
            //Class[] parameterTypes = {ServiceFunctionChain.class};
            Class c = this.getClass();
            Method method = null;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }
}
