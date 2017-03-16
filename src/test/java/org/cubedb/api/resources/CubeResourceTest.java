package org.cubedb.api.resources;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cubedb.api.resources.CubeResource;
import org.cubedb.api.utils.APIResponse;
import org.cubedb.core.MultiCube;
import org.cubedb.core.MultiCubeImpl;
import org.cubedb.core.beans.DataRow;
import org.cubedb.core.beans.Filter;
import org.cubedb.core.beans.GroupedSearchResultRow;
import org.cubedb.core.beans.SearchResult;
import org.cubedb.utils.TestUtils;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;

public class CubeResourceTest {
	private HttpServer httpServer;
	private WebTarget webTarget;
	private static final URI baseUri = URI.create("http://localhost:9090/rest/");

	public static final Logger log = LoggerFactory.getLogger(CubeResourceTest.class);

	public static class MultiCubeTest extends MultiCubeImpl {

		public MultiCubeTest(String savePath) {
			super(savePath);
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean hasCube(String cubeName) {
			return !cubeName.equals("invalid");
		}

		@Override
		public Map<GroupedSearchResultRow, Long> get(String cubeName, int lastNum, List<Filter> filters) {
			Map<GroupedSearchResultRow, Long> out = new HashMap<GroupedSearchResultRow, Long>();
			out.put(new GroupedSearchResultRow(SearchResult.FAKE_GROUP_FIELD_NAME, SearchResult.FAKE_GROUP_FIELD_VALUE,
					"fieldName", "fieldValue", "metricName"), 1l);
			return out;
		}

		@Override
		public Map<GroupedSearchResultRow, Long> get(String cubeName, int lastNum, List<Filter> filters,
				String groupedBy) {
			Map<GroupedSearchResultRow, Long> out = new HashMap<GroupedSearchResultRow, Long>();
			out.put(new GroupedSearchResultRow(SearchResult.FAKE_GROUP_FIELD_NAME, SearchResult.FAKE_GROUP_FIELD_VALUE,
					"fieldName", "fieldValue", "metricName"), 1l);
			return out;
		}
	}

	@Before
	public void setup() throws Exception {
		// create ResourceConfig from Resource class
		ResourceConfig rc = new ResourceConfig();
		MultiCube cube = new MultiCubeTest(null);
		rc.registerInstances(new CubeResource(cube));

		// create the Grizzly server instance
		httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
		// start the server
		httpServer.start();

		// configure client with the base URI path
		Client client = ClientBuilder.newClient();
		webTarget = client.target(baseUri);
	}

	@After
	public void tearDown() throws Exception {
		httpServer.shutdown();
	}

	@Test
	public void testGet() {
		String response = webTarget.path("v1/cubeName/last/120").queryParam("h", "1").request().get(String.class);// .get();
		APIResponse<Map<String, Map<String, Map<String, Long>>>> out = new Genson().deserialize(response,
				new GenericType<APIResponse<Map<String, Map<String, Map<String, Long>>>>>() {
				});
		assertTrue(out.response.size() == 1);
	}

	@Test
	public void testGetGrouped() {
		String response = webTarget.path("v1/cubeName/last/120/group_by/field_name").request().get(String.class);
		APIResponse<Map<String, Map<String, Map<String, Map<String, Long>>>>> out = new Genson()
				.deserialize(response,
						new GenericType<APIResponse<Map<String, Map<String, Map<String, Map<String, Long>>>>>>() {
						});
		assertTrue(out.response.size() == 1);
	}

	@Test
	public void testGetNonExistantCube() {
		Response r = webTarget.path("v1/invalid/last/120").request().get();
		assertEquals(404, r.getStatus());
	}

	@Test
	public void testInsert() {
		List<DataRow> data = TestUtils.genSimpleData("cubeName", "p", "f", "c", 100);
		Entity<List<DataRow>> entity = Entity.entity(data, MediaType.APPLICATION_JSON_TYPE);
		String r = webTarget.path("v1/insert").request().post(entity, String.class);

		APIResponse<Map<String, Integer>> out = new Genson().deserialize(r,
				new GenericType<APIResponse<Map<String, Integer>>>() {
				});
	}
}