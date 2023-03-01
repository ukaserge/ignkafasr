import logging
from pytube import Search

def search_and_select_url_results(keyword, limit_duration_seconds, limit_num):
    predicate = lambda video: (
        video is not None and 
        'videoDetails' in video.vid_info and 
        'lengthSeconds' in video.vid_info['videoDetails'] and 
        video.length != 0 and 
        video.length < limit_duration_seconds
    )

    search_obj = Search(keyword)
    urls = set(video.watch_url for video in search_obj.results if predicate(video))

    i = 0
    while len(urls) < limit_num and i < 1000:
        try:
            search_obj.get_next_results()
        except IndexError:
            logging.debug("no more search result")
            break
        urls = urls | set(video.watch_url for video in search_obj.results if predicate(video))     
        i += 1

    logging.debug("SEARCH OK")
    if i == 1000:
        logging.error("INFINITY LOOP ?!!!!")

    return urls


