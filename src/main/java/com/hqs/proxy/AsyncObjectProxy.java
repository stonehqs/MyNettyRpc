package com.hqs.proxy;

import com.hqs.async.RPCFuture;

public interface AsyncObjectProxy {
    RPCFuture call(String funcName, Object... args);
}
