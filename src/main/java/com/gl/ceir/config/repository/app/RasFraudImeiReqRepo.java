package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.BlockApiReq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RasFraudImeiReqRepo extends JpaRepository<BlockApiReq, Long>,JpaSpecificationExecutor<BlockApiReq> {

    public BlockApiReq save(BlockApiReq r);

}
