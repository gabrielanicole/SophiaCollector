## @package Collect-Data
#  Documentation for this module.
#
#  In this module, the different collectors are contained.
#  The collectors here shown are for Twitter and Articles from urls.

import tweepy
import time
from pymongo import MongoClient
import json

consumer_key = 'ac1yMlhXpxDAjzJWwmzagg'
consumer_secret= 'wpMMIXxkZ3ChqANkdVkzMH0wMdb8nKMqIVaztIEwtw'
access_token = '2177040169-5aVazrfgCpjhDOgemcIw1PvZXb3nbHtglUuAOcf'
access_secret = 'iCVFeVZkhSC3hsJfISbLDbpZZAyUS2Grn6ZJUNABaWmrt'

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_secret)

api = tweepy.API(auth)

since_id = 12345
count = 100
tweets_num = 0

## Connection with the local MongoDB for storage tweets.
		
client = MongoClient('localhost', 27017)
db = client['twitter_db']
collection = db['twitter_collection']

## Loop for obtain the tweets from the timeline
#
#  This loop runs every 5 minutes and makes a request to get 100 tweets.
#  The id from the tweets are storaged in order to determinate which was the last tweet saved.
#  The data will be storaged is:
#    - The article's url.
#    - The text from the tweet.
#    - The url of the tweet.
#    - The screen_name of the twitter account.
#    - The date that the tweet was created.
#    - The url of the image attached in the tweet.
#
#  NOTE:
#  When a tweet doesn't have a url that redirects to an article in a website, the tweet won't be storaged in the DB.
#  When a tweet doesn't have an image attached, it'll be storaged anyways.
while(1):
	try:
		print ("-- SEARCHING --")
		## List of the tweets' ids collected in this request.
		ids = []
		## Variable where the tweets obtained are.
		sophia_tweets = api.home_timeline(since_id = since_id, count = count)
		print("Number of tweets: "+str(len(sophia_tweets)))
		## For every tweet, we collect the metadata that's important for the application.
		for tweet in sophia_tweets:
			## Adding tweet's id to the list.
			ids.append(tweet.id)
			try:
				if tweet.entities['urls'][0]:
					article_url = tweet.entities['urls'][0]['expanded_url']
					tweet_text =  tweet.text
					tweet_url = 'https://twitter.com/'+tweet.user.screen_name+'/status/'+str(tweet.id)
			        screen_name = tweet.user.screen_name
			        date = tweet.created_at
			        image_url = tweet.entities['media'][0]['media_url']
			        
			        doc = {}
			        doc['article_url'] = article_url
			        doc['image_url'] = image_url
			        doc['tweet_text'] = tweet_text
			        doc['tweet_url'] = tweet_url
			        doc['screen_name'] = screen_name
			        ## YYYY-MM-DDTHH:MM:SS.
			        doc['date'] = date.isoformat()
			        doc['to_download'] = 1
			        ## The document is inserted into Local MonogDB.
			        collection.insert(doc)
			        print('Added to the DB WITH media')
			        tweets_num = tweets_num + 1
			## Exception to handle when a tweet has no urls in its metadata.
			except IndexError:
				print ('--Tweet without urls--')
			## Exception to handle when a tweet has no media (image(s) attached).
			except KeyError:
				print ('-- No media in tweet --')
				article_url = tweet.entities['urls'][0]['expanded_url']
				tweet_text =  tweet.text
				tweet_url = 'https://twitter.com/'+tweet.user.screen_name+'/status/'+str(tweet.id)
				screen_name = tweet.user.screen_name
				date = tweet.created_at
				
				doc = {}
				doc['article_url'] = article_url
				doc['tweet_text'] = tweet_text
				doc['tweet_url'] = tweet_url
				doc['screen_name'] = screen_name
				doc['date'] = date.isoformat()
				doc['to_download'] = 1
			        
				collection.insert(doc)
				print('Added to the DB WITHOUT media')
				tweets_num = tweets_num + 1	                    
		## From all the ids, we get the max from the list to know which tweet was the last in order to continue in the next run.
		since_id = max(ids)
		print("Tweets added: "+str(tweets_num)+"/"+str(len(sophia_tweets)))
		tweets_num = 0
		print("--New search in 5 minutes--")
		time.sleep(5*60)
	## Exception to handle the rate limit error for a request to Twitter, just in case.
	except tweepy.error.RateLimitError:
		print('Rate Limit Error, Waiting 2 Minutes')
		time.sleep(2*60)