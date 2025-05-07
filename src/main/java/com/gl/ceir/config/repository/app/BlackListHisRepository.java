package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.BlackListHis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlackListHisRepository extends JpaRepository<BlackListHis, Long> {
    public BlackListHis save(BlackListHis b);
}
