package com.hqs.async;

public interface AsyncRPCCallback {

    void success(Object result);
    void fail(Exception e);

}
