import requests
import json

from requests_oauthlib import OAuth2Session
from oauthlib.oauth2 import BackendApplicationClient
from requests.auth import HTTPBasicAuth

from pprint import pprint

def get_tag_description(tech, api_key):
    api_url = "https://api.stackexchange.com/2.3/tags/" + tech + "/wikis?site=stackoverflow&key=" + api_key
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.RequestException as e:
        print("Request error for description :", e)
        return None
    
    for elem in data["items"]:
        try:
            return elem["excerpt"]
        except:
            return "No description"

#TESTING : just to avoid going too far on requests
def load_json_file():
    with open("output.json", 'r') as file:
        json_data = json.load(file)
    return json_data

def get_technology_tags(api_key):
    #REMEMBER TO REMOVE THE KEY
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=10"
    has_more = True
    page = 1
    all_technology_tags = []

    while has_more:
        try:
            response = requests.get(api_url + f'&page={page}' + "&key=" + api_key)
            
            response.raise_for_status()
            data = response.json()
        except requests.exceptions.RequestException as e:
            print("Request error in get_technology_tags", e)
            return None
        
        #Only when testing with local json file
        #data = load_json_file()

        for tag in data['items']:
            tag_info = {
                    'name': tag['name'],
                    'count': tag['count'],
                    'description' : get_tag_description(tag['name'].strip(" "), api_key)
            }
            all_technology_tags.append(tag_info)
        has_more = data["has_more"]            
        
        ##############change later#############
        if page == 1:
            break
        ###########################
        page += 1

    return all_technology_tags

def main():
    f = open("key.txt", "r")
    api_infos = f.readlines()
    print(api_infos[0])
    technology_tags = get_technology_tags(api_infos[0])

    if technology_tags is not None:
        for tag in technology_tags:
            #print(tag["name"], "- Number of questions:", tag["count"], " - description: ")
            print(tag["name"], "- Number of questions:", tag["count"], " - description: ", tag["description"])

if __name__ == "__main__":
    main()