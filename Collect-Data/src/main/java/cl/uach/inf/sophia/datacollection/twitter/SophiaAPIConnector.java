package cl.uach.inf.sophia.datacollection.twitter;

import java.util.Map;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

public class SophiaAPIConnector {

	final String PARAM_SOPHIA_API_ARTICLES = "http://api.sophia-project.info/v2/articles/";

	public void getArticles(){
		try{
			GetRequest jsonResponse = Unirest.get(PARAM_SOPHIA_API_ARTICLES);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public int postArticles(Map<String,Object> map){
		try{
			HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_ARTICLES)
					  .header("accept", "application/json").fields(map)
					  .asJson();
			return jsonResponse.getStatus();
		}
		catch (Exception e){
			e.printStackTrace();
			return 0;
		}
	}


	/********************** TEST **************************************/

	public static final void main(final String[] args) throws Exception {
		SophiaAPIConnector x = new SophiaAPIConnector();
		x.runTest1();
	}
	
	private void runTest1() throws UnirestException {
		HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_ARTICLES)
				  .header("accept", "application/json")
				  .field("parameter", "value")
				  .field("foo", "bar")
				  .asJson();
	}
}