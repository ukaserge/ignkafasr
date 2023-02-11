import { Spin } from "antd";
import {useEffect, useRef, MutableRefObject, useState } from "react";
import styled from "styled-components";
import { Button2, Layout} from "./common";
import axios, {AxiosError, AxiosRequestConfig, AxiosResponse} from "axios";
import { v4 as uuidv4 } from 'uuid';
import RegistrationService from "@/services/registration.service";

export default function MainContent() {
    const UUID = useRef<string>("");
    const registeredUuid = [];
    const [isSpin, setSpin] = useState(true);
    const labelRef = useRef<HTMLInputElement>(null);
    const errorMsg = useRef("");
	const currentStatusRef = useRef<HTMLDivElement>();
    const inputFileRef = useRef();

    let prevOffset = -1;
    let offset = 0;
    const onClickButton = () => {
        console.log(inputFileRef)
        let file = inputFileRef.current.files[0]
        // console.log(file)

        return registerFile(file)
    }
    const registerFile: (blob) => Promise<AxiosResponse<any>> = async (blob) => {
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
        return await RegistrationService.registerFile(blob, blobUuid, userName)
    }

    useEffect(() => {
        if(UUID.current == undefined || UUID.current == "" || UUID.current == null){
            UUID.current = uuidv4();
        }
    }, []);

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

    return (
        <>
            <Layout height={540} style={{ padding: "3.2rem 0 0 0" }}>
                <Title>Register Voice</Title>
                <div style={{ height: "4rem" }} />
                <Status2 id="usage" ref={currentStatusRef}></Status2>
                
                { isSpin && <Spin /> }
                <div style={{ height: "4rem" }}>
                    <input type="text" ref={labelRef} placeholder="Enter your name"/>
                </div>
                <input type="file" id="input" ref={inputFileRef} />
                <button type="button" onClick={onClickButton}>Submit</button>
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

