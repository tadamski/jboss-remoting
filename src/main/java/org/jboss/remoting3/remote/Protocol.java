/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.remoting3.remote;

/**
 * The "remote" protocol.  Use this class to create an instance of the connection provider for the "remote" protocol.
 */
final class Protocol {

    // Behavior flags

    /**
     * Message close protocol flag.  If {@code true}, the remote side knows how to participate in proper async
     * close protocol; if {@code false}, just wing it and hope for the best.
     */
    static final int BH_MESSAGE_CLOSE = 1 << 0;

    /**
     * Bad message size flag.  If {@code true}, we know that the remote side is going to do the following:
     *
     * 1) Open the internal inbound window size by 8 bytes too many per acknowledged incoming message... and kill the
     *    connection if more than 2**29 messages are received due to the window "flipping" to negative values
     * 2) Send an ack/window open which is sized 8 bytes too large per incoming message
     * 3) Shrink its internal outbound window size by 8 bytes too many per outgoing message
     *
     * To compensate we must:
     *
     * 1) Avoid "crashing" the window which will gradually open too much over time
     * 2) Shrink our internal outbound window counter by an extra 8 bytes per outbound message
     * 3) Send message ACKs which are oversized by an extra 8 bytes per inbound message to prevent the window from sliding shut over time
     */
    static final int BH_FAULTY_MSG_SIZE = 1 << 1;

    /**
     * The highest-supported version of the remote protocol supported by this implementation.
     */
    static final byte VERSION = 1;

    // Message types

    /**
     * Sent by server only.
     * byte 0: GREETING
     * byte 1..n: greeting message body
     */
    static final byte GREETING = 0;
    /**
     * Sent by client then server.
     * byte 0: CAPABILITIES
     * byte 1..n: capabilities summary
     */
    static final byte CAPABILITIES = 1;
    /**
     * Sent by client
     * byte 0: AUTH_REQUEST
     * byte 1..n: request body
     */
    static final byte AUTH_REQUEST = 2;
    /**
     * Sent by server
     * byte 0: AUTH_CHALLENGE
     * byte 1..n: challenge body
     */
    static final byte AUTH_CHALLENGE = 3;
    /**
     * Sent by client
     * byte 0: AUTH_RESPONSE
     * byte 1..n: response body
     */
    static final byte AUTH_RESPONSE = 4;
    /**
     * Sent by server
     * byte 0: AUTH_COMPLETE
     * byte 1..n: final response body
     */
    static final byte AUTH_COMPLETE = 5;
    /**
     * Sent by server
     * byte 0: AUTH_REJECTED
     */
    static final byte AUTH_REJECTED = 6;
    /**
     * Sent by client then server
     * byte 0: STARTTLS
     */
    static final byte STARTTLS = 7;
    /**
     * Sent by server when message is not understood or invalid.
     * byte 0: NAK
     */
    static final byte NAK = 8;

    // Messages for opening channels

    // Channel are bidirectional thus each side's ID namespace is intermingled
    // if local in origin, read 0 MSb, write 1 MSb
    // if remote in origin, read 1 MSb, write 0 MSb

    /**
     * byte 0: CHANNEL_OPEN_REQUEST
     * byte 1..4: new channel ID (MSb = 1)
     * byte n+1..m: requested parameters (see Channel Open Parameters below)
     *    {@link #O_SERVICE_NAME} is required
     *    inbound = responder->requester
     *    outbound = requester->responder
     */
    static final byte CHANNEL_OPEN_REQUEST = 0x10;
    /**
     * byte 0: CHANNEL_OPEN_ACK
     * byte 1..4: channel ID (MSb = 0)
     * byte 5..n: agreed parameters (see Channel Open Parameters below)
     *    inbound = responder->requester
     *    outbound = requester->responder
     */
    static final byte CHANNEL_OPEN_ACK = 0x11;
    /**
     * byte 0: SERVICE_NOT_FOUND
     * byte 1..4: channel ID (MSb = 0)
     */
    static final byte SERVICE_NOT_FOUND = 0x12;
    /**
     * byte 0: SERVICE_ERROR
     * byte 1..4: channel ID (MSb = 0)
     * byte 5..n: reason UTF8
     */
    static final byte SERVICE_ERROR = 0x13;

    // Messages for managing channels

    /**
     * byte 0: CHANNEL_CLOSE_WRITE
     * byte 1..4: channel ID
     *
     * Sent when channel writes are shut down.
     */
    static final byte CHANNEL_SHUTDOWN_WRITE = 0x20;
    /**
     * byte 0: CHANNEL_CLOSE_READ
     * byte 1..4: channel ID
     *
     * Sent when a channel is closed without necessarily consuming all of its incoming messages.  Tell the sending side
     * to drop and close all in-progress messages, and refuse new ones.
     */
    static final byte CHANNEL_CLOSED = 0x21;

    // Messages for handling channel messages
    // Messages are unidirectional thus each side's ID namespace is distinct

    /**
     * byte 0: MESSAGE_DATA
     * byte 1..4: channel ID
     * byte 5..6: message ID
     * byte 7: flags: - - - - - C N E  C = Cancelled N = New E = EOF
     * byte 8..n: message content
     *
     * Always flows from message sender to message recipient.
     */
    static final byte MESSAGE_DATA = 0x30;
    /**
     * byte 0: MESSAGE_WINDOW_OPEN
     * byte 1..4: channel ID
     * byte 5..6: message ID
     * byte 7..10: window open amount
     *
     * Always flows from message recipient to message sender.
     */
    static final byte MESSAGE_WINDOW_OPEN = 0x31;
    /**
     * byte 0: MESSAGE_CLOSE
     * byte 1..4: channel ID
     * byte 5..6: message ID
     *
     * Always flows from message recipient to message sender.
     */
    static final byte MESSAGE_CLOSE = 0x32;

    // Messages for app authentication

    /**
     * byte 0: APP_AUTH_REQUEST (client -> server)
     * byte 1..4: identity (!= 0 or 1)
     * byte 5: mech name length in bytes (unsigned 1-256)
     * byte 6..n: mech name
     * byte n+1..m: initial response
     */
    static final byte APP_AUTH_REQUEST = 0x40;

    /**
     * byte 0: APP_AUTH_CHALLENGE (server -> client)
     * byte 1..4: identity from auth request
     * byte 5..n: challenge body
     */
    static final byte APP_AUTH_CHALLENGE = 0x41;

    /**
     * byte 0: APP_AUTH_RESPONSE (client -> server)
     * byte 1..4: identity from auth request
     * byte 5..n: response body
     */
    static final byte APP_AUTH_RESPONSE = 0x42;

    /**
     * byte 0: APP_AUTH_SUCCESS (server -> client)
     * byte 1..4: identity from auth request
     * byte 5..n: final challenge (update) body (0 = null)
     */
    static final byte APP_AUTH_SUCCESS = 0x43;

    /**
     * byte 0: APP_AUTH_REJECT (server -> client)
     * byte 1..4: identity from auth request
     */
    static final byte APP_AUTH_REJECT = 0x44;

    /**
     * byte 0: APP_AUTH_DELETE (client -> server)
     * byte 1..4: identity from auth request
     */
    static final byte APP_AUTH_DELETE = 0x45;

    /**
     * byte 0: APP_AUTH_DELETE_ACK (server -> client)
     * byte 1..4: identity from auth request
     *
     * Must only be sent in response to an APP_AUTH_DELETE else spurious failures may result.
     */
    static final byte APP_AUTH_DELETE_ACK = 0x46;

    // Messages for handling connection status

    /**
     * byte 0: CONNECTION_ALIVE
     * byte 1..n: random padding (optional)
     */
    static final byte CONNECTION_ALIVE = (byte) 0xF0;
    /**
     * byte 0: CONNECTION_ALIVE
     * byte 1..n: random padding (optional)
     */
    static final byte CONNECTION_ALIVE_ACK = (byte) 0xF1;
    /**
     * byte 0: CONNECTION_CLOSE
     *
     * No packets may be sent afterwards.  Connection is closed when message is sent and received.
     */
    static final byte CONNECTION_CLOSE = (byte) 0xFF;

    // Channel open parameters

    /**
     * End of parameters; no content.
     */
    static final int O_END = 0;
    /**
     * Service name; mandatory utf8 content.
     */
    static final int O_SERVICE_NAME = 1;
    /**
     * Max inbound message window size; mandatory uint31 content.
     * On channel open requests, this is inbound from the requester (client) viewpoint.
     * On channel open replies, this is inbound from the requester (client) viewpoint.
     */
    static final int O_MAX_INBOUND_MSG_WINDOW_SIZE = 0x80;
    /**
     * Max requester-bound message count; mandatory uint16 content.
     */
    static final int O_MAX_INBOUND_MSG_COUNT = 0x81;
    /**
     * Max responder-bound message window size; mandatory uint31 content.
     */
    static final int O_MAX_OUTBOUND_MSG_WINDOW_SIZE = 0x82;
    /**
     * Max responder-bound message count; mandatory uint16 content.
     */
    static final int O_MAX_OUTBOUND_MSG_COUNT = 0x83;
    /**
     * Max requester-bound message size; mandatory uint63 content.
     */
    static final int O_MAX_INBOUND_MSG_SIZE = 0x84;
    /**
     * Max responder-bound message size; mandatory uint63 content.
     */
    static final int O_MAX_OUTBOUND_MSG_SIZE = 0x85;

    // Capabilities

    static final byte CAP_VERSION = 0;   // sent by client & server - max version supported (must be first)
    static final byte CAP_SASL_MECH = 1; // sent by server and client; content = mechanism name (utf-8)
    static final byte CAP_STARTTLS = 2;  // sent by server; content = empty
    static final byte CAP_ENDPOINT_NAME = 3; // sent by client & server - our endpoint name if not anonymous
    static final byte CAP_MESSAGE_CLOSE = 4; // sent by client & server - if present, use message close protocol
    static final byte CAP_VERSION_STRING = 5; // sent by client & server
    static final byte CAP_CHANNELS_IN = 6; // sent by client & server, if missing peer does not support it
    static final byte CAP_CHANNELS_OUT = 7; // sent by client & server, if missing peer does not support it
    static final byte CAP_AUTHENTICATION = 8; // sent by client & server, if missing peer does not support it

    // Greeting messages

    static final byte GRT_SERVER_NAME = 0; // greeting server name

    // Message flags

    static final byte MSG_FLAG_EOF = 0x01;
    static final byte MSG_FLAG_NEW = 0x02;
    static final byte MSG_FLAG_CANCELLED = 0x04;
    static final byte MSG_FLAG_NO_DISPATCH = 0x08;

    private Protocol() {
    }
}
