package com.gl.ceir.config.model.app;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Setter
@Entity
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "block_imei_req")
public class BlockApiReq implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status,remark,source,fileName,requestId;

    private Integer statusCode;
    private Integer imeiCount;
    private Integer successCount;
    private Integer failCount;

    public BlockApiReq(String status, Integer statusCode, Integer imeiCount ,String source,String fileName,String requestId) {
        this.status = status;
        this.statusCode = statusCode;
        this.imeiCount = imeiCount;
        this.source = source;
        this.fileName = fileName;
        this.requestId = requestId;
     }

    @UpdateTimestamp
    private LocalDateTime modifiedOn;
}
