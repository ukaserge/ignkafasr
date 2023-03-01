import logging

from .protobuf.search_request_pb2 import SearchRequest
from .protobuf.analysis_request_pb2 import AnalysisRequest
from .youtube_util import search_and_select_url_results

def on_next(search_request: SearchRequest):
    logging.debug(search_request)

    logging.debug("SEARCH START!!")

    urls = search_and_select_url_results(
            keyword = search_request.keyword, 
            limit_duration_seconds = search_request.limitDurationSeconds,
            limit_num = search_request.limitNum
        )

    logging.debug("SEARCH OK")
    
    reqId: str = search_request.reqId
    userId: str = search_request.userId

    return (
        AnalysisRequest(reqId = reqId, userId = userId, url = url) 
        for url 
        in urls
    )
