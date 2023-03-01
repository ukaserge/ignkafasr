import logging
from pytube import YouTube
from urllib.parse import urlparse, parse_qs

def download_webm_audio(yt: YouTube):
    try:
        webm_filename = yt.streams.filter(only_audio=True).filter(mime_type="audio/webm").last().download(filename=yt.video_id+".webm")
    except Exception as e:
        logging.error("FAIL download_webm_audio")
        logging.error(e)
        return None

    return webm_filename

def init_youtube_object(url):
    predicate = lambda y: (y is None or 'videoDetails' not in y.vid_info)
    
    i = 0
    yt = None
    while i < 10 and predicate(yt):
        yt = YouTube(url)
        if i > 0:
            logging.info(f"RETRY {i}.. pytube bug handling. videoDetails not in yt.vid_info")
        i = i + 1

    if predicate(yt):
        logging.error("just return because can't get video info. ")
        return None

    return yt

def extract_video_id(url):
    try:
        video_id = parse_qs(urlparse(url).query)['v'][0]
    except KeyError as e:
        logging.error("VIDEO URL PARSE ERROR!!!!")
        logging.error(url)
        logging.error(e)
        return None

    return video_id

