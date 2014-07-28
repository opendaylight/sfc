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
 * Abstract parent Runnable for the REST API.
 * <p/>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @author Konstantin Blagov (blagov.sk@hotmail.com)
 * @version 0.1
 * @since 2014-07-28
 */
abstract public class SfcProviderAbstractRestAPI implements Runnable {
    protected static final OpendaylightSfc odlSfc = OpendaylightSfc.getOpendaylightSfcObj();
    protected String methodName = null;
    protected Object[] parameters;
    protected Class[] parameterTypes;

    protected SfcProviderAbstractRestAPI(Object[] params, Class[] paramsTypes, String m) {
        this.methodName = m;
        this.parameters = new Object[params.length];
        this.parameterTypes = new Class[params.length];
        this.parameters = Arrays.copyOf(params, params.length);
        this.parameterTypes = Arrays.copyOf(paramsTypes, paramsTypes.length);
    }

    @Override
    public void run() {
        if (methodName != null) {
            //Class[] parameterTypes = {ServiceFunctionChain.class};
            Class<?> c = this.getClass();
            Method method = null;
            try {
                method = c.getDeclaredMethod(methodName, parameterTypes);
                method.invoke(this, parameters);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

}
