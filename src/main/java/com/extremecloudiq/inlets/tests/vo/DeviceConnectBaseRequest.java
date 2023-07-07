package com.extremecloudiq.inlets.tests.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

/**
 * @author dprasad
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class DeviceConnectBaseRequest {

    @NotNull
    private String inletsVersion;

    @NotNull
    private String deviceFamily;

    @NotNull
    private String deviceModel;

    @NotNull
    private String platform;

}
