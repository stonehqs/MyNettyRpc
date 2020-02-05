package com.hqs.async;

import com.hqs.client.RPCClient;
import com.hqs.protocol.Request;
import com.hqs.protocol.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于实现异步调用
 */
public class RPCFuture implements Future<Object> {

    private Sync sync;
    private Request request;
    private Response response;
    private long startTime;
    private long responseTimeThreshold = 5000L;

    private List<AsyncRPCCallback> pendingCallbacks = new ArrayList<>();
    private Lock lock = new ReentrantLock();

    public RPCFuture(Request request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        if(this.response != null) {
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if(success) {
            if(this.response != null) {
                return this.response.getResult();
            }
            return null;
        }

        return new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                + ". Request class name: " + this.request.getClassName()
                + ". Request method: " + this.request.getMethodName());
    }

    public void done(Response response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        long responseTime = System.currentTimeMillis() - startTime;
        if(responseTime > responseTimeThreshold) {
            System.out.println("Service response time is too slow. Request id = " + response.getRequestId());
        }
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            for( AsyncRPCCallback asyncRPCCallback : pendingCallbacks) {
                runCallback(asyncRPCCallback);
            }
        } finally {
            lock.unlock();
        }
    }

    public RPCFuture addCallback(AsyncRPCCallback callback) {
        lock.lock();
        try {
            if(isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(final AsyncRPCCallback callback) {
        final Response response = this.response;
        RPCClient.submit(new Runnable() {
            @Override
            public void run() {
                if(!response.isError()) {
                    callback.success(response.getResult());
                } else {
                    callback.fail(new RuntimeException("Response error", new Throwable(response.getError())));
                }
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer {

        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if(getState() == pending) {
                if(compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean isDone() {
            return getState() == done;
        }
    }
}
