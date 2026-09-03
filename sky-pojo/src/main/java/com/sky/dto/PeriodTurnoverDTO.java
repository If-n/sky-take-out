package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodTurnoverDTO {
    //开始时间
    private LocalDateTime begin;
    //结束时间
    private LocalDateTime end;
    //订单状态指定
    private Integer status;
}
