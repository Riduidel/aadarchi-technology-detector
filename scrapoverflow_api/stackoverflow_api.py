import requests

def get_technology_tags():
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=100"
    
    try:
        all_technology_tags = []

        for i in range(1, 11):
            response = requests.get(api_url + f'&page={i}')
            response.raise_for_status()
            data = response.json()

            for tag in data['items']:
                #if 'technology' in tag['name']:
                all_technology_tags.append(tag['name'])

        return all_technology_tags

    except requests.exceptions.RequestException as e:
        print("Request error", e)
        return None

# Exemple d'utilisation
def main():
    popular_tags = get_technology_tags()
    if popular_tags is not None:
        for tag in popular_tags:
            print(tag)

if __name__ == "__main__":
    main()