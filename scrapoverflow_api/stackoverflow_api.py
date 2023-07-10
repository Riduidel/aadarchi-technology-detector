import requests
import json

def get_tag_description(tech):
    api_url = "https://api.stackexchange.com/2.3/tags/" + tech + "/wikis?site=stackoverflow"
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

def get_technology_tags(api_key):
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=100"
    has_more = True
    page = 1

    try:
        all_technology_tags = []

        while has_more:
            #response = requests.get(api_url + f'&page={page}', params={"key": api_key})
            #response = requests.get(api_url + f'&page={page}')
            
            #response.raise_for_status()
            #data = response.json()

            data = load_json_file()

            for tag in data['items']:
                tag_info = {
                        'name': tag['name'],
                        'count': tag['count'],
                        'description' : get_tag_description(tag['name'].strip(" "))
                }
                all_technology_tags.append(tag_info)
            has_more = data["has_more"]            
            
            ##############change later#############
            if page == 1:
                break
            ###########################
            page += 1

        return all_technology_tags

    except requests.exceptions.RequestException as e:
        print("Request error", e)
        return None

#TESTING : just to avoid going too far on requests
def load_json_file():
    with open("output.json", 'r') as file:
        json_data = json.load(file)
    return json_data

def main():
    f = open("key.txt", "r")
    api_key = f.readlines()
    technology_tags = get_technology_tags(api_key[0])

    if technology_tags is not None:
        for tag in technology_tags:
            # print(tag)
            print(tag["name"], "- Number of questions:", tag["count"], " - description: ", tag["description"])

if __name__ == "__main__":
    main()