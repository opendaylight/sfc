/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sfc.provider.OpendaylightSfc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * <p/>
 *
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-06-30
 */
public abstract class SfcProviderAbstractAPI implements Callable<Object> {

    protected static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    private static final Logger LOG = LoggerFactory.getLogger
            (SfcProviderAbstractAPI.class);
    private String methodName = null;
    private Object[] parameters;
    private Class[] parameterTypes;
    protected DataBroker dataBroker;

    public String getMethodName()
    {
        return methodName;
    }

    public void setMethodName(String methodName)
    {
        this.methodName = methodName;
    }

    public Object[] getParameters()
    {
        return parameters;
    }

    public void setParameters(Object[] newParameters)
    {
        this.parameters = newParameters;
    }

    public Class[] getParameterTypes()
    {
        return parameterTypes;
    }

    public void setParameterTypes(Class[] newParameterTypes)
    {
        this.parameterTypes = newParameterTypes;
    }

    public DataBroker getDataBroker()
    {
        return dataBroker;
    }

    public void setDataBroker(DataBroker dataBroker)
    {
        this.dataBroker = dataBroker;
    }

    SfcProviderAbstractAPI(Object[] params, String m) {
        setMethodName(m);
        setParameters(new Object[params.length]);
        setParameterTypes(new Class[params.length]);
        setParameters(Arrays.copyOf(params, params.length));
        setDataBroker(odlSfc.getDataProvider());

        for (int i = 0; i < params.length; i++) {
            this.parameterTypes[i] = params[i].getClass();
        }
    }

    protected SfcProviderAbstractAPI(Object[] params, Class[] paramsTypes, String m) {
        setMethodName(m);
        setParameters(new Object[params.length]);
        setParameterTypes(new Class[params.length]);
        setParameters(Arrays.copyOf(params, params.length));
        setParameterTypes(Arrays.copyOf(paramsTypes, paramsTypes.length));
        setDataBroker(odlSfc.getDataProvider());
    }

    @Override
    public final Object call() {
        Object result = null;
        if (methodName != null) {
            Class<?> c = this.getClass();
            Method method;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                result = method.invoke(this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LOG.error("Could not find method in class");
            }
        }
        return result;
    }

}
