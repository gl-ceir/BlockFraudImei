package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.GreyList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GreyListRepository extends JpaRepository<GreyList, Long> {

    public GreyList save(GreyList b);
    public GreyList getByImei(String imei);
    public GreyList findFirstByImeiAndImsiAndMsisdn(String imei,String imsi,String msisdn);

}
