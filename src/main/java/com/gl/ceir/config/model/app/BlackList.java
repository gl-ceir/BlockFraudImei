package com.gl.ceir.config.model.app;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionNumber;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "black_list")
//@DynamicInsert
@ToString
@AuditTable(value = "black_list_his")
@Audited
@EntityListeners(AuditingEntityListener.class)


public class BlackList implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="BLACKIMEI_SEQ")
    @SequenceGenerator(name="BLACKIMEI_SEQ", sequenceName="BLACKIMEI_SEQ", allocationSize=1)
    private Long id;

    @Column(name = "actual_imei")
    private String actualImei;

    @Column(name = "complaint_type")
    private String complaintType;

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "source_of_request")
    private String sourceOfRequest;

    @Column(name = "mode_type")
    private String modeType;

    @Column(name = "clarify_reason")
    private String clarifyReason="Fraud Api";

    @Column(name = "request_type")
    private String requestType="Other";

    private String tac, imei, imsi, msisdn;
    private LocalDateTime expiryDate;  // modeType,userType , clarifyReason, reason ,remarks
    private int taxPaid;

    public BlackList(String imei, String actualImei, String imsi, String msisdn, String tac, String txnId, String modeType) {
        this.imei = imei;
        this.actualImei = actualImei;
        this.imsi = imsi;
        this.msisdn = msisdn;
        this.tac = tac;
        this.txnId = txnId;
        this.modeType = modeType;
    }


//    @Transient
//    @RevisionNumber
//    @Column(name = "operation")
//    private int operation;

//    @CreationTimestamp
//    @JsonFormat(pattern="yyyy-MM-dd HH:mm")
//    private LocalDateTime createdOn;

    @UpdateTimestamp
    private LocalDateTime modifiedOn;

}
