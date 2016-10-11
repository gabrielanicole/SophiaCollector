package cl.uach.inf.sophia.datacollection.twitter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

import org.jsoup.Jsoup;

public class CollectArticlesFromMongoToSophia extends Thread{

	/** Paremetros en relación con el uso de Mongo para almacenar temporalmente los tweets */
	final private MongoClient mongoClient;
	final private MongoDatabase mongoDatabase;
	final private MongoCollection<Document> mongoCollection;
	final private String databaseName ="SophiaCollector";
	final private String collectionName ="Tweets";
	final int PARAM_WAITING_TIME=120000; //2 minutos
	/** Parametros en relación con la fuente de datos API REST y el intervalo para conectarse **/
	final String PARAM_SOPHIAAPI = "http://api.sophia-project.info/v2/articles/";
	final String PARAM_USERNAME = "sophia";
	final String PARAM_PASSWORD = "kelluwen";

	/** Variables privadas */
	private HttpGet requestGet;
	private HttpPut requestPut;
	private HttpPost requestPost;
	private CloseableHttpClient client;
	private CloseableHttpResponse response;
	SimpleDateFormat dateFormatWeWant = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat dateFormatWeHave = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);


	public boolean alreadyExistInSophia(org.jsoup.nodes.Document article){
		return false;
	}

	/*return 0 if SophiaAPI received everything, return 1 if not*/
	public int sendToSophiaAPI(org.jsoup.nodes.Document article, Document tweet){
		//FIXME:For the moment, it only sends the article to SophiaAPI. It should also send the publication and check if the article already exists before.
		/**Queries to SophiaAPI**/
		//Call SophiaAPI/Articles
		JSONObject jsonArticle = new JSONObject();
		jsonArticle.put("art_url", article.location());
		String dateWeWant="1900-01-01 00:00:00";
		try {
			Date dateTweet = dateFormatWeHave.parse(tweet.getString("created_at"));
			dateWeWant = dateFormatWeWant.format(dateTweet);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		jsonArticle.put("art_date",dateWeWant);
		jsonArticle.put("art_title", article.title());
		jsonArticle.put("art_content", article.select("p").text());
		jsonArticle.put("art_image_link", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/No_image_available.svg/600px-No_image_available.svg.png");
		Document entities = (Document)tweet.get("entities");
		if (entities!=null){
			if (entities.get("media")!=null){
				ArrayList<Document> medias = (ArrayList<Document>) entities.get("media");
				jsonArticle.put("art_image_link", medias.get(0).getString("media_url"));//---> media_url
			}
		}
		jsonArticle.put("art_name_press_source", ((Document)tweet.get("user")).getString("screen_name"));
		
		ArrayList<Long> list = new ArrayList<Long>();
		list.add(tweet.getLong("id"));
		//jsonArticle.put("art_publications", list); //For the moment : a unique tweet ID
		jsonArticle.put("art_category", "unclassified");

		CloseableHttpClient httpClient = HttpClientBuilder.create().build();

		try {
			HttpPost request = new HttpPost(PARAM_SOPHIAAPI);
			StringEntity params = new StringEntity(jsonArticle.toString());
			request.setHeader("Accept", "application/json");
			request.setHeader("content-type", "application/json");
			request.setEntity(params);
			//CloseableHttpResponse response = httpClient.execute(request);
			System.out.println(jsonArticle);
			// handle response here...
			//System.out.println("response SophiaAPI"+response.getStatusLine()+EntityUtils.toString(response.getEntity()));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return 0;
	}


	public void run(){
		while(true){
			//Read the mongo database to find new entries to download and send to Sophia
			FindIterable<Document> docCursor = mongoCollection.find(new BasicDBObject("to_download", 1));
			long numberResults=mongoCollection.count(eq("to_download", 1));

			if (numberResults>0){
				//There is new articles to download
				Iterator<Document> it = docCursor.iterator();
				while (it.hasNext()){
					//Take the next article to download
					Document newEntry = it.next();
					//Check if the new entry contain an URL
					if (newEntry.get("entities")!=null){
						if (((Document)newEntry.get("entities")).get("urls")!=null){
							ArrayList<Document> urls = (ArrayList<Document>) ((Document)newEntry.get("entities")).get("urls");
							if (urls.size()>0){
								//Take the first url and download the corresponding page
								try {
									org.jsoup.nodes.Document article = Jsoup.connect(urls.get(0).getString("url")).get();
									int flag = sendToSophiaAPI(article,newEntry);
									//Article was downloaded and send to SophiaAPI successfully
									mongoCollection.updateOne(new Document("id",newEntry.get("id")),new Document("$set", new Document("to_download", flag)));
								} catch (IOException e) {
									//In case there is a problem during article scraping: actualize Mongo field with -1 to indicate there is an error
									mongoCollection.updateOne(new Document("id",newEntry.get("id")),new Document("$set", new Document("to_download", -1)));
								}
							}
						}
					}
				}
			}
			else {
				//If there is no new entries, wait a moment...
				try {
					Thread.sleep(PARAM_WAITING_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public CollectArticlesFromMongoToSophia(String name){
		super(name);		
		//Conexión al SGBD Mongo
		mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		//Conexión a una collecion de una base de datos particular
		mongoDatabase = mongoClient.getDatabase(databaseName);
		mongoCollection = mongoDatabase.getCollection(collectionName);

		/* Creación de una consulta HTTP (metodo GET) */ 
		requestGet = new HttpGet(PARAM_SOPHIAAPI);
		requestGet.addHeader(BasicScheme.authenticate(
				new UsernamePasswordCredentials(PARAM_USERNAME, PARAM_PASSWORD),
				"UTF-8", false));

		/* Creación de una consulta HTTP (metodo Put) */ 
		requestPut = new HttpPut(PARAM_SOPHIAAPI);
		requestPut.addHeader(BasicScheme.authenticate(
				new UsernamePasswordCredentials(PARAM_USERNAME, PARAM_PASSWORD),
				"UTF-8", false));

		/* Creación de una consulta HTTP (metodo Post) */ 
		requestPost = new HttpPost(PARAM_SOPHIAAPI);
		requestPost.addHeader(BasicScheme.authenticate(
				new UsernamePasswordCredentials(PARAM_USERNAME, PARAM_PASSWORD),
				"UTF-8", false));

		// Creación de un cliente HTTP
		client = HttpClients.createDefault();
	}
}
