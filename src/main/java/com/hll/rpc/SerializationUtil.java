package com.hll.rpc;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by hll on 2016/5/9.
 */
public class SerializationUtil {
  private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

  private static Objenesis objenesis = new ObjenesisStd(true);

  private SerializationUtil() {
  }

  private static <T> Schema<T> getSchema(Class<T> cls) {
    Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
    if (schema == null) {
      schema = RuntimeSchema.createFrom(cls);
      if (schema != null) {
        cachedSchema.put(cls, schema);
      }
    }
    return schema;
  }

  public static <T> byte[] serialize(T obj) {
    Class<T> cls = (Class<T>) obj.getClass();
    LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    try {
      Schema<T> schema = getSchema(cls);
      return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
    } finally {
      buffer.clear();
    }
  }

  public static <T> T deserialize(byte[] data,Class<T> cls){
    T message = (T) objenesis.newInstance(cls);
    Schema<T> schema = getSchema(cls);
    ProtostuffIOUtil.mergeFrom(data,message,schema);
    return message;
  }
}
