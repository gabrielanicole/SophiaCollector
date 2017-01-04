package cl.uach.inf.sophia.datacollection.twitter;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

public class SophiaAPIConnector {

	final String PARAM_SOPHIA_API_ARTICLES = "http://api.sophia-project.info/v2/articles/";
	final String PARAM_SOPHIA_API_PUBLICATIONS = "http://localhost:8000/v2/publications/";

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
	
	public String postPublications(Map<String,Object> map){
		try{
			HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_PUBLICATIONS)
					  .header("accept", "application/json").fields(map)
					  .asJson();
			JSONObject response = new JSONObject(jsonResponse.getBody());
			JSONArray arrayResponse = response.getJSONArray("array");
			JSONObject cleanResponse = arrayResponse.getJSONObject(0);
			String idResponse = (String) cleanResponse.get("_id");
			return idResponse;
		}
		catch (Exception e){
			e.printStackTrace();
			return "Error";
		}
	}



	/********************** TEST **************************************/

	public static final void main(final String[] args) throws Exception {
		SophiaAPIConnector x = new SophiaAPIConnector();
		//x.runTest1();
	}
	
	private void runTest1() throws UnirestException {
		HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_ARTICLES)
				  .header("accept", "application/json")
				  .field("parameter", "value")
				  .field("foo", "bar")
				  .asJson();
	}
}