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
from ConfigParser import SafeConfigParser

parser = SafeConfigParser()
parser.read('app_data.ini')

user = str(parser.get('sophia_auth', 'user'))
password = str(parser.get('sophia_auth', 'password'))
## Connection with the local MongoDB where the tweets were storaged.
            
client = MongoClient('localhost', 27017)
db = client['twitter_db']
collection = db['media_tweets']
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
                ## Extracting the largest content
                #
                #  extractor is the variable that has the article's text, very raw text.
                extractor = Extractor(extractor='ArticleExtractor', html = response)
                ## Not the best way to filter, by it works so far.
                if extractor.getText() == '' or 'Rating is available when the video has been rented' in extractor.getText():
                    print ('No Content Available - WARNING')
                else:
                    ## Add data to the document
                    #
                    #  We make sure that there is a Title and text in the Article,
                    #  then we add this data to the document as is coded.
                    doc['title'] = Title
                    doc['date'] = document['date']
                    doc['host'] = document['screen_name']
                    doc['url'] = document['article_url']
                    doc['content'] = extractor.getText()
                    doc['imageLink'] = document['image_url']
                    if len(Title) > 50:
                        Title = Title[:20]
                    
                    article_found = False
                        
                    api_articles = requests.get('http://api.sophia-project.info/articles/', auth=(user, password))
                    json_articles = json.loads(api_articles.text)
                    next_results = json_articles['next']
                    print next_results
                    results = json_articles['results']
                    if next_results == None:
                        for i in results:
                            api_title = i['title']
                            api_date = i['date']
                            if api_title == Title and api_date == document['date']:
                                article_found = True
                    else:
                        for i in results:
                            api_title = i['title']
                            api_date = i['date']
                            if api_title == Title and api_date == document['date']:
                                article_found = True
                        while(article_found == False and next_results != None):
                            api_articles = requests.get(next_results, auth=(user, password))
                            json_articles = json.loads(api_articles.text)
                            next_results = json_articles['next']
                            print next_results
                            results = json_articles['results']
                            for i in results:
                                api_title = i['title']
                                api_date = i['date']
                                if api_title == Title and api_date == document['date']:
                                    article_found = True
                    if article_found == True:
                        print ('Article already exists in the DB, skipping...')
                    else:
                        print ('New Article, adding...')
                        content = extractor.getText()
                        content = json.dumps(content, 'utf-8')
                        data = '{"title": "'+Title+'","date": "'+document['date']+'","host": "'+document['screen_name']+'","url": "'+document['article_url']+'","content": '+content+',"imageLink": "'+document['image_url']+'"}'
                        data = data.encode('utf-8')
                        print data
                        headers = {
                                    'Content-Type': 'application/json',
                                    }
                        r = requests.post('http://api.sophia-project.info/articles/',  auth=(user, password), headers=headers, data=data)
                        print ('REQUEST STATUS: '+str(r.status_code))
                print ('--------------------------------------------------------------------')
            except AttributeError:
                print ('THERE IS NO TITLE, THEREFORE NO ARTICLE')
                print ('--------------------------------------------------------------------')
    else:
        ## The code enters here when there is no new articles in the local database and waits for 1 minute
        print ("CURSOR EMPTY, NO NEW ARTICLES TO DOWNLOAD")
        time.sleep(1*60)