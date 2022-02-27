package com.chaoqer.util;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
