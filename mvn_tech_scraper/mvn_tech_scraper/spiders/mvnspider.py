import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor

class createMvnSPider(CrawlSpider):
    name = "mvnSpider"
    allowed_domains = "mvnrepository.com"
    start_url = "https://mvnrepository.com/"

    #spring example : mvnrepository.com/open-source/web-frameworks -> /artifact/org.springframework.boot/spring-boot-starter-web -> https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web/3.1.0

    rules = (
        Rule(LinkExtractor(allow="artifact")),
        Rule(LinkExtractor(allow="open-source"))
        
    )