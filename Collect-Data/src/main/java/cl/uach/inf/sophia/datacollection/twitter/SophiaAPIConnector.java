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

	final String PARAM_SOPHIA_API_ARTICLES = "http://localhost:8000/v2/articles/";
	final String PARAM_SOPHIA_API_CHECK_ARTICLE = "http://localhost:8000/v2/articles/exist/";
	final String PARAM_SOPHIA_API_PUBLICATIONS = "http://localhost:8000/v2/publications/";

	public void getArticles(){
		try{
			GetRequest jsonResponse = Unirest.get(PARAM_SOPHIA_API_ARTICLES);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public String postArticles(Map<String,Object> map){
		try{
			HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_ARTICLES)
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
			return "error";
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
	
	public void putPublications(Map<String, Object> map, String idPublication) {
		try{
			HttpResponse<JsonNode> jsonResponse = Unirest.put(PARAM_SOPHIA_API_PUBLICATIONS+idPublication+"/")
					  .header("accept", "application/json").fields(map)
					  .asJson();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public String checkArticle(Map<String, Object> mapCheckArticle) {
		try{
			HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_CHECK_ARTICLE)
					  .header("accept", "application/json").fields(mapCheckArticle)
					  .asJson();
			JSONObject response = new JSONObject(jsonResponse.getBody());
			JSONArray arrayResponse = response.getJSONArray("array");
			JSONObject cleanResponse = arrayResponse.getJSONObject(0);
			String idResponse = (String) cleanResponse.get("_id");
			//System.out.println( idResponse);
			return idResponse;
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return null;
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