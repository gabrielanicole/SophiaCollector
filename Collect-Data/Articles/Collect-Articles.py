# -*- coding: utf-8 -*-
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
#from boilerpipe.extract import Extractor
from bs4 import BeautifulSoup
import simplejson as json
from ConfigParser import SafeConfigParser
import time

#parser = SafeConfigParser()
#parser.read('app_data.ini')
#URL_API = 'http://localhost:8000/v2/'
URL_API = 'http://api.sophia-project.info/v2/'
#user = str(parser.get('sophia_auth', 'user'))
#password = str(parser.get('sophia_auth', 'password'))
## Connection with the local MongoDB where the tweets were storaged.

client = MongoClient('localhost', 27017)
db = client['SophiaCollector']
collection = db['Tweets']
while(1):
    ## Cursor to find the data that is available to download the article.
    #
    #  Articles ready to be download are marked with the label 'to_download' = 1.
    #  This cursor have the data sorted by date, from the oldest one to the most recent.
    cursor = collection.find({'to_download': 1},no_cursor_timeout=True).sort('date', pymongo.ASCENDING)
    ## Getting each document found.
    #
    #  For each document, we'll get the article's url in order to do a request for its html code.
    #  Through the html code, we extract the largest content, assuming that it's the text from the article.
    if (cursor.count() > 0):
        print ("NUMBER OF NEW ARTICLES: "+str(cursor.count()))
        for document in cursor:
            try:
                ## A new json document is created
                try:
                    #print document['entities']['urls'][0]['url']
                    url = document['entities']['urls'][0]['url']
                    articleHasUrl = True
                except:
                    print 'publicación no posee url'
                    url = "http://www.no-url.com"
                    articleHasUrl = False
                #time.sleep(30)
                if articleHasUrl:
                    #doc to write with POST:api.sophia-project/v2/articles
                    #pub to write with POST:api.sophia-project/v2/publications
                    doc = {}
                    pub = {}
                    doc['art_url']=url
                    pub['pub_content'] = document['text']
                    #print('URL: '+doc['art_url'])
                    #print document['created_at'
                    doc['art_date'] = time.strftime('%Y-%m-%d %H:%M:%S', time.strptime(document['created_at'],'%a %b %d %H:%M:%S +0000 %Y'))
                    pub['pub_date'] = doc['art_date']
                    try:
                        doc['art_image_link'] = document['entities']['media'][0]['media_url']
                    except:
                        print 'publicación no posee imagen'
                        doc['art_image_link'] = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ac/No_image_available.svg/600px-No_image_available.svg.png"
                    doc['art_name_press_source'] = document['user']['screen_name']
                    pub['pub_username'] = doc['art_name_press_source']
                    doc['art_category'] = 'unclassified'
                    pub['pub_url'] = "https://twitter.com/"+document['user']['screen_name']+"/status/"+document['id_str']
                    print
                    user_agent = {'User-agent': 'Mozilla/5.0'}
                    try:
                        req = requests.get(url, headers=user_agent)
                        statusCode = req.status_code
                        if statusCode == 200:
                            # Pasamos el contenido HTML de la web a un objeto BeautifulSoup()
                            html = BeautifulSoup(req.text,"html.parser")
                            doc['art_title'] = html.title.get_text()
                            # Obtenemos todos los divs donde estan las entradas
                            entradas = html.find_all('p')
                            #print entradas
                            content = ''
                            for i in entradas:
                                #print i.get_text()
                                content += i.get_text()+'\n'
                            #print content
                            if len(content)>1:
                                doc['art_content'] = content
                            else:
                                doc['art_content'] = 'No posee contenido'
                            # Recorremos todas las entradas para extraer el título, autor y fecha
                            print doc
                            print pub
                            try:
                                #revisamos si existe el artículo
                                existArticle = requests.post(URL_API+'articles/exist/',data=doc)
                                responseExist = json.loads(existArticle.content)
                                responseApiId = responseExist['_id']
                            except Exception as e:
                                print 'no se ha podido revisar si existe o no el articulo'
                                print e
                                responseApiId = 'no write'
                            if responseApiId == '0':
                                print 'nuevo articulo'
                                response = requests.post(URL_API+'articles/',data=doc)
                                print json.loads(response.content)
                                print response.status_code
                                if response.status_code == 201:
                                    print 'escrito correctamente'
                                    newArticle =  json.loads(response.content)
                                    print newArticle['_id']
                                    pub['pub_article'] = newArticle['_id']
                                    response = requests.post(URL_API+'publications/',data=pub)
                                    result = collection.update_one({"_id": document['_id']}, {"$set": {"to_download": 0}})
                            else:
                                pub['pub_article'] = responseApiId
                                response = requests.post(URL_API+'publications/',data=pub)
                                result = collection.update_one({"_id": document['_id']}, {"$set": {"to_download": 0}})

                        else:
                            print "Status Code %d" %statusCode
                    except:
                        print 'url no permite descargar artículo'
                    print ('--------------------------------------------------------------------')
                    time.sleep(5)
            except AttributeError:
                print ('THERE IS NO TITLE, THEREFORE NO ARTICLE')
                print ('--------------------------------------------------------------------')
            except KeyError:
                print ('New Article, adding...')
                time.sleep(5)
    else:
        ## The code enters here when there is no new articles in the local database and waits for 1 minute
        print ("CURSOR EMPTY, NO NEW ARTICLES TO DOWNLOAD")
        time.sleep(1*60)
    cursor.close()
