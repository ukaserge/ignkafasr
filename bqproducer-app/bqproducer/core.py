from typing import Dict
from google.oauth2.service_account import Credentials

from .protobuf.analysis_result_sid_pb2 import AnalysisResultSid
from .bq_util import send_single_proto_to_bq

"""CORE FUNCTION"""
def on_next(
    analysis_result_sid: AnalysisResultSid, 
    bq_conf: Dict
):
    credentials: Credentials = bq_conf['credentials_loader']()

    result = send_single_proto_to_bq(
        proto_obj = analysis_result_sid,
        proto_type = AnalysisResultSid,
        credentials = credentials,
        project_id = credentials.project_id,
        dataset_name = bq_conf['dataset_name'],
        table_name = bq_conf['table_name']
    )
    assert result == True
