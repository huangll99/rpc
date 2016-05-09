package com.hll.rpc;

import java.util.Arrays;

/**
 * Created by hll on 2016/5/9.
 */
public class RPCRequest {

  private int sessionId;//会话Id
  private short requestId;//请求的序号，从0开始，达到最大值后重新复位为0
  private byte[] reqMethod;
  private byte[] requestParams;

  public RPCRequest() {
  }

  public RPCRequest(int sessionId, short requestId, byte[] reqMethod, byte[] requestParams) {
    this.sessionId = sessionId;
    this.requestId = requestId;
    this.reqMethod = reqMethod;
    this.requestParams = requestParams;
  }

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
  }

  public short getRequestId() {
    return requestId;
  }

  public void setRequestId(short requestId) {
    this.requestId = requestId;
  }

  public byte[] getReqMethod() {
    return reqMethod;
  }

  public void setReqMethod(byte[] reqMethod) {
    this.reqMethod = reqMethod;
  }

  public byte[] getRequestParams() {
    return requestParams;
  }

  public void setRequestParams(byte[] requestParams) {
    this.requestParams = requestParams;
  }

  @Override
  public String toString() {
    return "RPCRequest{" +
        "sessionId=" + sessionId +
        ", requestId=" + requestId +
        ", reqMethod=" + Arrays.toString(reqMethod) +
        ", requestParams=" + Arrays.toString(requestParams) +
        '}';
  }
}
