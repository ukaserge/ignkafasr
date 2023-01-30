import {Client, IMessage, StompConfig, StompHeaders} from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import axios, {AxiosRequestConfig, AxiosResponse} from "axios";

interface SubscriptionConf {
  destination: string,
  callback: (IMessage) => void,
  headers: StompHeaders
}

interface AnalyzeParams {
  userId: string;
  stompErrorCallback: (msg: string) => void;
  confs: any
}

class VerificationService {
  verify: (blob: Blob, uuid: string, userId: string) => Promise<AxiosResponse<any>> =
      async (blob: Blob, uuid: string, userId: string) => {
    const IGNASR_SERVER = process.env.NEXT_PUBLIC_IGNASR_SERVER
    const uploadApiURL = `${IGNASR_SERVER}/api/speech/upload`
    let withCredentials = !(IGNASR_SERVER.includes("http://localhost"))
    // const uploadApiURL = "https://ignasr.limdongjin.com/api/speech/upload"

    const axiosReqConfig: AxiosRequestConfig = {
      headers: {
        "Content-Type": "multipart/form-data"
      },
      timeout: 200000, 
      withCredentials: withCredentials
    }

    const formData = new FormData();
    formData.append("name", uuid);
    formData.append("file", blob, "voice.wav");
    formData.append("label", userId);
    formData.append("userId", userId);

    return await axios.post(uploadApiURL, formData, axiosReqConfig);
  };

  // TODO refactoring
  async initStomp({userId, stompErrorCallback, confs}: AnalyzeParams) {
    let stompClient: Client;
    const STOMASR_SERVER = process.env.NEXT_PUBLIC_STOMASR_SERVER
    const url = `${STOMASR_SERVER}/ws-stomp`
    const sock = new SockJS(url, null, {
      // transports: ["websocket", "xhr-streaming", "xhr-polling"]
      transports: ["xhr-polling"]
    })

    // const url = "https://stomasr.limdongjin.com/ws-stomp";
     stompClient = new Client({
      webSocketFactory: () => sock,
      debug: (msg) => console.log(msg),
      connectHeaders: {
        user: userId
      },
      reconnectDelay: 200,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: (frame) => {
        console.log(frame)
        confs.forEach(conf => {
          stompClient?.subscribe(conf.destination, conf.callback, conf.headers)
        })
        stompClient?.publish({
          destination: "/app/join",
          headers: {},
          body: JSON.stringify({targetUserName: userId, message: userId})
        });
      },
      onDisconnect: (e) => {
        console.log("stomp disconnect")
        console.log(e)
      },
      onStompError: (e) => {
        console.log("stomp error")
        console.log(e)
      },
      onWebSocketClose: (e: CloseEvent) => {
        console.log("websocket close")
        if (e.code === 1002) {
          // https://learn.microsoft.com/ko-kr/dotnet/api/system.net.websockets.websocketclosestatus?view=net-6.0
          console.log("protocol error; check CORS; ")
          stompErrorCallback(e.reason);
          // stompClient.deactivate()
        }
        
        console.log(e)
      }
    });

    stompClient.activate();

    // await new Promise((resolve) => setTimeout(resolve, 500)); // 0.5초

    return stompClient
  }
}

export default new VerificationService();
