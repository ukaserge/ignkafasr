# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: analysis_result_sid.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x19\x61nalysis_result_sid.proto\x12\x18limdongjin.ignasr.protos\"\xec\x01\n\x11\x41nalysisResultSid\x12\r\n\x05reqId\x18\x01 \x01(\t\x12\x0e\n\x06userId\x18\x02 \x01(\t\x12\x0f\n\x07videoId\x18\x03 \x01(\t\x12S\n\x0f\x63hunksSidResult\x18\x04 \x03(\x0b\x32:.limdongjin.ignasr.protos.AnalysisResultSid.ChunkSidResult\x12\x0b\n\x03msg\x18\x05 \x01(\t\x1a\x45\n\x0e\x43hunkSidResult\x12\x0f\n\x07\x63hunkId\x18\x01 \x01(\t\x12\x13\n\x0bspeakerName\x18\x02 \x01(\t\x12\r\n\x05score\x18\x03 \x01(\x02\x62\x06proto3')

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'analysis_result_sid_pb2', globals())
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  _ANALYSISRESULTSID._serialized_start=56
  _ANALYSISRESULTSID._serialized_end=292
  _ANALYSISRESULTSID_CHUNKSIDRESULT._serialized_start=223
  _ANALYSISRESULTSID_CHUNKSIDRESULT._serialized_end=292
# @@protoc_insertion_point(module_scope)