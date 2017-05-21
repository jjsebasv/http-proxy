package pdc.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import nio.TCPProtocol;

public class HttpSelectorProtocol implements TCPProtocol {
	private int bufferSize;

	public HttpSelectorProtocol(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void handleAccept(SelectionKey key) throws IOException {
		SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
	}

	public void handleRead(SelectionKey key) throws IOException {
		SocketChannel clientChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = (ByteBuffer) key.attachment();

		int bytesRead = clientChannel.read(buffer);

		byte[] data = new byte[bytesRead];
		System.arraycopy(buffer.array(), 0, data, 0, bytesRead);
		String stringRead = new String(data, "UTF-8");
		System.out.println(stringRead);

		if (bytesRead == -1) {
			clientChannel.close();
		} else if (bytesRead > 0) {
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
	}

	public void handleWrite(SelectionKey key) throws IOException {
		ByteBuffer buffer = (ByteBuffer) key.attachment();
		buffer.flip();
		SocketChannel clientChannel = (SocketChannel) key.channel();
		clientChannel.write(buffer);
		if (!buffer.hasRemaining()) {
			key.interestOps(SelectionKey.OP_READ);
		}
		buffer.compact();
	}
}