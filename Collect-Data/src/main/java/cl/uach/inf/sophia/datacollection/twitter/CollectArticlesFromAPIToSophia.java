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

import org.jsoup.Jsoup;

public class CollectArticlesFromAPIToSophia extends Thread{

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
		System.out.println(article.title());
		map.put("art_title", article.title());
		String contenido = article.select("p").text();
		if(contenido.length()>1){
			map.put("art_content",contenido );
		}else{
			map.put("art_content","notContent");
			System.out.println("notContent");
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
		String[] userAgents = {"Mozilla/5.0 (compatible; ABrowse 0.4; Syllable)",
				"AmigaVoyager/3.2 (AmigaOS/MC680x0)",
				"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
				"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
				"Cyberdog/2.0 (Macintosh; PPC)",
				"Mozilla/3.0 (compatible; NetPositive/2.2.2; BeOS)"};
		int index = ThreadLocalRandom.current().nextInt(0, 3);
		//System.out.println(userAgents[index]);
		//Scrap it con JSoup
		return Jsoup.connect(urls.get(0).getString("url"))
				.userAgent(userAgents[index]) 
				.get();

	}

	public void run(){
		try {
			int counterDownload = 0;
			while(true){
				//download articles
				JSONObject jtweet = sophiaAPI.getTweet();
				String created_at = jtweet.getString("created_at");
				Document tweet = Document.parse( jtweet.toString() );
				long numberResults=1L;
				if (numberResults>0){
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

							//**VERIFICAR SI EL ARTICULO YA EXISTE EN SOPHIA API
							JSONObject jsonResponse = sophiaAPI.hasExistingArticle(mapArticle);
							System.out.println(jsonResponse);
							String id = jsonResponse.getString("_id");
							if (id.equals("0")){
								//** EL ARTICULO NO EXISTE*
								//enviamos el nuevo articulo si posee contenido
								//System.out.println(mapArticle.get("art_content"));
								String artContent = (String)mapArticle.get("art_content");
								if(artContent.equals("notContent")){
									System.out.println("article not have content");
									Map<String, Object> mapPublication = formatPublication(tweet);
									mapPublication.put("pub_article", "notDownload");
									String idNewPublication = sophiaAPI.postPublications(mapPublication);
								}else{
									String idNewArticle = sophiaAPI.postArticles(mapArticle);
									
									//enviamos el nuevo tweet, formatando el tweet antes
									Map<String, Object> mapPublication = formatPublication(tweet);
									mapPublication.put("pub_article", idNewArticle);
									String idNewPublication = sophiaAPI.postPublications(mapPublication);
									System.out.println(idNewArticle);
								}
							}
							else {
								//** EL ARTICULO EXISTE
								//enviamos el nuevo tweet, formatando el tweet antes
								Map<String, Object> mapPublication = formatPublication(tweet);
								mapPublication.put("pub_article", id);
								String idNewPublication = sophiaAPI.postPublications(mapPublication);
							}

							//Informamos a Mongo que terminamos procesar este tweet
							//change to_download to 0 in mongo
							Map<String,Object> mapCreated = new HashMap<String,Object>();
							mapCreated.put("created_at", created_at);
							sophiaAPI.postTweet(mapCreated);
						}
						catch (IOException e){
							//mongoCollection.updateMany(new Document("id",tweet.get("id")),new Document("$set", new Document("to_download", -1)));
							Map<String, Object> mapPublication = formatPublication(tweet);
							mapPublication.put("pub_article", "notDownload");
							String idNewPublication = sophiaAPI.postPublications(mapPublication);
							e.printStackTrace();
				
						}
					}
					else {
						//This tweet does not contain URL, tell to Mongo that this tweet has been processed
						//change to_download to 0 in mongo
						Map<String,Object> mapCreated = new HashMap<String,Object>();
						mapCreated.put("created_at", created_at);
						sophiaAPI.postTweet(mapCreated);
						Map<String, Object> mapPublication = formatPublication(tweet);
						mapPublication.put("pub_article", "notDownload");
						String idNewPublication = sophiaAPI.postPublications(mapPublication);
					}
					
				}
				else {
					//If there is no new entries, wait a moment...
					System.out.println("CollectArticlesFromAPIToSophia-1-Sleep");
					Thread.sleep(PARAM_WAITING_TIME);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (UnirestException e) {
			e.printStackTrace();
		}
	}

	public CollectArticlesFromAPIToSophia(String name){
		super(name);
		sophiaAPI = new SophiaAPIConnector();
	}
}
