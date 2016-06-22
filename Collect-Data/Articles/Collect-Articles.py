## @package Collect-Data
#  Documentation for this module.
#
#  In this module, the different collectors are contained.
#  The collectors here shown are for Twitter and Articles from urls.
import time
import pymongo
from pymongo import MongoClient
import json
import requests
from boilerpipe.extract import Extractor
from BeautifulSoup import BeautifulSoup
## Connection with the local MongoDB where the tweets were storaged.
            
client = MongoClient('localhost', 27017)
db = client['twitter_db']
collection = db['twitter_collection']
while(1):
    ## Cursor to find the data that is available to download the article.
    #
    #  Articles ready to be download are marked with the label 'to_download' = 1.
    #  This cursor have the data sorted by date, from the oldest one to the most recent.
    cursor = collection.find({'to_download': 1}).sort('date', pymongo.ASCENDING)
    ## Getting each document found.
    #
    #  For each document, we'll get the article's url in order to do a request for its html code.
    #  Through the html code, we extract the largest content, assuming that it's the text from the article.
    if (cursor.count() > 0):
        print ("NUMBER OF NEW ARTICLES: "+str(cursor.count()))
        for document in cursor:
            try:
                ## A new json document is created
                doc = {}
                article_url = document['article_url']
                print('URL: '+article_url)
                print('DATE: '+document['date'])
                response = requests.get(article_url).content
                soup = BeautifulSoup(response, convertEntities=BeautifulSoup.HTML_ENTITIES)
                ##Extracting the article's title.
                Title = soup.title.string
                print ('Title: '+Title)
                print('')
                ## Extracting the largest content
                #
                #  extractor is the variable that has the article's text, very raw text.
                extractor = Extractor(extractor='ArticleExtractor', html = response)
                ## Not the best way to filter, by it works so far.
                if extractor.getText() == '' or 'Rating is available when the video has been rented' in extractor.getText():
                    print ('No Content Available - WARNING')
                else:
                    print('Article: ')
                    print extractor.getText()
                    ## Add data to the document
                    #
                    #  We make sure that there is a Title and text in the Article,
                    #  then we add this data to the document as is coded.
                    doc['Title'] = Title
                    doc['Article'] = extractor.getText()
                ##HERE WE MUST THEN CHECK FOR DUPLICATED ARTICLES TO UPDATE OR TO ADD THEM TO THE DATABASE FROM SOPHIA
                ##WORK TO DO...
                print ('--------------------------------------------------------------------')
            except AttributeError:
                print ('THERE IS NO TITLE, THEREFORE NO ARTICLE')
                print ('--------------------------------------------------------------------')
    else:
        ## The code enters here when there is no new articles in the local database and waits for 1 minute
        print ("CURSOR EMPTY, NO NEW ARTICLES TO DOWNLOAD")
        time.sleep(1*60)