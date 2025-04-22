package com.gl.ceir.config.model.app;


import lombok.*;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Devices {
    String imei;
    String msisdn;
    String reason;
    String imsi;
}
