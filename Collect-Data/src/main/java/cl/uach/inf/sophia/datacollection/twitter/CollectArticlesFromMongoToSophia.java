package cl.uach.inf.sophia.datacollection.twitter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.protocol.HttpClientContext;
import org.bson.Document;
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
	SimpleDateFormat dateFormatWeWant = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat dateFormatWeHave = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

	private SophiaAPIConnector sophiaAPI;
	private HttpClientContext httpClientContext;

	public boolean tweetHasURL(Document tweet){
		if (tweet.get("entities")!=null){
			if (((Document)tweet.get("entities")).get("urls")!=null){
				if (((ArrayList<Document>) ((Document)tweet.get("entities")).get("urls")).size()>0){
					return true;
				}
			}
		}
		return false;
	}

	public Map<String,Object> format(org.jsoup.nodes.Document article, Document tweet){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("art_url", article.location());
		String dateWeWant="1900-01-01 00:00:00";
		try {
			Date dateTweet = dateFormatWeHave.parse(tweet.getString("created_at"));
			dateWeWant = dateFormatWeWant.format(dateTweet);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		map.put("art_date",dateWeWant);
		map.put("art_title", article.title());
		map.put("art_content", article.select("p").text());
		map.put("art_image_link", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/No_image_available.svg/600px-No_image_available.svg.png");
		Document entities = (Document)tweet.get("entities");
		if (entities!=null){
			if (entities.get("media")!=null){
				ArrayList<Document> medias = (ArrayList<Document>) entities.get("media");
				map.put("art_image_link", medias.get(0).getString("media_url"));//---> media_url
			}
		}
		map.put("art_name_press_source", ((Document)tweet.get("user")).getString("screen_name"));

		ArrayList<Long> list = new ArrayList<Long>();
		list.add(tweet.getLong("id"));
		//jsonArticle.put("art_publications", list); //For the moment : a unique tweet ID
		map.put("art_category", "unclassified");

		return map;
	}

	public void run(){
		try {
			while(true){
				//Read the mongo database to find new entries to download and send to Sophia
				FindIterable<Document> docCursor = mongoCollection.find(new BasicDBObject("to_download", 1));
				long numberResults=mongoCollection.count(eq("to_download", 1));
				if (numberResults>0){
					//There is new articles to download
					Iterator<Document> it = docCursor.iterator();
					while (it.hasNext()){
						//Take the next article to download
						Document tweet = it.next();
						if (tweetHasURL(tweet)){
							try {
								//This tweet contains URL, download it
								ArrayList<Document> urls = (ArrayList<Document>) ((Document)tweet.get("entities")).get("urls");
								//Scrap it con JSoup

								org.jsoup.nodes.Document article = Jsoup.connect(urls.get(0).getString("url")).get();


								//Check if the article already exist
								//TODO
								//Format it to prepare the POST to SophiaAPI
								Map<String, Object> map = format(article,tweet);
								int responseCode = sophiaAPI.postArticles(map);
								if (responseCode == 200){
									mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", 0)));
								}
								else {
									mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", -1)));
								}
							}
							catch (Exception e){
								System.out.println(e);
								mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", -1)));
							}
						}
						else {
							//This tweet does not contain URL, tell to Mongo that this tweet has been processed
							mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", -1)));
						}
					}
				}
				else {
					//If there is no new entries, wait a moment...
					System.out.println("CollectTweetsFromMongoToSophia-1-Sleep");
					Thread.sleep(PARAM_WAITING_TIME);
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public CollectArticlesFromMongoToSophia(String name){
		super(name);		
		//Conexión al SGBD Mongo
		mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		//Conexión a una collecion de una base de datos particular
		mongoDatabase = mongoClient.getDatabase(databaseName);
		mongoCollection = mongoDatabase.getCollection(collectionName);

		sophiaAPI = new SophiaAPIConnector();

		/* Creación de una consulta HTTP (metodo Post) */ 
		//requestPost = new HttpPost(PARAM_SOPHIAAPI);
		//requestPost.addHeader(BasicScheme.authenticate(
		//	new UsernamePasswordCredentials(PARAM_USERNAME, PARAM_PASSWORD),
		//	"UTF-8", false));
		// Creación de un cliente HTTP
		//client = HttpClients.createDefault();
	}
}
