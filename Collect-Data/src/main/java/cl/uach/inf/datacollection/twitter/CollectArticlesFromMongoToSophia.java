package cl.uach.inf.datacollection.twitter;

import java.io.IOException;
import java.util.ArrayList;

import org.bson.Document;

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

	public void run(){
		while(true)
		{
			//Read the mongo database to find articles to download
			FindIterable<Document> docCursor = mongoCollection.find(new BasicDBObject("to_download", 1));

			if (mongoCollection.count(eq("to_download", 1))>0){
				while (docCursor.iterator().hasNext()){
					//Get the URL of the article and scrap it
					Document doc = docCursor.iterator().next();
					Document entities = (Document) doc.get("entities");
					if (entities !=null){
						ArrayList<Document> urls = (ArrayList<Document>) entities.get("urls");
						if (urls!=null&&urls.size()>0){
							String url = urls.get(0).getString("url");
							org.jsoup.nodes.Document article;
							try {
								article = Jsoup.connect(url).get();
								//System.out.println("title:"+article.title());
								//System.out.println("content:"+article.select("p").text());
								
								//Queries to SophiaAPI

								//Update MongoDB
								mongoCollection.updateOne(new Document("id",doc.get("id")),new Document("$set", new Document("to_download", 0)));
								
							} catch (IOException e) {
								e.printStackTrace();
								//ERROR: code 2
								mongoCollection.updateOne(new Document("id",doc.get("id")),new Document("$set", new Document("to_download", 2)));
							}
						}
					}
				}
			}
			else {
				try {
					System.out.println("wait 2 minutes");
					Thread.sleep(PARAM_WAITING_TIME);

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}		
	}	

	public CollectArticlesFromMongoToSophia(String name){
		super(name);		
		//Conexión al SGBD Mongo
		mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
		//Conexión a una collecion de una base de datos particular
		mongoDatabase = mongoClient.getDatabase(databaseName);
		mongoCollection = mongoDatabase.getCollection(collectionName);
	}
}
