package cl.uach.inf.sophia.datacollection;

import cl.uach.inf.sophia.datacollection.twitter.CollectArticlesFromMongoToSophia;
import cl.uach.inf.sophia.datacollection.twitter.CollectTweetsFromTwitterToMongo;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class SophiaCollector {

	public static void main(String[] args) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		CollectTweetsFromTwitterToMongo t1 = new CollectTweetsFromTwitterToMongo("CollectDataFromTwitterToMongo-1");
		t1.start();
		CollectArticlesFromMongoToSophia t2 = new CollectArticlesFromMongoToSophia("CollectArticlesFromMongoToSophia-1");
		t2.start();
	}
}
