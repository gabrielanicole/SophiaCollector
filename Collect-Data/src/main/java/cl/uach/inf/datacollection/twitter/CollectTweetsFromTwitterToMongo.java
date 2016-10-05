package cl.uach.inf.datacollection.twitter;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class CollectTweetsFromTwitterToMongo extends Thread{

	/** Parametros en relación con la autentificación en Twitter*/
	final String PARAM_TOKEN = "2177040169-5aVazrfgCpjhDOgemcIw1PvZXb3nbHtglUuAOcf";
	final String PARAM_SECRET = "iCVFeVZkhSC3hsJfISbLDbpZZAyUS2Grn6ZJUNABaWmrt";
	final String PARAM_CONSUMERKEY = "ac1yMlhXpxDAjzJWwmzagg";
	final String PARAM_CONSUMERSECRET = "wpMMIXxkZ3ChqANkdVkzMH0wMdb8nKMqIVaztIEwtw";
	final String PARAM_USERNAME = "sophia";
	final String PARAM_PASSWORD = "kelluwen";
	final int PARAM_WAITING_TIME=300000; //5 minutos
	final String PARAM_TWITTERAPI_URL_TIMELINE = "https://api.twitter.com/1.1/statuses/home_timeline.json";
	final String PARAM_NBRESULT="?count=200";


	/** Paremetros en relación con el uso de Mongo para almacenar temporalmente los tweets */
	final private MongoClient mongoClient;
	final private MongoDatabase mongoDatabase;
	final private MongoCollection<Document> mongoCollection;
	final private String databaseName ="SophiaCollector";
	final private String collectionName ="Tweets";

	/** Variables privadas */
	private HttpGet request;
	private CloseableHttpClient client;
	private CloseableHttpResponse response;
	private OAuthConsumer consumer;


	//FIXME
	private long getLastTweetIdInMongo(){
		if (mongoCollection.count()>0){
			FindIterable<Document> docCursor= mongoCollection.find();
			docCursor.sort(new BasicDBObject("id",-1));
			Document doc = docCursor.iterator().next();
			return (long) doc.get("id");
		}
		else return 0;
	}

	private JSONArray getLastTweets(){
		JSONArray dataJSONArray = new JSONArray();
		try {
		// Execución de la consulta HTTP por el cliente
		response = client.execute(request);
		// Recopilación de la respuesta de la consulta en un objeto JSON
		dataJSONArray = new JSONArray(EntityUtils.toString(response.getEntity()));
		// Cierre de la conexión HTTP entre el cliente y el servidor
		response.close();
		}
		catch (Exception e1){
			System.out.println(e1);
		}
		return dataJSONArray;
	}

	public void run(){
		while(true)
		{
			JSONArray lastTweets = getLastTweets();
			long lastTweetIdInMongo = getLastTweetIdInMongo();
			int resultCount = lastTweets.length();
			for (int i = 0; i < resultCount; i++)
			{
				JSONObject currentTweet =  lastTweets.getJSONObject(i);
				long current_id = currentTweet.getLong("id");
				// nuevo max ID ?
				if (current_id>lastTweetIdInMongo){
					Document doc = Document.parse(currentTweet.toString());
					//add a flag to indicate that we still do not have downloaded the article related to this tweet
					doc.append("to_download", 1);
					mongoCollection.insertOne(doc);
				}
			}
			//esperar un momento antes de consultar Twitter de nuevo
			try {
				Thread.sleep(PARAM_WAITING_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public CollectTweetsFromTwitterToMongo(String name) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException{
		super(name);		
		//Conexión al SGBD Mongo
		mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		//Conexión a una collecion de una base de datos particular
		mongoDatabase = mongoClient.getDatabase(databaseName);
		mongoCollection = mongoDatabase.getCollection(collectionName);
		/* Configurácion de la autentificación con el protoco OAuth*/
		consumer = new CommonsHttpOAuthConsumer(PARAM_CONSUMERKEY,PARAM_CONSUMERSECRET);
		consumer.setTokenWithSecret(PARAM_TOKEN, PARAM_SECRET);
		/* Creación de una consulta HTTP (metodo GET) hacia una fuente de datos Twitter*/ 
		request = new HttpGet(PARAM_TWITTERAPI_URL_TIMELINE+PARAM_NBRESULT);
		// Autentificación de la consulta HTTP
		consumer.sign(request);
		// Creación de un cliente HTTP
		client = HttpClients.createDefault();
	}
}
