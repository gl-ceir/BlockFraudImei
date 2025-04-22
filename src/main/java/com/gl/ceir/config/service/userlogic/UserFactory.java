package com.gl.ceir.config.service.userlogic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserFactory {
    private final Logger logger = LogManager.getLogger(getClass());

    @Autowired
    UserDetailDao userDetailDao;

    public UserInterface createUser() {
        return userDetailDao;


    }
}
