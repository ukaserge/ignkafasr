from .protobuf import infer_pb2

class InferException(Exception):
    def __init__(self, *args, **kwargs) -> None:
        assert 'reqId' in kwargs
        assert 'userId' in kwargs
        
        self.reqId = kwargs['reqId']
        self.userId = kwargs['userId']
        
        self.info = "infer exception"
        if 'info' in kwargs:
            self.info = kwargs['info']
        
        super().__init__(*args)
    def __str__(self) -> str:
        message = infer_pb2.Infer()
        message.reqId = self.reqId
        message.inferResult = "FAIL"
        message.info = self.info
        message.userId = self.userId
        return message.SerializeToString().decode('utf-8')

class EmptyRegisterGroupException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "empty register group exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()

class InvalidAccessException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "invalid access exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()

class UnknownBugException(InferException):
    def __init__(self, *args, **kwargs) -> None:
        kwargs["info"] = "unknown bug exception"
        super().__init__(*args, **kwargs)
    
    def __str__(self) -> str:
        return super().__str__()