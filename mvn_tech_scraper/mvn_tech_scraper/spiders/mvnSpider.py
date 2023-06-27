import scrapy
from scrapy_playwright.page import PageMethod
#from scrapy_playwright.page import PageCoroutine

# https://www.youtube.com/watch?v=0wO7K-SoUHM&ab_channel=JohnWatsonRooney -> https://shoppable-campaign-demo.netlify.app/#/

class MvnspiderSpider(scrapy.Spider):
    name = 'mvnSpider'
    # allowed_domains = ['https://mvnrepository.com/popular']
    # start_urls = ['http://https://mvnrepository.com/popular/']

    def start_requests(self):
        yield scrapy.Request("https://mvnrepository.com/popular", 
                            meta= dict(
                                playwright = True,
                                playwright_include_page = True,
                                headless = False,
                                #playwright_page_methods = [PageMethod("wait_for_selector", "div.content")],
                            ))

    def parse(self, response):
        yield {
            "text": response.text
        }
