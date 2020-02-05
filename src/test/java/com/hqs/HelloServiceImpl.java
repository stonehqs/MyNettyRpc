package com.hqs;

import com.hqs.server.RPCService;

@RPCService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }

    @Override
    public String sayAge(int age) {
        return "my Age" + age;
    }
}
