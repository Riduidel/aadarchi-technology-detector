import requests
import json
import urllib.parse

import re
import logging
import sys

def get_tag_infos(tech, api_key):
    api_url = "https://api.stackexchange.com/2.3/tags/" +  urllib.parse.quote(tech) + "/wikis?site=stackoverflow" + "&filter=!nNPvSNMavg" + "&key=" + api_key
    to_return = list()
    
    logging.info("Fetching description and link for %s tag", tech)
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.RequestException as e:
        logging.error("Request error for %s tag description :", tech, e)
        return None
    
    for elem in data.get("items", []):
        if elem.get("excerpt", None) is not None:
            to_return.append(elem["excerpt"])
        else:
            to_return.append("No description found for " + tech)

        link = re.search(r'<a\s+href=[\'"]?(https?://[^\'" >]+)', elem["body"])
        if link == None:
            to_return.append("No link found for " + tech) 
            logging.info("No description found for %s tag", tech)
        else:
            to_return.append(link.group(0).strip('<a href='))
    return to_return

def get_technology_tags(api_key):
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=100"
    has_more = True
    page = 1
    all_technology_tags = []

    while has_more:
        logging.info("[PAGE] - Currently on page %d", page)
        try:
            response = requests.get(api_url + f'&page={page}' + "&key=" + api_key)
            response.raise_for_status()
            data = response.json()
        except requests.exceptions.RequestException as e:
            logging.error("Request error in get_technology_tags: %s", e)
            return None

        for tag in data['items']:
            logging.info("Processing %s tag", tag["name"])
            infos = get_tag_infos(tag['name'].strip(" "), api_key)

            tag_info = {
                    'name': tag['name'],
                    'popularity': tag['count'],
                    'description' : infos[0],
                    'link' : infos[1]
            }
            all_technology_tags.append(tag_info)
        has_more = data["has_more"]            
        logging.info("[INFO] - Quota remaining : " + str(data["quota_remaining"]))
        page += 1

    return all_technology_tags

def main(argv):
    logging.basicConfig(format='%(asctime)s - %(message)s', level=logging.INFO)
    technology_tags = get_technology_tags(argv)

    # if technology_tags is not None:
    #    for tag in technology_tags:
    #        print(tag["name"], "- Number of questions:", tag["popularity"], " - description: ", tag["description"], tag["link"])
    with open('technologies.json', 'w') as fp:
        json.dump(technology_tags, fp, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <API_KEY>")
        sys.exit(1)
    main(sys.argv[1])