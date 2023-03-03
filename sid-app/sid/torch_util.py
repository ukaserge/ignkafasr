import io
import torch

def torch_to_blob(tensor_obj):
    bo = io.BytesIO()
    torch.save(tensor_obj, bo)
    bo.seek(0)
    blob = bo.read()
    bo.close()
    return blob

def blob_to_torch(blob):
    bo = io.BytesIO(blob)
    tensor_obj = torch.load(bo)
    bo.close()
    return tensor_obj

def to_numpy(tensor_obj: torch.Tensor):
    return tensor_obj.detach().cpu().numpy()

