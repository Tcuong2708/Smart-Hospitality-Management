package com.votricuong.mayhotel.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileResponse {
    Long idTaiKhoan;
    String tenDangNhap;
    String hoTen;
    String email;
    String soDienThoai;
    String quocTich;
    String diaChi;
    String gender;
    Long roleID;
    String roleName;
    String displayTier;
    Integer points;
}
