import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor

class createMvnSPider(CrawlSpider):
    name = "mvnSpider"
    allowed_domains = "mvnrepository.com"
    start_url = "https://mvnrepository.com/"

    rules = (
        
    )