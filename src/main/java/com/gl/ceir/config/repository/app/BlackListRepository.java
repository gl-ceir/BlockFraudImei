package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListRepository extends JpaRepository<BlackList, Long> {

    public BlackList save(BlackList b);
    public BlackList getByImei(String imei);
    public BlackList findFirstByImeiAndImsiAndMsisdn(String imei,String imsi,String msisdn);

}
