package com.gl.ceir.config.repository.app;

import com.gl.ceir.config.model.app.OperatorSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperatorSeriesRepository extends JpaRepository<OperatorSeries, Long> {

    public List<OperatorSeries> findAll();
}


