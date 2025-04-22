package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    public BlackList save(BlackList b);
    public BlackList getByImei(String imei);

}
