import { Spin } from "antd";
import {useEffect, useRef, MutableRefObject, useState } from "react";
import styled from "styled-components";
import { Button2, Layout} from "./common";
import axios, {AxiosError, AxiosRequestConfig, AxiosResponse} from "axios";
// import { StereoAudioRecorder, RecordRTCPromisesHandler, Options as RecordOptions } from "recordrtc";
// import { useRecorderPermission } from "@/hooks/useRecorder";
import { v4 as uuidv4 } from 'uuid';
import RegistrationService from "@/services/registration.service";
import { ConcatenateBlobs } from "@/helpers";
import { RecorderButton } from "@/hooks/useRecorder";

export default function MainContent() {
    const UUID = useRef<string>("");
    // const isRecording = useRef(false);
    // const isAnalyzing = useRef(false);
    // const isSuccess = useRef(false);
    // const isError = useRef(false);
    const registeredUuid = [];
    // let accBlob: Blob;
    // let prevBlob: Blob;
    // const audioElemRef = useRef<HTMLAudioElement>(null);
    // const spinElemRef = useRef(null);
    // const statusElemRef = useRef(null);
    // const startButtonRef = useRef(null);
    // const stopButtonRef = useRef(null);
    const [isSpin, setSpin] = useState(true);
    const labelRef = useRef<HTMLInputElement>(null);
    const errorMsg = useRef("");
	const currentStatusRef = useRef<HTMLDivElement>();
    let prevOffset = -1;
    let offset = 0;

    function onRecorderLoaded(){
        console.log("recorder load ok");
        currentStatusRef.current.innerHTML = "Please enter your name and start recording.";
        setSpin(false);
    }
    
    const registerBlob: (blob) => Promise<AxiosResponse<any>> = async (blob) => {
        // console.log(UUID.current)
        console.log("registerBlob :")
        console.log(blob)
        const blobUuid = uuidv4()
        const userName: string = labelRef.current.value;

        if(userName?.trim() == undefined || userName.trim() == ""){
            alert("wrong access");
            return
        }

        registeredUuid.push(blobUuid)
        return await RegistrationService.register(blob, blobUuid, userName)
    }

    const recorderOnDataAvailable = (blob: Blob) => {
        prevOffset = offset
        offset += blob.size
        setSpin(true);
        registerBlob(blob)
            .then((axiosRes) => {
                // isError.current = false;
                errorMsg.current = ""
            })
            .catch(e => {
                console.log("dataava catch")
                console.log(e)
                errorMsg.current = e
                // isError.current = true
            })
            .finally(() => {
                setSpin(false);
            })
    }
    
    const recorderPermissionOnRejected = (e) => {
        console.log("recorder permission rejected");
        console.log(e)
        // isError.current = true
        // elementsUpdate()
    }

    useEffect(() => {
        if(UUID.current == undefined || UUID.current == "" || UUID.current == null){
            UUID.current = uuidv4();
        }
        // elementsUpdate();
    }, []);

    const startRecording = async (recorder, successCallback, failureCallback) => {
        console.log(labelRef.current.value)

        if(labelRef.current.value === null || labelRef.current.value.trim() === ""){
            alert("Enter your name!");
            return
        }

        console.log("recording start!")
        labelRef.current.disabled = true
        successCallback()
        
        await recorder
        .startRecording()
        .then(() => {
                // isRecording.current = true
                currentStatusRef.current.innerHTML = recordingInfoMsg()
            })
            .catch((e) => {
                console.log("record start error")
                console.log(e)
                errorMsg.current = e
                // isRecording.current = false;
                // isError.current = true;
                currentStatusRef.current.innerHTML = registerFailMsg()
                failureCallback()
            })
    }
    
    const recordingInfoMsg = () => {
        return "Hello, " + labelRef.current.value + ".<br>Your voice is currently being recorded.";
    }
    const registerFailMsg = () => {
        return "🚨 An error occurred during processing: " + errorMsg.current + ".. <br>Please refresh the page.. "
    }
    const registerOkMsg = () => {
        return "✅ Hello, " + labelRef.current.value + ".<br>Your voice registration completed. ";
    }
    const processingMsg = () => {
        return "Processing..."
    }
    const stopRecording = async (recorder, successCallback, failureCallback) => {
        console.log("recording stop!");
        successCallback()
        if(labelRef.current.value == null || labelRef.current.value.trim() == ""){
            alert("wrong access");
            return
        }
        console.log("stop deb0")
        recorder.pauseRecording()
        console.log("stop deb1")

        await recorder.stopRecording()
            .then((blobUrl) => {
                console.log(blobUrl);
                currentStatusRef.current.innerHTML = processingMsg()
                
                fetch(blobUrl)
                    .then(async (response) => {
                        const entireBlob = await response.blob();
                        const nextStatus = registerOkMsg();
                        console.log(entireBlob)

                        if(offset == 0){
                            registerBlob(entireBlob)
                                .then((v) => {
                                    console.log(v)
                                    currentStatusRef.current.innerHTML = nextStatus
                                })
                                .catch((e) => {
                                    console.log("dataava catch");
                                    console.log(e);
                                    currentStatusRef.current.innerHTML = registerFailMsg()
                                    failureCallback();
                                    // isError.current = true
                                    // elementsUpdate();
                                    return undefined;
                                });
                            return;
                        }
                                    
                        // let prevBlob = entireBlob.slice(
                        //     prevOffset,
                        //     offset,
                        //     "audio/wav"
                        // )
                        let lastBlob = entireBlob.slice(
                            prevOffset,
                            entireBlob.size,
                            "audio/wav"
                        );

                        console.log(lastBlob);
                        registerBlob(lastBlob)
                            .then((v) => {
                                console.log(v)
                                currentStatusRef.current.innerHTML = nextStatus
                                successCallback()
                            })
                            .catch((e) => {
                                console.log("dataava catch");
                                console.log(e);
                                currentStatusRef.current.innerHTML = registerFailMsg()
                                failureCallback();
                                // isError.current = true
                                // elementsUpdate();
                                return undefined;
                            });
                        // ConcatenateBlobs([prevBlob, lastBlob], "audio/wav", (cBlob) => {
                        // });
                    })
                // isRecording.current = false;
                // isAnalyzing.current = false;
                // isSuccess.current = true;
            })
            .catch((e) => {
                console.log(e)
                errorMsg.current = e
                currentStatusRef.current.innerHTML = registerFailMsg()
                // isRecording.current = false;
                // isAnalyzing.current = false;
                // isError.current = true
            })
        ;
    }

    return (
        <>
            <Layout height={540} style={{ padding: "3.2rem 0 0 0" }}>
                <Title>Register Voice</Title>
                <div style={{ height: "4rem" }} />
                <Status2 id="usage" ref={currentStatusRef}></Status2>
                
                { isSpin && <Spin /> }
                {/* <Status2 ref={spinElemRef} style={{ display: "none" }}>
                    <div>
                        <Spin style={{ marginRight: "1.2rem" }} /> Processing ...
                        <Spin style={{ marginLeft: "1.2rem" }} />
                    </div>
                </Status2> */}
                <div style={{ height: "4rem" }}>
                    <input type="text" ref={labelRef} placeholder="Enter your name"/>
                </div>
                <RecorderButton 
                    onLoaded={onRecorderLoaded}
                    onLoadFail={recorderPermissionOnRejected}
                    ondataavailable={recorderOnDataAvailable}
                    onStartRecording={startRecording}
                    onStopRecording={stopRecording}
                />
            </Layout>
        </>
    );
}

const Title = styled.p`
  margin-bottom: 1.2rem;

  font-size: 3.6rem;
  font-weight: 500;
  color: ${({ theme }) => theme.colors.gray1};
`;

const Status = styled.p`
  margin-bottom: 1.6rem;

  font-size: 2rem;
  font-weight: 500;
  color: ${({ theme }) => theme.colors.gray5};
`;

const Status2 = styled.div`
  margin-bottom: 1.6rem;

  font-size: 2rem;
  font-weight: 500;
  color: ${({ theme }) => theme.colors.gray5};
`;

const ButtonRow = styled.div`
  display: flex;
  flex-direction: row;
  gap: 1.6rem;

  margin-bottom: 3.2rem;
`;

