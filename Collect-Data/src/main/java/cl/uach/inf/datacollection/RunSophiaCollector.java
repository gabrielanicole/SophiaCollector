package cl.uach.inf.datacollection;

import cl.uach.inf.datacollection.twitter.CollectArticlesFromMongoToSophia;
import cl.uach.inf.datacollection.twitter.CollectTweetsFromTwitterToMongo;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class RunSophiaCollector {

	public static void main(String[] args) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		CollectTweetsFromTwitterToMongo t1 = new CollectTweetsFromTwitterToMongo("CollectDataFromTwitterToMongo-1");
		t1.start();
		CollectArticlesFromMongoToSophia t2 = new CollectArticlesFromMongoToSophia("CollectArticlesFromMongoToSophia-1");
		t2.start();
	}
}
