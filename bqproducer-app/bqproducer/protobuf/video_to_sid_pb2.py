# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: video_to_sid.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x12video_to_sid.proto\"\xe9\x01\n\nVideoToSid\x12\r\n\x05reqId\x18\x01 \x01(\t\x12\x0e\n\x06userId\x18\x02 \x01(\t\x12\x0f\n\x07videoId\x18\x03 \x01(\t\x12\x33\n\x0f\x63hunksSidResult\x18\x04 \x03(\x0b\x32\x1a.VideoToSid.ChunkSidResult\x12\x0b\n\x03msg\x18\x05 \x01(\t\x12\x11\n\ttimestamp\x18\x06 \x01(\x03\x1aV\n\x0e\x43hunkSidResult\x12\x12\n\nchunkRange\x18\x01 \x01(\t\x12\x13\n\x0bspeakerName\x18\x02 \x01(\t\x12\r\n\x05score\x18\x03 \x01(\x02\x12\x0c\n\x04text\x18\x04 \x01(\tb\x06proto3')

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'video_to_sid_pb2', globals())
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  _VIDEOTOSID._serialized_start=23
  _VIDEOTOSID._serialized_end=256
  _VIDEOTOSID_CHUNKSIDRESULT._serialized_start=170
  _VIDEOTOSID_CHUNKSIDRESULT._serialized_end=256
# @@protoc_insertion_point(module_scope)
