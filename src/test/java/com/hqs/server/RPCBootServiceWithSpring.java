package com.hqs.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RPCBootServiceWithSpring {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("server.xml");
    }
}
