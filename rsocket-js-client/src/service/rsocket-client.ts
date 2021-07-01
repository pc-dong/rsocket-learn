import {Flowable} from 'rsocket-flowable';
import {RSocketClient} from 'rsocket-core';
import RSocketWebSocketClient from 'rsocket-websocket-client';
import {ReactiveSocket} from 'rsocket-types'
import {
    BufferEncoders,
    encodeAndAddWellKnownMetadata,
    MESSAGE_RSOCKET_COMPOSITE_METADATA,
    MESSAGE_RSOCKET_ROUTING,
    encodeRoute,
    encodeCompositeMetadata
} from 'rsocket-core';

let _rsocket: ReactiveSocket<any, any>;

async function connect() {
    console.log('begin connect');
    const transport = new RSocketWebSocketClient({url: "ws://127.0.0.1:8081"}, BufferEncoders);
    const setup = {
        keepAlive: 5000,
        lifetime: 30000,
        dataMimeType: 'application/json',
        metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
    }
    const clint = new RSocketClient({setup, transport});
    _rsocket = await clint.connect();
    console.log('connected');
}


const log = (message: string) => {
    const routeMetadata = encodeRoute('log');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    _rsocket.fireAndForget({data: Buffer.from(message, 'utf8'), metadata});
}

const toUpperCase = (message: string) => {
    const routeMetadata = encodeRoute('toUpperCase');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    return _rsocket.requestResponse({data: Buffer.from(message, 'utf8'), metadata});
}

const channelToUpperCase = (messages: string[]) => {
    const routeMetadata = encodeRoute('channelToUpperCase');
    const metadata = encodeCompositeMetadata([
            [MESSAGE_RSOCKET_ROUTING, routeMetadata]
        ]
    );

    console.log(Array.isArray(messages));

    const request = Flowable.just(...messages)
        .map((message: string) => {
            console.log(message);
            return {
                data: Buffer.from(message, 'utf8'),
                metadata
            };
        });

    return _rsocket.requestChannel(request);
}

const splitString = (message: string) => {
    const routeMetadata = encodeRoute('splitString');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    return _rsocket.requestStream({data: Buffer.from(message, 'utf8'), metadata});
}

const isConnected = () => !_rsocket

connect().catch(error => console.error(error));

const rsocketClient = {connect, isConnected, log, toUpperCase, splitString, channelToUpperCase}

export default rsocketClient;