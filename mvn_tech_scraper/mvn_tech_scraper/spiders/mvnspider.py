import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor

#api key : 933ff5de-cd0f-4c71-ae93-ed565aefacfa

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

    def parse_categories():
        yield {

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