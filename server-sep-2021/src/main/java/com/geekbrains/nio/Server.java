package com.geekbrains.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Server {

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;
    String ROOT_DIR = "server-sep-2021/root";

    public Server() throws IOException {

        buffer = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);


        while (serverChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        buffer.clear();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while (true) {
            if (read == -1) {
                channel.close();
                return;
            }
            read = channel.read(buffer);
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String message = msg.toString();
        //При подключении через Putty в каждом сообщении добавляется \r\n
        //message = message.substring(0, message.indexOf('\r'));

        String[] command = message.split(" ");
        switch (command[0]){
            case "ls":
                Path pathLs = Paths.get(ROOT_DIR);
                try (DirectoryStream<Path> files = Files.newDirectoryStream(pathLs)){
                    for (Path paths : files) {
                        channel.write(ByteBuffer.wrap((paths.toString() + "\n").getBytes(StandardCharsets.UTF_8)));
                    }
                }
            case "cat":
                Path pathCat = Paths.get(ROOT_DIR, command[1]);
                List<String> list = Files.readAllLines(pathCat);
                for (String str : list){
                    channel.write(ByteBuffer.wrap((str + "\n").getBytes(StandardCharsets.UTF_8)));
                }
            default:
                channel.write(ByteBuffer.wrap(("[" + LocalDateTime.now() + "] " + message).getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}