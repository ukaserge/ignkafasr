import {useEffect, useRef} from "react";
import RecordRTC, {RecordRTCPromisesHandler} from "recordrtc";
import styled from "styled-components";

import {
	invokeSaveAsDialog,
	StereoAudioRecorder,
	Options as RecordOptions,
} from "recordrtc";
import {
	useState,
	MutableRefObject,
	useTransition,
} from "react";
import { Button2, Layout } from "../components/common";


export const useRecorderPermission = (
    recordingOptions: RecordRTC.Options,
    onRejected?: (reason: any) => void,
    onFulfilled?: () => void
) => {
    let recorder = useRef<RecordRTCPromisesHandler>(null);

    useEffect(() => {
        async function getStream(): Promise<MediaStream> {
            return await (navigator as any).mediaDevices.getUserMedia({
                video: false,
                audio: true,
            })
        }

        getStream()
            .then((stream: MediaStream) => {
                console.log("getStream")
                recorder.current = new RecordRTCPromisesHandler(stream, recordingOptions)
                onFulfilled()
            })
            .catch(e => {
                console.log("getPerm fail");
                console.log(e);
            });
    }, []);

    return recorder;
};

export function RecorderButton(props) {
    console.log(props)
    const recordingOptions: RecordOptions = {
        type: "audio",
        mimeType: "audio/wav",
        recorderType: StereoAudioRecorder,
        numberOfAudioChannels: 1,
        desiredSampRate: 16000,
        timeSlice: 5000,
        ondataavailable: props.ondataavailable,
    };
    const recorder: MutableRefObject<RecordRTCPromisesHandler> =
        useRecorderPermission(
            recordingOptions,
            props.onLoadFail,
            () => {
                props.onLoaded()
            }
        );
    const [isRecordingStart, setIsRecordingStart] = useState(false);
    const buttonRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        console.log("recorderButton useEffect");
        console.log(recorder.current);
        console.log(recorder);
    });

    async function onStartRecording() {
        const successCallback = () => setIsRecordingStart(true);
        const failureCallback = () => console.log("ERRRRR");
        return await props.onStartRecording(recorder.current, successCallback, failureCallback);
    }
    async function onStopRecording() {
        const successCallback = () => {
            buttonRef.current.hidden = true;
        };
        const failureCallback = () => console.log("error!!");
        return await props.onStopRecording(
            recorder.current,
            successCallback,
            failureCallback
        );
    }

    return (
        <div>
            <ButtonRow style={{ display: "block" }} ref={buttonRef}>
                <Button2
                    size="small"
                    onClick={isRecordingStart ? onStopRecording : onStartRecording}
                >
                    {isRecordingStart ? "Stop Recording" : "Start Recording"}
                </Button2>
            </ButtonRow>
        </div>
    );
}

const ButtonRow = styled.div`
    display: flex;
    flex-direction: row;
    gap: 1.6rem;
    margin-bottom: 3.2rem;
`;
