package com.gl.ceir.config.model.app;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Getter
@Setter
@ToString
@MappedSuperclass
public class GenericList {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "actual_imei")
    private String actualImei;

    @Column(name = "complaint_type")
    private String complaintType = "RAS";

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "source_of_request")
    private String sourceOfRequest;

    @Column(name = "mode_type")
    private String modeType = "API";

    @Column(name = "clarify_reason")
    private String clarifyReason = "Fraud Api";

    @Column(name = "request_type")
    private String requestType = "Other";

    private String reason = "Fraud API";
    private String remarks = "Imei Blocked Due to RAS Fraud";

    private int operator_id;

    private String tac, imei, imsi, msisdn, userType, userId;

    @UpdateTimestamp
    private LocalDateTime modifiedOn;


    private LocalDateTime expiryDate;


    public GenericList(String actualImei, String imsi, String msisdn, String txnId, int operator_id, String operatorName, String userType, String userId ,LocalDateTime expiryDate) {
        this.imei = actualImei.substring(0, 14);
        this.actualImei = actualImei;
        this.imsi = imsi;
        this.msisdn = msisdn;
        this.tac = imei.substring(0, 8);
        this.txnId = txnId;
        this.operator_id = operator_id;
        this.operatorName = operatorName;
        this.userId = userId;
        this.userType = userType;
        this.expiryDate = expiryDate;
    }


    public GenericList() {
    }
}
