package NioServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;
    private Path current = Path.of(System.getProperty("user.home"));

    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();
        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        while (server.isOpen()){
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                if (key.isAcceptable()){
                    handleAccept();
                }
                if (key.isReadable()){
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public void handleAccept() throws IOException {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector,SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap("Welcome in terminal Alex!\n->".getBytes(StandardCharsets.UTF_8)));
    }

    public void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuffer s = new StringBuffer();
        while (channel.isOpen()) {
            int read = channel.read(buf);
            if (read < 0) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear();
        }

        String messageIn = s.toString().trim();

            if (messageIn.equals("ls")) {
                String bufferLine = "";
                for (int i = 0; i < getFiles(current.toString()).size(); i++) {
                    bufferLine = bufferLine + getFiles(current.toString()).get(i) + "  ";
                }
                byte message[] = (bufferLine + "\n->").getBytes(StandardCharsets.UTF_8);
                channel.write(ByteBuffer.wrap(message));
            } else if (messageIn.startsWith("cat")) {
                String bufferMas[] = messageIn.split(" ");
                File file = new File(current + "/" + bufferMas[1]);
                FileReader reader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = "";
                String c;
                while ((c = bufferedReader.readLine()) != null) {
                    line = line + c + "\n";
                }
                byte message[] = (line + "->").getBytes(StandardCharsets.UTF_8);
                channel.write(ByteBuffer.wrap(message));
            } else if (messageIn.startsWith("cd")){
                String bufferMas[] = messageIn.split(" ");
                if (bufferMas.length == 2){
                    String dir = bufferMas[1];
                    Path target = current.resolve(dir);
                    if (Files.exists(target)){
                        if (Files.isDirectory(target)){
                            current = target;
                            byte message[] = "->".getBytes(StandardCharsets.UTF_8);
                            channel.write(ByteBuffer.wrap(message));
                        } else {
                            String responce = "arg should be directory\n->";
                            channel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
                        }
                    } else {
                        String responce = "directory is not exist\n->";
                        channel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
                    }
                } else {
                    String responce = "command cd should have 1 only arguments\n->";
                    channel.write(ByteBuffer.wrap(responce.getBytes(StandardCharsets.UTF_8)));
                }
            } else {
                String msg = "Unknow command\n";
                byte message[] = (msg+ "->").getBytes(StandardCharsets.UTF_8);
                channel.write(ByteBuffer.wrap(message));
            }
        }

    private List<String> getFiles(String dir){
        String list[] = new File(dir).list();
        assert list != null;
        return Arrays.asList(list);
    }
}
