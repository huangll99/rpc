package com.hll.rpc;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by hll on 2016/5/9.
 */
public class RPCCaller implements Runnable {
  private ByteBuffer requestDataBuffer = ByteBuffer.allocateDirect(10 * 1024 * 1024);
  private ByteBuffer responseDataBuffer = ByteBuffer.allocateDirect(10 * 1024 * 1024);

  private LinkedBlockingQueue<RPCRequestEvent> requestEventQueue = new LinkedBlockingQueue<>();
  private LinkedBlockingQueue<RPCResponse> responseQueue = new LinkedBlockingQueue<>();

  private ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public RPCCaller() {
    new Thread(this).start();
  }

  public RPCResponse doRPCCall(RPCRequest request) throws InterruptedException {
    byte[] bytes = SerializationUtil.serialize(request);
    writeRequest(bytes);
    requestEventQueue.put(new RPCRequestEvent());
    RPCResponse rpcResponse = responseQueue.take();
    if (request.getSessionId() == rpcResponse.getSessionId() && request.getRequestId() == rpcResponse.getRequestId()) {
      return rpcResponse;
    } else {
      responseQueue.put(rpcResponse);
    }
    return null;
  }

  private void writeRequest(byte[] requestBytes) {
    requestDataBuffer.putInt(requestBytes.length);
    requestDataBuffer.put(requestBytes);
  }

  private byte[] readRequest() {
    int length = requestDataBuffer.getInt();
    byte[] bytes = new byte[length];
    requestDataBuffer.get(bytes);
    return bytes;
  }

  @Override
  public void run() {
    //当有新的Request写入requestDataBuffer后，他被得到通知，获取requestDataBuffer的锁，读取数据，并解析为Request 对象，
    //把这个对象包装为一个Job，放入到一个线程池里去执行
    try {
      RPCRequestEvent requestEvent = requestEventQueue.take();
      byte[] bytes = readRequest();
      RPCRequest rpcRequest = SerializationUtil.deserialize(bytes, RPCRequest.class);
      threadPool.submit(new Job(rpcRequest));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  class Job implements Runnable {

    private RPCRequest rpcRequest;

    public Job(RPCRequest rpcRequest) {
      this.rpcRequest = rpcRequest;
    }

    @Override
    public void run() {
      System.out.println(rpcRequest);
      RPCResponse rpcResponse = new RPCResponse(rpcRequest.getSessionId(), rpcRequest.getRequestId(), RPCResponse.OK, null, "ok".getBytes());
      byte[] bytes = SerializationUtil.serialize(rpcResponse);
      responseDataBuffer.putInt(bytes.length);
      responseDataBuffer.put(bytes);
      //响应通知

    }
  }
}
