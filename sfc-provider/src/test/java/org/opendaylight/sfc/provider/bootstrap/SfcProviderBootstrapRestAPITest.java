/*
 * Copyright (c) 2015 Intel Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.bootstrap;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static org.junit.Assert.assertNotNull;

public class SfcProviderBootstrapRestAPITest {

    @Test
    public void testGetPutBootstrapData(){
        writeJSON();

        Object[] parameters = {"SFF1"};
        Class[] parameterTypes = {ServiceFunctionForwarder.class};

        SfcProviderBootstrapRestAPI sfcProviderBootstrapRestAPI = new SfcProviderBootstrapRestAPI(parameters, parameterTypes, "m");

        sfcProviderBootstrapRestAPI.putBootstrapData();
        assertNotNull("No data created.", SfcProviderBootstrapRestAPI.getPutBootstrapData(parameters, parameterTypes));

        //remove created file with all folders
        File file = new File("configuration/startup/sfc_provider_config.json");
        file.delete();
        File folder = new File("configuration");
        rmdir(folder);
    }

    private void  writeJSON(){
        try {
            File targetFile = new File("configuration/startup/sfc_provider_config.json");
            File parent = targetFile.getParentFile();
            if(!parent.exists() && !parent.mkdirs()){
                throw new IllegalStateException("Couldn't create dir: " + parent);
            }
            PrintWriter out = null;
            try{
                out = new PrintWriter("configuration/startup/sfc_provider_config.json");
            }
            catch(FileNotFoundException fnfe){
                fnfe.printStackTrace();
            }
            out.println("{\n" +
                    "  \"bootstrap\": {\n" +
                    "    \"bootstrapDataDir\": \"configuration/startup/\",\n" +
                    "    \"configDataUrl\": \"http://localhost:8181/restconf/config/\",\n" +
                    "    \"configDataMimeType\": \"application/json\",\n" +
                    "    \"files\": [\n" +
                    "      { \n" +
                    "        \"name\": \"ipfix-class-id.json\",\n" +
                    "        \"urlpath\": \"ipfix-application-information:class-id-dictionary\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}");

            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rmdir(final File folder) {
        // check if folder file is a real folder
        if (folder.isDirectory()) {
            File[] list = folder.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    File tmpF = list[i];
                    if (tmpF.isDirectory()) {
                        rmdir(tmpF);
                    }
                    tmpF.delete();
                }
            }
            if (!folder.delete()) {
                System.out.println("can't delete folder : " + folder);
            }
        }
    }

}
