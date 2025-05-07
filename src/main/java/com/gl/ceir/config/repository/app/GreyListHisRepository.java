package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.GreyListHis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GreyListHisRepository extends JpaRepository<GreyListHis, Long> {
    public GreyListHis save(GreyListHis b);
}
