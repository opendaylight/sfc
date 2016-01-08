/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.api;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.glassfish.grizzly.http.server.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sfc.tacker.dto.*;
import org.opendaylight.sfc.tacker.dto.Error;
import org.opendaylight.sfc.tacker.dto.TackerRequest;
import org.opendaylight.sfc.tacker.dto.TackerResponse;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SfcVnfManagerProviderTest extends JerseyTest {

    private static final Logger LOG = LoggerFactory.getLogger(SfcVnfManagerProviderTest.class);
    private static final String BASE_URI = "http://localhost:1234/tackerserver/";
    private static final List<String> vnfs = new ArrayList<>();
    private static SfcVnfManagerProvider sfcVnfManagerProvider;
    private static HttpServer server;
    private static TackerResponse tackerResponse;
    private static TackerError badRequestError;
    private static TackerError notFoundError;

    private static HttpServer startServer() {
        final ResourceConfig resourceConfig = new ClassNamesResourceConfig(tackerServer.class);
        HttpServer httpServer = null;
        try {
            httpServer = GrizzlyServerFactory.createHttpServer(URI.create(BASE_URI), resourceConfig);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.debug(e.getMessage());
        }
        return httpServer;
    }

    @BeforeClass
    public static void setUpClass() {
        server = startServer();

        sfcVnfManagerProvider = new SfcVnfManagerProvider.SfcVnfManagerProviderBuilder().setBaseUri(BASE_URI)
            .setAuth(Auth.builder()
                .setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("admin", "devstack"))
                .build())
            .build();

        tackerResponse = TackerResponse.builder()
            .setVnf(Vnf.builder()
                .setStatus("PENDING_CREATE")
                .setName("")
                .setTenant_id("4dd6c1d7b6c94af980ca886495bcfed0")
                .setDescription("OpenWRT with services")
                .setInstance_id("4f0d6222-afa0-4f02-8e19-69e7e4fd7edc")
                .setMgmt_url(null)
                .setAttributes(Attributes.builder()
                    .setService_type("firewall")
                    .setHeat_template("description: OpenWRT with services\n"
                            + "                <sample_heat_template> type: OS::Nova::Server\n")
                    .setMonitoring_policy("noop")
                    .setFailure_policy("noop")
                    .build())
                .setId("e3158513-92f4-4587-b949-70ad0bcbb2dd")
                .setVnfd_id("247b045e-d64f-4ae0-a3b4-8441b9e5892c")
                .build())
            .build();

        badRequestError = new TackerError(Error.builder()
            .setCode(Response.Status.BAD_REQUEST.getStatusCode())
            .setTitle("Bad Request")
            .setMessage("Request not processed, wrong data.")
            .build());

        notFoundError = new TackerError(Error.builder()
            .setCode(Response.Status.NOT_FOUND.getStatusCode())
            .setTitle("Not Found")
            .setMessage("The resource could not be found.")
            .build());
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null && server.isStarted())
            server.shutdownNow();
    }

    @Test
    public void createSfTest() {
        // create new vnf
        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftType("firewall")).build();
        boolean result = sfcVnfManagerProvider.createSf(sfType);
        LOG.debug("Create vnf passed: " + result);
        Assert.assertTrue(result);

        // now delete the created vnf
        ServiceFunction sf = new ServiceFunctionBuilder().setName(new SfName(sfType.getType().getValue()))
            .setType(sfType.getType())
            .build();
        result = sfcVnfManagerProvider.deleteSf(sf);

        LOG.debug("Delete vnf passed: " + result);
        Assert.assertTrue(result);
    }

    @Test
    public void createSfTestAuthFail() {
        // authenticate with wrong username/password
        SfcVnfManagerProvider sfcVnfManagerProvider = new SfcVnfManagerProvider.SfcVnfManagerProviderBuilder()
            .setBaseUri(BASE_URI)
            .setAuth(Auth.builder()
                .setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("user", "password"))
                .build())
            .build();
        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftType("firewall")).build();
        boolean result = sfcVnfManagerProvider.createSf(sfType);
        LOG.debug("createSfTestAuthFail passed: " + !result);
        Assert.assertFalse(result);
    }

    @Test
    public void createSfTestEmptyNameFail() {
        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftType("")).build();
        boolean result = sfcVnfManagerProvider.createSf(sfType);
        LOG.debug("createSfTestEmptyNameFail passed: " + !result);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSfTest() {
        // manualy add one vnf to list for deletion
        vnfs.add("Dpi");

        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName("Dpi")).setType(new SftType("firewall")).build();
        boolean result = sfcVnfManagerProvider.deleteSf(sf);
        Assert.assertTrue(result);
    }

    @Test
    public void deleteSfTestNotFound() {
        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName("Nope")).setType(new SftType("nope")).build();
        boolean result = sfcVnfManagerProvider.deleteSf(sf);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSfTestBadRequest() {
        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName(" ")).setType(new SftType("error")).build();
        Assert.assertFalse(sfcVnfManagerProvider.deleteSf(sf));
        sf = new ServiceFunctionBuilder().setName(new SfName("")).setType(new SftType("error")).build();
        Assert.assertFalse(sfcVnfManagerProvider.deleteSf(sf));
    }

    @Override
    protected AppDescriptor configure() {
        return new WebAppDescriptor.Builder().build();
    }

    @Path("/v1.0/vnfs")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class tackerServer {

        @POST
        public Response postVnf(String json) {
            TackerRequest testRequest = new TackerRequest(json);
            if (!(testRequest.getAuth().getPasswordCredentials().getUsername().equals("admin")
                    || testRequest.getAuth().getPasswordCredentials().getPassword().equals("devstack"))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("unauthorized").build();
            } else if (testRequest.getVnf().getName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(badRequestError.toJson())
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
            }
            vnfs.add(testRequest.getVnf().getName());
            return Response.status(Response.Status.CREATED).entity(tackerResponse.toJson()).build();
        }

        @DELETE
        @Path("/{vnf_id}")
        public Response deleteVnf(@PathParam("vnf_id") @DefaultValue("") String vnf_id) {

            if (vnf_id.equals(" ")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(badRequestError.toJson())
                    .build();
            }

            if (vnfs.remove(vnf_id)) {
                return Response.status(Response.Status.OK)
                    .entity("resource " + vnf_id + " successfully deleted.")
                    .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(notFoundError.toJson())
                    .build();
            }
        }
    }
}
