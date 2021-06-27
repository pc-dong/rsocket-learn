const {Flowable} = require('rsocket-flowable');
const {RSocketClient} = require('rsocket-core');
const RSocketTcpClient = require('rsocket-tcp-client').default;
const {
    BufferEncoders,
    encodeAndAddWellKnownMetadata,
    MESSAGE_RSOCKET_COMPOSITE_METADATA,
    MESSAGE_RSOCKET_ROUTING,
    encodeRoute,
    encodeCompositeMetadata
} = require('rsocket-core');

let _rsocket = null;

async function connect() {
    const transport = new RSocketTcpClient({host: '127.0.0.1', port: 8081}, BufferEncoders);
    const setup = {
        keepAlive: 5000,
        lifetime: 30000,
        dataMimeType: 'application/json',
        metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
    }
    const clint = new RSocketClient({setup, transport});
    _rsocket = await clint.connect();
}


function log(message) {
    const routeMetadata = encodeRoute('log');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    _rsocket.fireAndForget({data: Buffer.from(message, 'utf8'), metadata});
}

function toUpperCase(message) {
    const routeMetadata = encodeRoute('toUpperCase');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    const single = _rsocket.requestResponse({data: Buffer.from(message, 'utf8'), metadata});
    return single;
}

function channelToUpperCase(messages) {
    const routeMetadata = encodeRoute('channelToUpperCase');
    const metadata = encodeCompositeMetadata([
        [MESSAGE_RSOCKET_ROUTING, routeMetadata]
    ]
    );

    console.log(Array.isArray(messages));

    const request = Flowable.just(...messages).map(message => {
        console.log(message);
        return {
            data: Buffer.from(message, 'utf8'),
            metadata
        };
    });

    return _rsocket.requestChannel(request);
}

function splitString(message) {
    const routeMetadata = encodeRoute('splitString');
    const metadata = encodeAndAddWellKnownMetadata(
        Buffer.alloc(0),
        MESSAGE_RSOCKET_ROUTING,
        routeMetadata
    );

    const stream = _rsocket.requestStream({data: Buffer.from(message, 'utf8'), metadata});
    return stream;
}

connect().catch(error => console.error(error));

module.exports = {log, toUpperCase, splitString, channelToUpperCase}
