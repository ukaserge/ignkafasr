import axios, {AxiosError, AxiosRequestConfig, AxiosResponse} from "axios";
import { invokeSaveAsDialog } from "recordrtc";
import Error from "next/error";
class RegistrationService {
    register = async (blob: Blob, uuid: string, label: string): Promise<AxiosResponse<any>> => {
        const IGNASR_SERVER = process.env.NEXT_PUBLIC_IGNASR_SERVER
        const uploadApiURL = `${IGNASR_SERVER}/api/speech/register`
        const withCredentials = !(IGNASR_SERVER.includes("http://localhost"));
        // const uploadApiURL = "https://ignasr.limdongjin.com/api/speech/register";

        const axiosReqConfig: AxiosRequestConfig = {
            headers: {
                "Content-Type": "multipart/form-data"
            },
            timeout: 1000000, // 100s
            withCredentials: withCredentials
        }

        const formData = new FormData();
        formData.append("name", uuid);
        formData.append("file", blob, "voice.wav");
        formData.append("label", label);

        // const timeBasedFileName = () => 'msr-' + (new Date).toISOString().replace(/:|\./g, '-') + '.wav'
        // invokeSaveAsDialog(blob, timeBasedFileName());

        return await axios.post(uploadApiURL, formData, axiosReqConfig);
    };
}

export default new RegistrationService();
