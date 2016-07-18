/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.sfc.tacker.dto.Attributes;
import org.opendaylight.sfc.tacker.dto.Auth;
import org.opendaylight.sfc.tacker.dto.Error;
import org.opendaylight.sfc.tacker.dto.KeystoneRequest;
import org.opendaylight.sfc.tacker.dto.PasswordCredentials;
import org.opendaylight.sfc.tacker.dto.TackerError;
import org.opendaylight.sfc.tacker.dto.TackerRequest;
import org.opendaylight.sfc.tacker.dto.TackerResponse;
import org.opendaylight.sfc.tacker.dto.Tenant;
import org.opendaylight.sfc.tacker.dto.Token;
import org.opendaylight.sfc.tacker.dto.Vnf;
import org.opendaylight.sfc.tacker.util.DateSerializer;
import org.opendaylight.sfc.tacker.util.DateUtils;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SftTypeName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunctionBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TackerManagerTest extends JerseyTest {

    private static final Logger LOG = LoggerFactory.getLogger(TackerManagerTest.class);
    private static final String BASE_URI = "http://localhost";
    private static final int BASE_PORT = 1234;
    private static final int KEYSTONE_PORT = 4321;
    private static final DateSerializer DATE_SERIALIZER = new DateSerializer();
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, DATE_SERIALIZER).create();
    private static final List<String> vnfs = new ArrayList<>();
    private static TackerManager tackerManager;
    private static HttpServer server;
    private static HttpServer keystoneServer;
    private static TackerResponse tackerResponse;
    private static TackerError badRequestError;
    private static TackerError notFoundError;
    private static Token token;

    private static HttpServer startServer() {
        final ResourceConfig resourceConfig = new ClassNamesResourceConfig(tackerServer.class);
        HttpServer httpServer = null;
        try {
            httpServer = GrizzlyServerFactory.createHttpServer(URI.create(BASE_URI + ":" + BASE_PORT), resourceConfig);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.debug(e.getMessage());
        }
        return httpServer;
    }

    private static HttpServer startKeystoneServer() {
        final ResourceConfig resourceConfig = new ClassNamesResourceConfig(keystoneServer.class);
        HttpServer httpServer = null;
        try {
            httpServer =
                    GrizzlyServerFactory.createHttpServer(URI.create(BASE_URI + ":" + KEYSTONE_PORT), resourceConfig);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.debug(e.getMessage());
        }
        return httpServer;
    }

    @BeforeClass
    public static void setUpClass() {
        server = startServer();
        keystoneServer = startKeystoneServer();

        tackerManager = TackerManager.builder()
            .setBaseUri(BASE_URI)
            .setTackerPort(BASE_PORT)
            .setKeystonePort(KEYSTONE_PORT)
            .setAuth(Auth.builder()
                .setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("admin", "devstack"))
                .build())
            .build();

        tackerResponse = TackerResponse.builder()
            .setVnf(Vnf.builder()
                .setStatus("PENDING_CREATE")
                .setName("")
                .setTenantId("4dd6c1d7b6c94af980ca886495bcfed0")
                .setDescription("OpenWRT with services")
                .setInstanceId("4f0d6222-afa0-4f02-8e19-69e7e4fd7edc")
                .setMgmtUrl(null)
                .setAttributes(Attributes.builder()
                    .setServiceType("firewall")
                    .setHeatTemplate("description: OpenWRT with services\n"
                            + "                <sample_heat_template> type: OS::Nova::Server\n")
                    .setMonitoringPolicy("noop")
                    .setFailurePolicy("noop")
                    .build())
                .setId("e3158513-92f4-4587-b949-70ad0bcbb2dd")
                .setVnfdId("247b045e-d64f-4ae0-a3b4-8441b9e5892c")
                .build())
            .build();

        badRequestError = new TackerError(Error.builder()
            .setType("BadRequest")
            .setDetail("Request not processed, wrong data.")
            .setMessage("Bad Request")
            .build());

        notFoundError = new TackerError(Error.builder()
            .setType("NotFound")
            .setDetail("The resource could not be found.")
            .setMessage("Not Found")
            .build());
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null && server.isStarted())
            server.shutdownNow();
        if (keystoneServer != null && keystoneServer.isStarted())
            keystoneServer.shutdownNow();
    }

    @Test
    public void createSfTest() {
        // create new vnf
        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftTypeName("firewall")).build();
        boolean result = tackerManager.createSf(sfType);
        LOG.debug("Create vnf passed: " + result);
        Assert.assertTrue(result);

        // now delete the created vnf
        ServiceFunction sf = new ServiceFunctionBuilder().setName(new SfName(sfType.getType().getValue()))
            .setType(sfType.getType())
            .build();
        result = tackerManager.deleteSf(sf);

        LOG.debug("Delete vnf passed: " + result);
        Assert.assertTrue(result);
    }

    @Test
    public void createSfTestAuthFail() {
        // authenticate with wrong username/password
        TackerManager tackerManager = TackerManager.builder()
            .setBaseUri(BASE_URI)
            .setTackerPort(BASE_PORT)
            .setKeystonePort(KEYSTONE_PORT)
            .setAuth(Auth.builder()
                .setTenantName("admin")
                .setPasswordCredentials(new PasswordCredentials("user", "password"))
                .build())
            .build();

        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftTypeName("firewall")).build();
        boolean result = tackerManager.createSf(sfType);
        LOG.debug("createSfTestAuthFail passed: " + !result);
        Assert.assertFalse(result);
    }

    @Test
    public void createSfTestEmptyNameFail() {
        ServiceFunctionType sfType = new ServiceFunctionTypeBuilder().setType(new SftTypeName("")).build();
        boolean result = tackerManager.createSf(sfType);
        LOG.debug("createSfTestEmptyNameFail passed: " + !result);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSfTest() {
        // manualy add one vnf to list for deletion
        vnfs.add("Dpi");

        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName("Dpi")).setType(new SftTypeName("firewall")).build();
        boolean result = tackerManager.deleteSf(sf);
        Assert.assertTrue(result);
    }

    @Test
    public void deleteSfTestNotFound() {
        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName("Nope")).setType(new SftTypeName("nope")).build();
        boolean result = tackerManager.deleteSf(sf);
        Assert.assertFalse(result);
    }

    @Test
    public void deleteSfTestBadRequest() {
        ServiceFunction sf =
                new ServiceFunctionBuilder().setName(new SfName(" ")).setType(new SftTypeName("error")).build();
        Assert.assertFalse(tackerManager.deleteSf(sf));
        sf = new ServiceFunctionBuilder().setName(new SfName("")).setType(new SftTypeName("error")).build();
        Assert.assertFalse(tackerManager.deleteSf(sf));
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
        public Response postVnf(@HeaderParam("X-Auth-Token") String authToken,
                @HeaderParam("X-Auth-Project-Id") String authProject, String json) {
            if (authToken == null || authProject == null || authToken.isEmpty() || authProject.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(GSON.toJson(badRequestError))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();

            if ((!authToken.equals(token.getId())) || (!authProject.equals(token.getTenant().getName()))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
            }

            TackerRequest testRequest = GSON.fromJson(json, TackerRequest.class);

            if (testRequest.getVnf().getName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(GSON.toJson(badRequestError)).build();
            }

            vnfs.add(testRequest.getVnf().getName());
            return Response.status(Response.Status.CREATED).entity(GSON.toJson(tackerResponse)).build();
        }

        @DELETE
        @Path("/{vnf_id}")
        public Response deleteVnf(@HeaderParam("X-Auth-Token") String authToken,
                @HeaderParam("X-Auth-Project-Id") String authProject,
                @PathParam("vnf_id") @DefaultValue("") String vnf_id) {

            if (authToken == null || authProject == null || authToken.isEmpty() || authProject.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(GSON.toJson(badRequestError))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();

            if ((!authToken.equals(token.getId())) || (!authProject.equals(token.getTenant().getName()))) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
            }

            if (vnf_id.equals(" ")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(GSON.toJson(badRequestError))
                    .build();
            }

            if (vnfs.remove(vnf_id)) {
                return Response.status(Response.Status.OK)
                    .entity("resource " + vnf_id + " successfully deleted.")
                    .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(GSON.toJson(notFoundError))
                    .build();
            }
        }
    }

    @Path("/v2.0/tokens")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public static class keystoneServer {

        @POST
        public Response postVnf(String json) {
            KeystoneRequest testRequest = GSON.fromJson(json, KeystoneRequest.class);
            if (testRequest.getAuth().getTenantName().equals("admin")
                    && testRequest.getAuth().getPasswordCredentials().getUsername().equals("admin")
                    && testRequest.getAuth().getPasswordCredentials().getPassword().equals("devstack")) {
                SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

                Date now = DateUtils.getUtcDate(new Date());
                Date expire = DateUtils.addHours(new Date(now.getTime()), 1);

                token = Token.builder()
                    .setIssued_at(now)
                    .setExpires(expire)
                    .setId("7a17dc67ba284ab2beeccc21ce198626")
                    .setTenant(Tenant.builder().setDesription(null).setEnabled(true).setId("").setName("admin").build())
                    .setAudit_ids(new String[] {"LUMVW2kmQU29kwkZv8VCZg"})
                    .build();

                String response = "{\"access\":{\"token\":" + GSON.toJson(token) + "}}";

                return Response.status(Response.Status.OK)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(response)
                    .build();
            }
            return Response.status(Response.Status.UNAUTHORIZED).entity("ok").build();
        }
    }
}
