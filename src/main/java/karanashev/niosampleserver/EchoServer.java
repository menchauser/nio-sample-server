/*
 This software is the confidential information and copyrighted work of
 NetCracker Technology Corp. ("NetCracker") and/or its suppliers and
 is only distributed under the terms of a separate license agreement
 with NetCracker.
 Use of the software is governed by the terms of the license agreement.
 Any use of this software not in accordance with the license agreement
 is expressly prohibited by law, and may result in severe civil
 and criminal penalties. 
 
 Copyright (c) 1995-2015 NetCracker Technology Corp.
 
 All Rights Reserved.
 
*/

package karanashev.niosampleserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: MUKA0210,
 * Date: 24.07.2015
 * <br />
 */
public class EchoServer {
    private final int port;
    private final ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
    private final Map<Integer, byte[]> echoCache = new HashMap<>();
    private final AtomicInteger clientCounter = new AtomicInteger(1);

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        Selector serverSelector = Selector.open();
        serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");

        while (true) {
            int channelsCount = serverSelector.select();

            if (channelsCount > 0) {
                Set<SelectionKey> selectedKeys = serverSelector.selectedKeys();
                System.out.println("Selected " + selectedKeys.size() + " keys");

                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        System.out.println("Channel: " + key.channel());
                        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = channel.accept();
                        socketChannel.configureBlocking(false);
                        System.out.println("Accepted client socket");
                        socketChannel.register(serverSelector, SelectionKey.OP_READ, clientCounter.getAndIncrement());
                        System.out.println("Socket configured and registered");
                    } else if (key.isReadable()) {
                        System.out.println("Reading data from channel: " + key.channel());
                        Integer clientNumber = (Integer) key.attachment();
                        System.out.println("Client number: " + clientNumber);
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        WritableByteChannel readBufferChannel = Channels.newChannel(bos);
                        while (true) {
                            echoBuffer.clear();
                            int read = socketChannel.read(echoBuffer);

                            if (read <= 0) {
                                break;
                            }
                            echoBuffer.flip();

                            readBufferChannel.write(echoBuffer);

                            // TODO: write in non-blocking way too
                            //socketChannel.write(echoBuffer);
                        }
                        System.out.println("Data read");
                        echoCache.put(clientNumber, bos.toByteArray());
                        System.out.println("Data stored in cache");
                        socketChannel.register(serverSelector, SelectionKey.OP_WRITE, clientNumber);
                        System.out.println("Write to socket scheduled");
                    } else if (key.isWritable()) {
                        System.out.println("Writing data to channel: " + key.channel());
                        Integer clientNumber = (Integer) key.attachment();
                        System.out.println("Client number: " + clientNumber);
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        ByteBuffer echoResult = ByteBuffer.wrap(echoCache.get(clientNumber));
                        socketChannel.write(echoResult);
                    }
                    iterator.remove();
                }
            }
        }
    }
}
/*
 WITHOUT LIMITING THE FOREGOING, COPYING, REPRODUCTION, REDISTRIBUTION,
 REVERSE ENGINEERING, DISASSEMBLY, DECOMPILATION OR MODIFICATION
 OF THE SOFTWARE IS EXPRESSLY PROHIBITED, UNLESS SUCH COPYING,
 REPRODUCTION, REDISTRIBUTION, REVERSE ENGINEERING, DISASSEMBLY,
 DECOMPILATION OR MODIFICATION IS EXPRESSLY PERMITTED BY THE LICENSE
 AGREEMENT WITH NETCRACKER. 
 
 THIS SOFTWARE IS WARRANTED, IF AT ALL, ONLY AS EXPRESSLY PROVIDED IN
 THE TERMS OF THE LICENSE AGREEMENT, EXCEPT AS WARRANTED IN THE
 LICENSE AGREEMENT, NETCRACKER HEREBY DISCLAIMS ALL WARRANTIES AND
 CONDITIONS WITH REGARD TO THE SOFTWARE, WHETHER EXPRESS, IMPLIED
 OR STATUTORY, INCLUDING WITHOUT LIMITATION ALL WARRANTIES AND
 CONDITIONS OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 TITLE AND NON-INFRINGEMENT.
 
 Copyright (c) 1995-2015 NetCracker Technology Corp.
 
 All Rights Reserved.
*/
