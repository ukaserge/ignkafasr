import logging
from google.oauth2.service_account import Credentials
from google.cloud.bigquery_storage_v1 import BigQueryWriteClient
from google.cloud.bigquery_storage_v1 import types
from google.cloud.bigquery_storage_v1 import writer
from google.protobuf import descriptor_pb2

def _to_descriptor_proto_data(proto_type):
    proto_schema = types.ProtoSchema()
    proto_descriptor = descriptor_pb2.DescriptorProto()
    proto_type.DESCRIPTOR.CopyToProto(proto_descriptor)
    proto_schema.proto_descriptor = proto_descriptor

    proto_data = types.AppendRowsRequest.ProtoData()
    proto_data.writer_schema = proto_schema
    return proto_data

def _to_single_proto_rows(proto_obj):
    proto_rows = types.ProtoRows()
    proto_rows.serialized_rows.append(proto_obj.SerializeToString())

    return proto_rows

def send_single_proto_to_bq(
    proto_obj,
    proto_type,
    credentials: Credentials,
    project_id: str,
    dataset_name: str,
    table_name: str
):
    client = BigQueryWriteClient(credentials = credentials)
    parent = client.table_path(project_id, dataset_name, table_name)
    
    write_stream = types.WriteStream()
    write_stream.type_ = types.WriteStream.Type.PENDING
    write_stream = client.create_write_stream(parent = parent, write_stream = write_stream)
    stream_name = write_stream.name

    request_template = types.AppendRowsRequest() 
    request_template.write_stream = stream_name

    #
    request_template.proto_rows = _to_descriptor_proto_data(proto_type)
    append_rows_stream = writer.AppendRowsStream(client, request_template)
    request = types.AppendRowsRequest()
    request.offset = 0

    send_proto_data = types.AppendRowsRequest.ProtoData()

    # 
    send_proto_data.rows = _to_single_proto_rows(proto_obj)
    request.proto_rows = send_proto_data

    result_future = append_rows_stream.send(request)
    logging.debug(result_future.result())
    
    append_rows_stream.close()
    client.finalize_write_stream(name=write_stream.name)

    batch_commit_write_streams_request = types.BatchCommitWriteStreamsRequest()
    batch_commit_write_streams_request.parent = parent
    batch_commit_write_streams_request.write_streams = [write_stream.name]
    client.batch_commit_write_streams(batch_commit_write_streams_request)

    return True


