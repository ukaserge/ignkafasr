export const ConcatenateBlobs = (blobs, type, callback) => {
    let buffers = [];
    let index = 0;

    const readAsArrayBuffer = () => {
        if (!blobs[index]) {
            return concatenateBuffers();
        }
        const reader = new FileReader()
        reader.onload = function(event) {
            buffers.push(event.target.result);
            index++;
            readAsArrayBuffer();
        };
        reader.readAsArrayBuffer(blobs[index]);
    }

    readAsArrayBuffer();

    const audioLengthTo32Bit = n => {
        let floorN = Math.floor(n);
        let b1 = floorN & 255
        let b2 = (floorN >> 8) & 255
        let b3 = (floorN >> 16) & 255
        let b4 = (floorN >> 24) & 255

        return [b1, b2, b3, b4];
    }

    function concatenateBuffers() {
        let byteLength = 0
        buffers.forEach(buffer => byteLength += buffer.byteLength);

        let tmp = new Uint8Array(byteLength)
        let lastOffset = 0
        let newData
        buffers.forEach(buffer => {
            if (type==='audio/wav' && lastOffset >  0) newData = new Uint8Array(buffer, 44);
            else newData = new Uint8Array(buffer);
            tmp.set(newData, lastOffset);
            lastOffset += newData.length;
        });
        if (type ==='audio/wav') {
            tmp.set(audioLengthTo32Bit(lastOffset - 8), 4);
            tmp.set(audioLengthTo32Bit(lastOffset - 44), 40); // update audio length in the header
        }
        let blob = new Blob([tmp.buffer], {
            type: type
        })
        callback(blob);
    }
}
