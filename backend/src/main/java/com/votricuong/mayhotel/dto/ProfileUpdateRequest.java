package com.votricuong.mayhotel.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String nationality;
    private String address;
    private String gender;
}
