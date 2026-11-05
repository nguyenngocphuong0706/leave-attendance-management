package com.phuong.lams.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicabilityTest {

    @Test
    void tatCa_apDungChoMoiNguoiKeCaChuaCoGioiTinh() {
        assertTrue(Applicability.ALL.apDungCho(Gender.MALE));
        assertTrue(Applicability.ALL.apDungCho(Gender.FEMALE));
        assertTrue(Applicability.ALL.apDungCho(null));
    }

    @Test
    void chiNam_chiApDungChoNam() {
        assertTrue(Applicability.MALE.apDungCho(Gender.MALE));
        assertFalse(Applicability.MALE.apDungCho(Gender.FEMALE));
    }

    @Test
    void chiNu_chiApDungChoNu() {
        assertTrue(Applicability.FEMALE.apDungCho(Gender.FEMALE));
        assertFalse(Applicability.FEMALE.apDungCho(Gender.MALE));
    }

    @Test
    void nguoiChuaCoGioiTinh_khongDuocHuongLoaiNghiChiNamHoacChiNu() {
        assertFalse(Applicability.MALE.apDungCho(null));
        assertFalse(Applicability.FEMALE.apDungCho(null));
    }
}