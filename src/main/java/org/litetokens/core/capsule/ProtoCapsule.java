package org.litetokens.core.capsule;

public interface ProtoCapsule<T> {

  byte[] getData();

  T getInstance();
}
