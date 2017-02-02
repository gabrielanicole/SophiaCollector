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

	final String URL_BASE_API = "http://localhost:8000/v2/";
	//final String URL_BASE_API = "http://api.sophia-project.info/v2/";
	final String PARAM_SOPHIA_API_ARTICLES = URL_BASE_API + "articles/";
	final String PARAM_SOPHIA_API_CHECK_ARTICLE = URL_BASE_API + "articles/exist/";
	final String PARAM_SOPHIA_API_PUBLICATIONS = URL_BASE_API + "publications/";

	public void getArticles(){
		GetRequest jsonResponse = Unirest.get(PARAM_SOPHIA_API_ARTICLES);
	}

	public String postArticles(Map<String,Object> map) throws UnirestException{
		HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_ARTICLES)
				.header("accept", "application/json").fields(map)
				.asJson();
		JSONObject response = new JSONObject(jsonResponse.getBody());
		JSONArray arrayResponse = response.getJSONArray("array");
		JSONObject cleanResponse = arrayResponse.getJSONObject(0);
		String idResponse = (String) cleanResponse.get("_id");
		return idResponse;
	}

	public String postPublications(Map<String,Object> map) throws UnirestException{
		HttpResponse<JsonNode> jsonResponse= Unirest.post(PARAM_SOPHIA_API_PUBLICATIONS)
				.header("accept", "application/json").fields(map)
				.asJson();
		JSONObject response = new JSONObject(jsonResponse.getBody());
		JSONArray arrayResponse = response.getJSONArray("array");
		JSONObject cleanResponse = arrayResponse.getJSONObject(0);
		String idResponse = (String) cleanResponse.get("_id");
		return idResponse;

	}

	public String putPublications(Map<String, Object> map, String idPublication) throws UnirestException {
		HttpResponse<JsonNode> jsonResponse=Unirest.put(PARAM_SOPHIA_API_PUBLICATIONS+idPublication+"/")
				.header("accept", "application/json").fields(map)
				.asJson();
		return jsonResponse.getStatusText();
	}

	public String putArticles(Map<String, Object> map, String idArticle) throws UnirestException {
		HttpResponse<JsonNode> jsonResponse=Unirest.put(PARAM_SOPHIA_API_ARTICLES+idArticle+"/")
				.header("accept", "application/json").fields(map)
				.asJson();
		return jsonResponse.getStatusText();
	}
	
	public JSONObject getTweet() throws UnirestException {
		HttpResponse<JsonNode> jsonResponse=Unirest.get("http://localhost:8000/past/").asJson();
		//HttpResponse<JsonNode> jsonResponse=Unirest.get("http://api.sophia-project.info/past/").asJson();
		JSONObject response = jsonResponse.getBody().getObject();
		return response;
	}
	
	public void postTweet(Map<String,Object> map) throws UnirestException{
		//HttpResponse<JsonNode> jsonResponse= Unirest.post("http://api.sophia-project.info/past/")
		HttpResponse<JsonNode> jsonResponse= Unirest.post("http://localhost:8000/past/")
				.header("accept", "application/json").fields(map)
				.asJson();
		JSONObject response = jsonResponse.getBody().getObject();
		System.out.println(response);
	}

	public JSONObject hasExistingArticle(Map<String, Object> mapArticle) throws UnirestException {
		HttpResponse<JsonNode> jsonResponse = Unirest.post(PARAM_SOPHIA_API_CHECK_ARTICLE)
				.header("accept", "application/json").fields(mapArticle)
				.asJson();
		JSONObject response = new JSONObject(jsonResponse.getBody());
		JSONArray arrayResponse = response.getJSONArray("array");
		JSONObject cleanResponse = arrayResponse.getJSONObject(0);
		String idResponse = (String) cleanResponse.get("_id");
		//System.out.println( idResponse);
		return cleanResponse;
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