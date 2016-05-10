package com.hll.rpc;

/**
 * Created by hll on 2016/5/10.
 */
public class TestRpcCall {
  public static void main(String[] args) {

    for (int i = 0; i < 10; i++) {
      int sessionid=i;
      new Thread(() -> {
        RPCCaller rpcCaller = RPCCaller.getInstanse();
        for (int j=0;j<10;j++){
          RPCResponse rpcResponse = rpcCaller.doRPCCall(new RPCRequest(sessionid, (short) j, "method".getBytes(), "param".getBytes()));
          System.out.println(Thread.currentThread().getName()+" - "+rpcResponse);
        }
      }).start();
    }
  }
}
