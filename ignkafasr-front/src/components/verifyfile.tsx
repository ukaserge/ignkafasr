import { Spin } from "antd";
import {
	useEffect,
	useRef,
	useState,
	MutableRefObject,
	useTransition,
} from "react";
import styled from "styled-components";
import { Button2, Layout } from "./common";
import axios, { AxiosRequestConfig, AxiosResponse } from "axios";

import { useRecorderPermission, RecorderButton } from "@/hooks/useRecorder";
import { VerificationService } from "@/services";
import { v4 as uuidv4 } from "uuid";
import { Client, IMessage } from "@stomp/stompjs";
import { ConcatenateBlobs, sleep } from "@/helpers";
import { fail } from "assert";

export default function MainContent() {
	const TAB_LITERAL = "&nbsp; &nbsp; &nbsp; &nbsp";
	const successMessage = (result) => {
		return (
			"✅ You are a registered user in the system.<br>The Speaker Verification results:<br>" +
			TAB_LITERAL +
			result
		);
	};
	const stompClientRef = useRef<Client>(null);
	const isStompReady = useRef(false);
	const inferResultRef = useRef("");
	const currentStatusRef = useRef<HTMLDivElement>();
	const transcriptionRef = useRef<HTMLDivElement>();
	const tableRef = useRef<HTMLDivElement>();
	const isSuccess = useRef(false);
	const isFail = useRef(false);
	const isError = useRef(false);
	const userIdRef = useRef<string>("");
const inputFileRef = useRef();
	const blobUuids: string[] = [];
	const blobUuid2Idx = {};
	const idx2verify = {};
	const idx2transcription = {};
	const idx2label = {};
	const idx2score = {};
	const idx2info = {};

	let prevOffset = -1;
	let offset = 0;
	const [isSpin, setSpin] = useState(true);

    const recordingInfoMsg = () => {
        return "Your voice is currently being recorded....";
    }
    const errorInfoMsg = () => {
        return "🚨 An error occurred during processing: " + inferResultRef.current + ".. <br>Please refresh the page.. "
    }

    const processingMsg = () => {
        return "Processing..."
    }
    const onClickButton = async () => {
         let file = inputFileRef.current.files[0]

        const userId = userIdRef.current;
        const blobUuid = uuidv4();
        return await VerificationService.verifyFile(file, blobUuid, userId)
    }

	async function startRecording(recorder, successCallback, failureCallback) {
		console.log(isStompReady);
		console.log(stompClientRef.current);
		if (!isStompReady) {
			currentStatusRef.current.innerHTML += "<br>Wait a minute, please retry.";
			return;
		}
		currentStatusRef.current.innerHTML = recordingInfoMsg()
		
		await recorder
			.startRecording()
			.then((msg) => {
				console.log("execute start recording!");
				successCallback();
				// setIsRecordingStart(true);
				currentStatusRef.current.innerHTML = recordingInfoMsg();
				recorder
					.getState()
					.then((a) => {
						console.log("current recorder state: ");
						console.log(a);
					})
					.catch((e) => {
						console.log("ERR current recorder state: ");
						console.log(e);
					});
			})
			.catch((e) => {
				console.log("record start error");
				console.log(e);
				currentStatusRef.current.innerHTML = errorInfoMsg()
				failureCallback();
			});
	}

	async function stopRecording(recorder, successCallback, failureCallback) {
        await recorder
            .stopRecording()
            .then((blobUrl) => {
                console.log("execute stop recording!");
                console.log(blobUrl);
                currentStatusRef.current.innerHTML = processingMsg();

                // buttonRef.current.hidden = true;
                successCallback();
                executeAnalyze(blobUrl, () => {
                    console.log("error!!");
                    currentStatusRef.current.innerHTML = errorInfoMsg()
                });
            })
            .catch((e) => {
                currentStatusRef.current.innerHTML = errorInfoMsg()
                console.log(e);
                failureCallback();
            });
    }
	function onRecorderLoaded() {
		console.log("recorder load ok");
		currentStatusRef.current.innerHTML = "Please start recording.. ";
		setSpin(false);
	}

	function recorderPermissionOnRejected(e) {
        console.log("recorder.current.permission rejected");
        console.log(e);
    }

	function transcribeCallback(message: IMessage) {
        console.log(message);
        console.log(message.body);

        var tmp = message.body.split("\n");
        var reqId = tmp.filter((s) => s.startsWith("reqId:"))[0];
        reqId = reqId.slice(8, reqId.length - 1);
        var transcription = tmp.filter((s) => s.startsWith("transcription:"))[0];
        transcription = transcription.slice(15, transcription.length - 1);
        idx2transcription[blobUuid2Idx[reqId]] = transcription;
				
				let tmp2 = "";
				for(let i = 0; i < blobUuids.length; i++){
					if(idx2transcription[i] != undefined){
						tmp2 += idx2transcription[i]
					}
				}
				transcriptionRef.current.innerHTML = tmp2;
        console.log(idx2transcription);
    }
	function verifCallback(message: IMessage) {
        console.log(message);
        console.log(message.body);
        let result;
        if (message.body.includes("FAIL")) {
            result = "FAIL";
            console.log(message.body);
            if (!isFail.current) {
                isFail.current = true;
            }
            console.log(idx2verify);
            inferResultRef.current = message.body;
        } else {
            result = "OK";
            isSuccess.current = true;
            currentStatusRef.current.innerHTML = successMessage(message.body);
        }

        updateResultTable(message);
    }

	function updateResultTable(message: IMessage) {
        return;

		let result;
		if (message.body.includes("FAIL")) {
			result = "FAIL";
		} else {
			result = "OK";
		}
		var tmp = message.body.split("\n");
		let reqId = tmp.filter((s) => s.startsWith("reqId:"))[0];
		reqId = reqId.slice(8, reqId.length - 1);

		let idx = blobUuid2Idx[reqId];

		var tmp2 = tmp.filter((s) => s.startsWith("info:"));
		if (tmp2.length != 0) {
			let info = tmp2[0];
			info = info.slice(7, info.length - 1);
			idx2info[idx] = info;
		}
		tmp2 = tmp.filter((s) => s.startsWith("label:"));
		if (tmp2.length != 0) {
			let label = tmp2[0];
			label = label.slice(8, label.length - 1);
			idx2label[idx] = label;
		}
		tmp2 = tmp.filter((s) => s.startsWith("score:"));
		if (tmp2.length != 0) {
			let score = tmp2[0];
			score = score.slice(8, score.length - 1);
			idx2score[idx] = score;
		}
		console.log(reqId);
		idx2verify[blobUuid2Idx[reqId]] = result;
		console.log(blobUuid2Idx);
		console.log(idx2info);
		console.log(idx2verify);
		console.log(idx2label);
		console.log(idx2score);

		var content = "";
		for (let i = 0; i < blobUuids.length; i++) {
			content += "<tr>";
			content += "<td>" + i * 5 + "</td>";
			content += "<td>" + idx2verify[i] + "</td>";
			content += "<td>" + idx2label[i] + "</td>";
			content += "<td>" + idx2score[i] + "</td>";
			content += "<td>" + idx2info[i] + "</td>";
			// content += "<td>" + idx2transcription[i] + "</td>";
			content += "</tr>";
		}

		tableRef.current.innerHTML =
			"<table>" +
			"<thead><tr><th>time</th><th>result</th><th>label</th><th>score</th><th>info</th></tr></thead>" +
			"<tbody>" +
			content +
			"</tbody></table>";
	}
	const conf2 = {
		destination: "/user/topic/succ/transcribe",
		callback: transcribeCallback,
		headers: {},
	};
	const conf = {
		destination: "/user/topic/succ",
		callback: verifCallback,
		headers: {},
	};

	async function verifyBlob(blob) {
        // console.log(UUID.current)
        console.log("verifyBlob :");
        console.log(blob);

        const blobUuid = uuidv4();
        blobUuids.push(blobUuid);
        blobUuid2Idx[blobUuid] = blobUuids.length - 1;

        const userId = userIdRef.current;

        return await VerificationService.verify(blob, blobUuid, userId);
    }

	function recorderOnDataAvailable(blob: Blob) {
        prevOffset = offset;
        offset += blob.size;

        verifyBlob(blob)
            .then((axiosRes) => {
                isError.current = false;
            })
            .catch((e) => {
                console.log("dataava catch");
                console.log(e);
                // inferResultRef.current = e
                isError.current = true;
            })
            .finally(() => {
                // elementsUpdate()
            });
    }

	function stompErrorCallback(reason) {
        console.log("stomp error");
        console.log(reason);
    }

	async function initStopClient() {
		console.log("initStompClient!");
		if (userIdRef.current == undefined || userIdRef == null) {
			console.log("userIdRef.current is null");
			userIdRef.current = uuidv4();
		}
		await VerificationService.initStomp({
			userId: userIdRef.current,
			stompErrorCallback,
			confs: [conf, conf2]
		})
			.then((client) => {
				console.log("then!!!");
				stompClientRef.current = client;
				isStompReady.current = true;
				// startButtonRef.current.style.display = "block"
				// spinElemRef.current.style.display = "none"
				// elementsUpdate()
			})
			.catch((e) => {
				console.log("stomp init fail..");
				console.log(e);
				isStompReady.current = false;

				// spinElemRef.current.style.display = "none"
				// isError.current = true
				inferResultRef.current = "socket connection fail.." + e;
				currentStatusRef.current.innerHTML = errorInfoMsg()
				// elementsUpdate();
			});
	}
	useEffect(() => {
		console.log("useEffect");

		if (userIdRef.current == undefined || userIdRef.current === "") {
			userIdRef.current = uuidv4();
		}
		console.log(userIdRef.current);

		if (stompClientRef.current == undefined || stompClientRef.current == null) {
			initStopClient();
		}
	}, []);

	function executeAnalyze(blobUrl, errorCallback) {
        console.log("recording stop!");
        console.log(blobUrl);
        setSpin(true);

        fetch(blobUrl).then(async (response) => {
            const entireBlob = await response.blob();
            console.log(entireBlob);
            if (offset == 0) {
                verifyBlob(entireBlob)
                    .then((v) => {
                        console.log(v);
                    })
                    .catch((e) => {
                        console.log("dataava catch");
                        console.log(e);
                        errorCallback();
                        // isError.current = true
                        // elementsUpdate();
                        return undefined;
                    });
                return;
            }

            let prevBlob = entireBlob.slice(prevOffset, offset, "audio/wav");
            let lastBlob = entireBlob.slice(offset, entireBlob.size, "audio/wav");

            ConcatenateBlobs([prevBlob, lastBlob], "audio/wav", (cBlob) => {
                console.log(cBlob);
                verifyBlob(cBlob)
                    .then((v) => console.log(v))
                    .catch((e) => {
                        console.log("dataava catch");
                        console.log(e);
                        errorCallback();
                        // isError.current = true
                        // elementsUpdate();
                        return undefined;
                    });
            });
        });
    }


	return (
		<>
			<Layout height={540} style={{ padding: "3.2rem 0 0 0" }}>
				<Title>Verify Voice</Title>
				<div style={{ height: "4rem" }} />
				<Status2 id="usage" ref={currentStatusRef}></Status2>
				<Status2 ref={transcriptionRef} />
				{isSpin && <Spin />}

				<div style={{ height: "4rem" }} />
                <input type="file" ref={inputFileRef} />
                <button type="button" onClick={onClickButton}> Submit </button>
				<Status2 ref={tableRef}></Status2>
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
