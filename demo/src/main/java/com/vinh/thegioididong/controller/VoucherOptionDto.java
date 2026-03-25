package com.hutech.demo.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherOptionDto {
    private int vnd;     // giá trị thẻ VND: 10000, 20000, ...
    private int points;  // điểm cần: 100, 200, ...
}
