import io
import logging
import sys
import uuid
from typing import Optional, Tuple, Any, Dict, List
from uuid import UUID
from confluent_kafka \
    import Message, Consumer as KafkaConsumer, Producer as KafkaProducer, KafkaException
from urllib.parse import urlparse, parse_qs
from pytube import YouTube
from pydub import AudioSegment
import ffmpeg

from .protobuf import analysis_request_pb2, analysis_result_basic_pb2, speech_request_pb2
from .ignite import IgniteRepository
# from .exception import EmptyRegisterGroupException, InvalidAccessException, UnknownBugException
from .kafka import print_assignment, on_acked_print
from .cassandra import CassandraRepository

logging.basicConfig(
    format="%(asctime)s %(levelname)s:%(name)s: %(message)s",
    level=logging.DEBUG,
    datefmt="%H:%M:%S",
    stream=sys.stdout,
)

# KAFKA
ANALYSIS_REQUEST_TOPIC = 'analysis.request'
ANALYSIS_RESULT_BASIC_TOPIC = 'analysis.result.basic'
SPEECH_REQUEST_TOPIC = 'speech.request'

# CASSANDRA
YOUTUBE_VIDEO_TABLE = 'dj.youtube_video'

# IGNITE
BLOB_CACHE = 'blobs'

# CREATE TABLE dj.youtube_video (id text PRIMARY KEY, title text, description text, channel_id text, channel_url text, embed_url text, keywords text, views bigint, thumbnail_url text, is_processed boolean, duration float);

class ScrapMain:
    consumer: KafkaConsumer
    producer: KafkaProducer

    def __init__(
            self,
            consumer: KafkaConsumer,
            producer: KafkaProducer,
            ignite_repository: IgniteRepository,
            cassandra_repository: CassandraRepository
    ):
        self.consumer = consumer
        self.producer = producer
        self.ignite_repository = ignite_repository
        self.cassandra_repository = cassandra_repository
    
    def build_analysis_result_basic(self, req_id, user_id, url, yt, result_row):
        print(yt)
        print(result_row)
        assert yt is not None or result_row is not None
        assert yt is None or result_row is None
        
        obj = yt if yt is not None else result_row

        analysis_result_basic = analysis_result_basic_pb2.AnalysisResultBasic(
            reqId = req_id,
            userId = user_id,
            url = url,
            videoId = yt.video_id if yt is not None else result_row.id,
            title = obj.title,
            description = obj.description,
            channelId = obj.channel_id,
            channelUrl = obj.channel_url,
            duration = str(yt.length) if yt is not None else str(result_row.duration),
            embedUrl = obj.embed_url,
            keywords = str(obj.keywords),
            views = str(obj.views),
            thumbnailUrl = obj.thumbnail_url
        )
        
        return analysis_result_basic 
    
    def download_wav_audio(self, yt):
        webm_filename = yt.streams.filter(only_audio=True).filter(mime_type="audio/webm").last().download(filename=yt.video_id+".webm")
        webm_stream = ffmpeg.input(webm_filename, f='webm')
        wav_filename = webm_filename.replace(yt.video_id+'.webm', yt.video_id+'.wav')
        wav_stream = ffmpeg.output(webm_stream, wav_filename, format='wav', osr=16000, ac=1)
        ffmpeg.run(wav_stream)
        return wav_filename
    
    def calc_num_of_chunks(self, duration_seconds, window_size=5000):
        return ((int(duration_seconds*1000)+1)//window_size)+1

    def build_chunk_dict(self, wav_filename, prefix, window_size=5000):
        Chunk_Key = lambda i: (prefix + str(i))
        segment = AudioSegment.from_file(wav_filename, format="wav")
        chunk_sequence = (segment[start:start+window_size] for start in range(0, int(segment.duration_seconds*1000)+1, window_size))
        chunk_dict = { Chunk_Key(i):  chunk.export(format='wav').read() for i, chunk in enumerate(chunk_sequence) }
        return chunk_dict
    
    def add_video_to_cassandra(self, video_id, video):
        query = f"INSERT INTO {YOUTUBE_VIDEO_TABLE} (id, title, description, channel_id, duration, embed_url, views, thumbnail_url) VALUES ('{video_id}', '{video.title}', '{video.description}', '{video.channelId}', {float(video.duration)}, '{video.embedUrl}', {video.views}, '{video.thumbnailUrl}')"

        self.cassandra_repository.execute(query)

    def on_next(self, msg: Message):
        logging.debug('on_next analysis.request: msg.value = ')
        logging.debug(msg.value())

        analysis_request = analysis_request_pb2.AnalysisRequest.FromString(msg.value())
        req_id: str = analysis_request.reqId
        user_id: str = analysis_request.userId
        url: str = analysis_request.url 

        try:
            video_id = parse_qs(urlparse(url).query)['v'][0]
        except KeyError as e:
            print("on_next KeyError!!!!")
            print(url)
            print(e)
            return

        is_stored = False
        result_set = self.cassandra_repository.execute(f"SELECT * FROM {YOUTUBE_VIDEO_TABLE} WHERE id = '{video_id}'")
        result_row = result_set.one() 
        if result_row is not None and result_row.is_processed is True:
            is_stored = True    
        
        assert url.find("youtube") != -1

        if result_row is None:
            yt = YouTube(url)
            print(yt)
            # scrap video's basic info
            analysis_result_basic = self.build_analysis_result_basic(
                req_id, 
                user_id,
                url,
                yt,
                None
            )
        else:
            yt = None
            analysis_result_basic = self.build_analysis_result_basic(
                req_id,
                user_id,
                url,
                None,
                result_row
            )
        
        duration_seconds = yt.length if yt is not None else result_row.duration
        
        # publish to analysis.result.basic
        self.producer.produce(
                topic=ANALYSIS_RESULT_BASIC_TOPIC, 
                value = analysis_result_basic.SerializeToString(), 
                callback=on_acked_print
        )
        self.producer.poll(3)
         
        if not is_stored:
            chunk = self.ignite_repository.get(
                cache_name = BLOB_CACHE,
                key = video_id + "_0"
            )
            if chunk is not None:
                is_stored = True
        
        if not is_stored:
            if yt is None:
                yt = YouTube(url)
            # download wav audio
            wav_filename = self.download_wav_audio(yt)

            # build chunk_dict
            chunk_dict = self.build_chunk_dict(wav_filename, prefix=f"{video_id}_")

            #   and audio chuncking and store to ignite db
            self.ignite_repository.put_all(
                    cache_name = BLOB_CACHE, 
                    key_value_dict = chunk_dict
            )

        # publish to speech.request
        speech_request = speech_request_pb2.SpeechRequest(
            reqId = req_id,
            userId = user_id,
            videoId = video_id,
            numOfChunk = self.calc_num_of_chunks(duration_seconds)
        )

        self.producer.produce(
            topic=SPEECH_REQUEST_TOPIC, 
            value=speech_request.SerializeToString(), 
            callback=on_acked_print
        )
        self.producer.poll(3)
        
        if result_row is None: 
            self.add_video_to_cassandra(video_id, analysis_result_basic)

    def run_loop(self):
        try:
            self.consumer.subscribe([ANALYSIS_REQUEST_TOPIC], on_assign=print_assignment)
            while True:
                msg: Optional[Message] = self.consumer.poll(timeout=1000.0)

                if msg is None:
                    logging.debug("Wait...")
                    continue
                elif msg.error():
                    logging.debug("MSG.ERROR() !!!!")
                    raise KafkaException(msg.error())
                
                sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                 (msg.topic(), msg.partition(), msg.offset(),
                                  str(msg.key())))
                logging.debug(msg.value())
                self.consumer.store_offsets(msg)

                try:
                    self.on_next(msg)
                except RuntimeError as e:
                    logging.debug("ERRORRR: ")
                    logging.debug(e)
                    raise e

                self.consumer.commit(asynchronous=True)
                logging.debug("consumer.commit ok")
        finally:
            self.consumer.close()
            self.producer.flush()
