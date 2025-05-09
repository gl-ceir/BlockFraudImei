package com.gl.ceir.config.model.app;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Setter
@Entity
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table
public class OperatorSeries implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int seriesStart, seriesEnd;
    private String seriesType, operatorName;

    public OperatorSeries(Long id, String operatorName) {
        this.id = id;
        this.operatorName = operatorName;
    }
}
