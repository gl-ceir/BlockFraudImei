package com.gl.ceir.config.model.app;


import lombok.*;

import java.util.LinkedList;

@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BlockedImeiRequest {
    LinkedList<Devices> devices;
}

