import rsocketClient from './service/rsocket-client'
import React, {useState} from "react";
import { Payload } from 'rsocket-core';
import {Requestable} from "rsocket-core/dist/RSocket";


function Rsocket() {
    const [items, setItems] = useState(['']);
    const [log, setLog] = useState('');
    const [upperCases, setUpperCases] = useState(['']);
    const upperItems: string[] = [];
    const  loadItems = async () => {
        console.log('loadItems() called')
        const message = "Hello Rsocket!"
        const array: string[] = [];
        rsocketClient.splitString(message, {
            onError: (error: Error) => console.error(error),
            onNext: (payload: any) => {
                array.push(payload.data.toString());
                setItems(array.concat());
            },
            onComplete: () => console.log("complete() called"),
            onExtension(extendedType: number, content: Buffer | null | undefined, canBeIgnored: boolean) {
            }
        }).then(requester => {
            requester.request(100);
        }).catch(err => console.log(err))

    }

    const  doLog = async () => {
        console.log('doLog() called')
        const message = "Hello Rsocket!"
        const array: string[] = [];
        rsocketClient.log(message)
            .then(() => setLog("log Hello Rsocket!"))
            .catch((err) =>  setLog("log failed"))
    }

    const toUpperCase = async () => {
        console.log('toUpperCase() called')
        const message = "Hello Rsocket!"
        rsocketClient.toUpperCase(message, {
            onNext(payload: Payload, isComplete: boolean) {
                upperItems.push(payload?.data ? JSON.parse(payload.data.toString()).message : "");
                console.log(upperItems)
                setUpperCases(upperItems);
            },
            onComplete() {
            },
            onError(message) {

            },
            onExtension(extendedType: number, content: Buffer | null | undefined, canBeIgnored: boolean) {
            }

        }).then(responder => {
            console.log(responder);
        }).catch(err => {
            console.log(err)
            upperItems.push("failed");
            setUpperCases(upperItems);
        })

    }

    const channelToUpperCase = async () => {
        console.log('toUpperCase() called')
        const message = ["Hello", "Rsocket!"]
        const array: string[] = [];
        let requester: Requestable;
        rsocketClient.channelToUpperCase(message, {
            onError: (error: Error) => console.error(error),
            onNext: (payload: any) => {
                array.push(JSON.parse(payload?.data.toString())?.message);
                setUpperCases(array.concat());
                requester.request(1)
            },
            onComplete: () => console.log("complete() called"),
            onExtension(extendedType: number, content: Buffer | null | undefined, canBeIgnored: boolean) {
            },
            cancel: () => {
                // requester.cancel()
            }
        }).then(responder => {
            requester = responder
            console.log(responder);
        }).catch(err => console.log(err))

    }

    return (
        <div className="Rsocket" >
            <div>
                <button type="button" onClick={doLog}>log [Hello Rsocket!]</button>
                {logMessage(log)}
            </div>
            <div>
            <button type="button" onClick={loadItems}>split [Hello Rsocket!]</button>
                {generateItems(items)}
            </div>
            <div>
                <button type="button" onClick={toUpperCase}>toUpperCase [Hello Rsocket!]</button>
                <button type="button" onClick={channelToUpperCase}>channelToUpperCase [Hello Rsocket!]</button>
                {generateItems(upperCases)}
            </div>
        </div>
    )
}



function generateItems(items: string[]) {
   return  items.map((item, i) => {
        return (<li key={i}>{ item }</li>);
    });
}

function logMessage(log: string) {
        return (<li key='1'>{ log }</li>);
}

export default Rsocket
