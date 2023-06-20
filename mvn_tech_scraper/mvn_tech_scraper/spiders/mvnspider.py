import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor

class createMvnSPider(CrawlSpider):
    name = "mvnSpider"
    allowed_domains = "mvnrepository.com"
    start_url = "https://mvnrepository.com/"

    #spring example : mvnrepository.com/open-source/web-frameworks -> /artifact/org.springframework.boot/spring-boot-starter-web -> https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web/3.1.0
    # from mvn home page, all categories have "open-source" url section.

    rules = (
        Rule(LinkExtractor(allow="open-source"), callback="parse_categories") #get categories
        #Rule(LinkExtractor(allow="artifact")) -> get techs
        
    )

    def parse_categories(self, response):
        yield {
            "categories": response.css("ul.box-content").get()
        }



# def parse(self, response):
#         tags = response.css("a.post-tag") #on a div would be nice
#         try:
#             yield tags
#         except:
#             return
        
#         for tags in response.css("a.post-tag"):
#             try:
#                 'data': tags.css(::data // something to get the data field containing the tech name)
#             yield: