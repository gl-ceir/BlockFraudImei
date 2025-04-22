package com.gl.ceir.config.service.userlogic;

import org.springframework.stereotype.Service;

@Service
public interface UserInterface {

    public <T> T getUserDetailDao(String userName, String password, int parentId);

    public <T> T getUserDetailDao(String userName, String password);

}