package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cloudant.client.api.Changes;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.Response;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

public class ChangeNotificationsTest {
	
	private static final Log log = LogFactory.getLog(ChangeNotificationsTest.class);
	private static CloudantClient dbClient;
	private static Properties props ;
	private static Database db;

	

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties("cloudant.properties",log);
		dbClient = new CloudantClient(props.getProperty("cloudant.account"),
									  props.getProperty("cloudant.username"),
									  props.getProperty("cloudant.password"));
		
		db = dbClient.database("lightcouch-db-test", true);
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}
	
	@Test
	public void changes_normalFeed() {
		db.save(new Foo()); 

		ChangesResult changes = db.changes()
				.includeDocs(true)
				.limit(1)
				.getChanges();
		
		List<ChangesResult.Row> rows = changes.getResults();
		
		for (Row row : rows) {
			List<ChangesResult.Row.Rev> revs = row.getChanges();
			String docId = row.getId();
			JsonObject doc = row.getDoc();
			
			assertNotNull(revs);
			assertNotNull(docId);
			assertNotNull(doc);
		}
		
		assertThat(rows.size(), is(1));
	}

	@Test
	public void changes_continuousFeed() {
		db.save(new Foo()); 

		DbInfo dbInfo = db.info();
		String since = dbInfo.getUpdateSeq();

		Changes changes = db.changes()
				.includeDocs(true)
				.since(since)
				.heartBeat(30000)
				.continuousChanges();

		Response response = db.save(new Foo());

		while (changes.hasNext()) {
			ChangesResult.Row feed = changes.next();
			final JsonObject feedObject = feed.getDoc();
			final String docId = feed.getId();
			
			assertEquals(response.getId(), docId);
			assertNotNull(feedObject);
			
			changes.stop();
		}
	}
}
