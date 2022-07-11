import {
    RSocketConnector,
    RSocket,
} from 'rsocket-core';

import {
    encodeCompositeMetadata,
    encodeRoute,
    WellKnownMimeType,
} from "rsocket-composite-metadata";
import {WebsocketClientTransport} from 'rsocket-websocket-client';
// import MESSAGE_RSOCKET_ROUTING = WellKnownMimeType.MESSAGE_RSOCKET_ROUTING;

import {
    Cancellable,
    OnExtensionSubscriber,
    OnNextSubscriber,
    OnTerminalSubscriber,
    Requestable
} from "rsocket-core/dist/RSocket";
const MESSAGE_RSOCKET_COMPOSITE_METADATA = WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA;
const MESSAGE_RSOCKET_ROUTING = WellKnownMimeType.MESSAGE_RSOCKET_ROUTING;

let _rsocket: RSocket;
let isRsocketClosed = true


function makeConnector() {
    return new RSocketConnector({
        setup: {
            keepAlive: 5000,
            lifetime: 30000,
            dataMimeType: 'application/json',
            metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
        },
        transport: new WebsocketClientTransport({
            url: "ws://localhost:8081",
            wsCreator: (url) => new WebSocket(url) as any,
        })
    });
}

async function getRsocket() {
    console.log('begin connect');
    console.log(_rsocket);
    if (_rsocket && !isRsocketClosed) {
        return _rsocket
    }

    _rsocket = await makeConnector().connect()
    isRsocketClosed = false;
    _rsocket.onClose(() => isRsocketClosed = true)
    console.log('_rsocket');
    console.log('connected');

    return _rsocket
}


const log = async (message: string) => {
    const encodedRoute = encodeRoute('log');
    const map = new Map<WellKnownMimeType, Buffer>();
    map.set(MESSAGE_RSOCKET_ROUTING, encodedRoute);
    const compositeMetaData = encodeCompositeMetadata(map);

    return new Promise((resolve, reject) => getRsocket()
        .then(rsocket => rsocket.fireAndForget({data: Buffer.from(message, 'utf8'), metadata: compositeMetaData},
            {
                onError: (e) => {
                    reject(e)
                },
                onComplete: () => {
                    resolve(null)
                }
            }
        )).catch(err => reject(err))
    )
}

const toUpperCase = async (message: string, callback: OnTerminalSubscriber & OnNextSubscriber & OnExtensionSubscriber) => {
    const encodedRoute = encodeRoute('toUpperCase');
    const map = new Map<WellKnownMimeType, Buffer>();
    map.set(MESSAGE_RSOCKET_ROUTING, encodedRoute);
    const metadata = encodeCompositeMetadata(map);

    const rsocket = await getRsocket()
    return rsocket.requestResponse({data: Buffer.from(message, 'utf8'), metadata}, callback);
}

const channelToUpperCase = async (messages: string[], callBack: OnTerminalSubscriber & OnNextSubscriber & OnExtensionSubscriber & Cancellable) => {
    const routeMetadata = encodeRoute('channelToUpperCase');
    const metadata = encodeCompositeMetadata([
            [MESSAGE_RSOCKET_ROUTING, routeMetadata]
        ]
    );

    console.log(Array.isArray(messages));
    // const request = Flowable.just(...messages)
    //     .map((message: string) => {
    //         console.log(message);
    //         return {
    //             data: Buffer.from(message, 'utf8'),
    //             metadata
    //         };
    //     });
    // let buffer = new ArrayBuffer( messages.length);
    // for(var i = 0; i< messages.length; i++) {
    //     buffer[i] = Buffer.from(messages[i], 'utf8')
    // }

    const rsocket = await getRsocket()
    let send = 0;
    let interval:any = undefined;

    const requester = rsocket.requestChannel({
            data: Buffer.from(messages[send]),
            metadata
        }, 1, false, {...callBack, request: (n) => {
            console.log(`request(${n})`);
            if (!interval) {
                interval = setInterval(() => {
                    send++;
                    if (send >= messages.length - 1)  {
                        clearInterval(interval);
                        interval = undefined;
                    }
                    requester.onNext(
                        {
                            data: Buffer.from(messages[send]),
                        },
                        send >= messages.length - 1
                    );
                }, 100);
            }
        }})

    return requester
}

const splitString = async (message: string, responder: OnTerminalSubscriber & OnNextSubscriber & OnExtensionSubscriber) => {
    const encodedRoute = encodeRoute('splitString');
    const map = new Map<WellKnownMimeType, Buffer>();
    map.set(MESSAGE_RSOCKET_ROUTING, encodedRoute);
    const metadata = encodeCompositeMetadata(map);

    const rsocket = await getRsocket()

    return rsocket.requestStream({data: Buffer.from(message, 'utf8'), metadata},
        100,
        responder);
}

const isConnected = () => !_rsocket

const rsocketClient = {isConnected, log, toUpperCase, splitString, channelToUpperCase}

export default rsocketClient;
