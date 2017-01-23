package cl.uach.inf.sophia.datacollection.twitter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.protocol.HttpClientContext;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.exceptions.UnirestException;
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
	final private String databaseName ="SophiaCollectorNew";
	final private String collectionName ="Tweets";
	final int PARAM_WAITING_TIME=60000; //1 minuto
	final int PARAM_DOWNLOAD_AND_WAIT = 50;

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
		String contenido = article.select("p").text();
		if(contenido.length()>1){
			map.put("art_content",contenido );
		}else{
			map.put("art_content","articulo no posee contenido");
		}
		
		map.put("art_image_link", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/No_image_available.svg/600px-No_image_available.svg.png");
		Document entities = (Document)tweet.get("entities");
		if (entities!=null){
			if (entities.get("media")!=null){
				ArrayList<Document> medias = (ArrayList<Document>) entities.get("media");
				map.put("art_image_link", medias.get(0).getString("media_url"));//---> media_url
			}
		}
		map.put("art_name_press_source", ((Document)tweet.get("user")).getString("screen_name"));

		map.put("art_category", "unclassified");

		return map;
	}

	public Map<String,Object> formatPublication(Document tweet){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("pub_content", tweet.getString("text"));
		String dateWeWant="1900-01-01 00:00:00";
		try {
			Date dateTweet = dateFormatWeHave.parse(tweet.getString("created_at"));
			dateWeWant = dateFormatWeWant.format(dateTweet);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		map.put("pub_date",dateWeWant);
		map.put("pub_url", "https://twitter.com/"+((Document)tweet.get("user")).getString("screen_name")+"/status/"+
		 tweet.getString("id_str"));
		
		map.put("pub_username", ((Document)tweet.get("user")).getString("screen_name"));

		return map;
	}

	//Download and scrap an html page from a tweet containing an URL
	public org.jsoup.nodes.Document downloadArticle(Document tweet) throws IOException{
		
		ArrayList<Document> urls = (ArrayList<Document>) ((Document)tweet.get("entities")).get("urls");
		//array with user-agents
		String[] userAgents = {"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0",
				"Mozilla/5.0 (compatible; ABrowse 0.4; Syllable)",
				"Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; Acoo Browser 1.98.744; .NET CLR 3.5.30729)",
				"Mozilla/4.0 (compatible; MSIE 7.0; America Online Browser 1.1; Windows NT 5.1; (R1 1.5); .NET CLR 2.0.50727; InfoPath.1)",
				"AmigaVoyager/3.2 (AmigaOS/MC680x0)",
				"Mozilla/5.0 (compatible; MSIE 9.0; AOL 9.7; AOLBuild 4343.19; Windows NT 6.1; WOW64; Trident/5.0; FunWebProducts)",
				"Mozilla/5.0 (X11; U; UNICOS lcLinux; en-US) Gecko/20140730 (KHTML, like Gecko, Safari/419.3) Arora/0.8.0",
				"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; Avant Browser; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0)",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
				"Opera/9.80 (X11; Linux i686; Ubuntu/14.10) Presto/2.12.388 Version/12.16",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A"};
		int index = ThreadLocalRandom.current().nextInt(0, 11);
		//Scrap it con JSoup
		return Jsoup.connect(urls.get(0).getString("url"))
				.userAgent(userAgents[index]) 
				.get();

	}

	public void run(){
		try {
			while(true){
				//System.out.println("Bienvenido a depurar JAVA en CollectArticlesFromMongoToSophia");
				//Read the mongo database to find new tweets
				FindIterable<Document> docCursor = mongoCollection.find(new BasicDBObject("to_download", 1)).noCursorTimeout(true);
				//System.out.println(docCursor);
				long numberResults=mongoCollection.count(eq("to_download", 1));
				//System.out.println(numberResults);
				if (numberResults>0){
					//There is new tweets
					Iterator<Document> itTweets = docCursor.iterator();
					int counterDownload = 0;
					//System.out.println(itTweets);
					while (itTweets.hasNext()){
						//Take the next tweet to download
						Document tweet = itTweets.next();
						// Check if the tweet contains an URL
						if (tweetHasURL(tweet)){
							counterDownload = counterDownload + 1;
							if(counterDownload == PARAM_DOWNLOAD_AND_WAIT){
								counterDownload = 0;
								System.out.println("CollectTweetsFromMongoToSophia-1-Sleep");
								Thread.sleep(PARAM_WAITING_TIME);
							}
							try {
								//sleep random time for scrapping
								int RANDOM_WAITING_TIME = ThreadLocalRandom.current().nextInt(1000, 5000);
								Thread.sleep(RANDOM_WAITING_TIME);
								//This tweet contains URL, download it
								org.jsoup.nodes.Document article=downloadArticle(tweet);
								//Format the article to prepare the POST to SophiaAPI
								Map<String, Object> mapArticle = format(article,tweet);

								/**VERIFICAR SI EL ARTICULO YA EXISTE EN SOPHIA API*/
								JSONObject jsonResponse = sophiaAPI.hasExistingArticle(mapArticle);
								System.out.println(jsonResponse);
								String id = jsonResponse.getString("_id");

								if (id.equals("0")){
									/** EL ARTICULO NO EXISTE*/
									//enviamos el nuevo articulo
									String idNewArticle = sophiaAPI.postArticles(mapArticle);

									//enviamos el nuevo tweet, formatando el tweet antes
									Map<String, Object> mapPublication = formatPublication(tweet);
									mapPublication.put("pub_article", idNewArticle);
									String idNewPublication = sophiaAPI.postPublications(mapPublication);

									//actualizamos el articulo con el id de la publicacion
								/*	Map<String, Object> updateArticle = new HashMap<String,Object>();
									ArrayList<String> listPublications = new ArrayList<String>();
									listPublications.add(idNewPublication);
									updateArticle.put("art_publications", listPublications);
									sophiaAPI.putArticles(updateArticle,idNewArticle);*/

								}
								else {
									/** EL ARTICULO EXISTE*/
									//enviamos el nuevo tweet, formatando el tweet antes
									Map<String, Object> mapPublication = formatPublication(tweet);
									mapPublication.put("pub_article", id);
									String idNewPublication = sophiaAPI.postPublications(mapPublication);
									mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", 0)));
									//Actualizamos el articulo antiguo agregando el id de la nueva publicacion
								/*	JSONArray publicationsArray = jsonResponse.getJSONArray("art_publications");
									Iterator<Object> publications = publicationsArray.iterator();
									ArrayList<String> listPublications = new ArrayList<String>();
									while (publications.hasNext()){
										listPublications.add(publications.next().toString());
									}
									listPublications.add(idNewPublication);
									Map<String, Object> updateArticle = new HashMap<String,Object>();
									updateArticle.put("art_publications", listPublications);
									sophiaAPI.putArticles(updateArticle,id);*/
								}

								//Informamos a Mongo que terminamos procesar este tweet
								mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", 0)));
							}
							catch (IOException e){
								mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", -1)));
								e.printStackTrace();
					
							}
						}
						else {
							//This tweet does not contain URL, tell to Mongo that this tweet has been processed
							mongoCollection.updateOne(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", 0)));
						}
					}
				}
				else {
					//If there is no new entries, wait a moment...
					System.out.println("CollectTweetsFromMongoToSophia-1-Sleep");
					Thread.sleep(PARAM_WAITING_TIME);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (UnirestException e) {
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
	}
}
