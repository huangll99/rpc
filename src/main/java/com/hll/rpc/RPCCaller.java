package com.hll.rpc;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by hll on 2016/5/9.
 */
public class RPCCaller implements Runnable {
  private final ByteBuffer requestDataBuffer = ByteBuffer.allocateDirect(10 * 1024 * 1024);
  private final ByteBuffer responseDataBuffer = ByteBuffer.allocateDirect(10 * 1024 * 1024);

  private LinkedBlockingQueue<RPCRequestEvent> requestEventQueue = new LinkedBlockingQueue<>();

  private ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  private Map<Integer, BlockingQueue<RPCResponse>> responseMap = new ConcurrentHashMap<>();

  private BlockingQueue<RPCResponseEvent> responseEventQueue = new LinkedBlockingQueue<>();

  private RPCCaller() {
    new Thread(this).start();
  }

  private static RPCCaller rpcCaller = new RPCCaller();

  public static RPCCaller getInstanse() {
    return rpcCaller;
  }

  public RPCResponse doRPCCall(RPCRequest request) {
    byte[] bytes = SerializationUtil.serialize(request);
    writeRequest(bytes);
    try {
      requestEventQueue.put(new RPCRequestEvent());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return waitForResponse(request.getSessionId(), request.getRequestId());
  }

  private RPCResponse waitForResponse(int sessionId, int requestId) {
    BlockingQueue<RPCResponse> queue;
    if (!responseMap.containsKey(sessionId)) {
      queue = new LinkedBlockingQueue<>();
      responseMap.put(sessionId, queue);
    } else {
      queue = responseMap.get(sessionId);
    }
    for (; ; ) {
      RPCResponse rpcResponse = fetchResponse(queue, sessionId, requestId);
      if (rpcResponse != null) {
        return rpcResponse;
      }
    }
  }

  private RPCResponse fetchResponse(BlockingQueue<RPCResponse> queue, int sessionId, int requestId) {
    try {
      RPCResponse rpcResponse = queue.take();
      if (rpcResponse.getSessionId() == sessionId && rpcResponse.getRequestId() == requestId) {
        return rpcResponse;
      } else {
        // System.out.println("got wrong message!!!");
        return null;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeRequest(byte[] requestBytes) {
    synchronized (requestDataBuffer) {
      requestDataBuffer.putInt(requestBytes.length);
      requestDataBuffer.put(requestBytes);
    }
  }

  @SuppressWarnings("Duplicates")
  private byte[] readRequest() {
    synchronized (requestDataBuffer) {
      requestDataBuffer.flip();
      int length = requestDataBuffer.getInt();
      byte[] bytes = new byte[length];
      requestDataBuffer.get(bytes);
      requestDataBuffer.compact();
      return bytes;
    }
  }

  private void writeReponse(byte[] responseBytes) {
    synchronized (responseDataBuffer) {
      responseDataBuffer.putInt(responseBytes.length);
      responseDataBuffer.put(responseBytes);
    }
  }

  @SuppressWarnings("Duplicates")
  private byte[] readReponse() {
    synchronized (responseDataBuffer) {
      responseDataBuffer.flip();
      int length = responseDataBuffer.getInt();
      byte[] bytes = new byte[length];
      responseDataBuffer.get(bytes);
      responseDataBuffer.compact();
      return bytes;
    }
  }

  @Override
  public void run() {
    //当有新的Request写入requestDataBuffer后，他被得到通知，获取requestDataBuffer的锁，读取数据，并解析为Request 对象，
    //把这个对象包装为一个Job，放入到一个线程池里去执行
    while (true) {
      //请求事件
      try {
        RPCRequestEvent requestEvent = requestEventQueue.poll(10, TimeUnit.MILLISECONDS);
        if (requestEvent != null) {
          byte[] bytes = readRequest();
          RPCRequest rpcRequest = SerializationUtil.deserialize(bytes, RPCRequest.class);
          threadPool.submit(new Job(rpcRequest));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      //响应事件
      try {
        RPCResponseEvent responseEvent = responseEventQueue.poll(10, TimeUnit.MILLISECONDS);
        if (responseEvent != null) {
          byte[] bytes = readReponse();
          RPCResponse rpcResponse = SerializationUtil.deserialize(bytes, RPCResponse.class);
          responseMap.get(rpcResponse.getSessionId()).put(rpcResponse);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  class Job implements Runnable {

    private RPCRequest rpcRequest;

    public Job(RPCRequest rpcRequest) {
      this.rpcRequest = rpcRequest;
    }

    @Override
    public void run() {
      System.out.println(Thread.currentThread().getName() + " - " + rpcRequest);
      RPCResponse rpcResponse = new RPCResponse(rpcRequest.getSessionId(), rpcRequest.getRequestId(), RPCResponse.OK, null, "ok".getBytes());
      byte[] bytes = SerializationUtil.serialize(rpcResponse);
      writeReponse(bytes);
      //响应通知
      try {
        responseEventQueue.put(new RPCResponseEvent());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
