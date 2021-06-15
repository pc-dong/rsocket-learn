1. Start socat. It is used for emulation of transport disconnects

``socat TCP6-LISTEN:7001,fork,reuseaddr TCP6:localhost:7000`

2. start `ChannelEchoResume.main`

3. terminate/start socat periodically for session resumption
