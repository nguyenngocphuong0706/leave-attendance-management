package com.phuong.lams.service;

import com.phuong.lams.dto.DepartmentForm;
import com.phuong.lams.dto.DepartmentRow;
import com.phuong.lams.entity.Department;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.DepartmentRepository;
import com.phuong.lams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DepartmentRow> layDanhSach() {
        return departmentRepository.layDanhSachKemSoNhanVien();
    }

    // Lấy dữ liệu phòng ban đưa lên form sửa
    @Transactional(readOnly = true)
    public DepartmentForm layDeSua(Long id) {
        Department phongBan = timTheoId(id);
        DepartmentForm form = new DepartmentForm();
        form.setName(phongBan.getName());
        return form;
    }

    @Transactional
    public void them(DepartmentForm form) {
        String ten = form.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(ten)) {
            throw new BusinessException("Tên phòng ban \"" + ten + "\" đã tồn tại");
        }
        Department phongBan = new Department();
        phongBan.setName(ten);
        departmentRepository.save(phongBan);
    }

    @Transactional
    public void capNhat(Long id, DepartmentForm form) {
        Department phongBan = timTheoId(id);
        String ten = form.getName().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(ten, id)) {
            throw new BusinessException("Tên phòng ban \"" + ten + "\" đã tồn tại");
        }
        // Không cần gọi save(): entity đang được quản lý trong transaction,
        // Hibernate tự phát hiện thay đổi và cập nhật khi kết thúc transaction
        phongBan.setName(ten);
    }

    @Transactional
    public void xoa(Long id) {
        Department phongBan = timTheoId(id);
        long soNhanVien = userRepository.countByDepartmentId(id);
        if (soNhanVien > 0) {
            throw new BusinessException("Không thể xóa: phòng ban \"" + phongBan.getName()
                    + "\" còn " + soNhanVien + " nhân viên");
        }
        departmentRepository.delete(phongBan);
    }

    private Department timTheoId(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng ban có mã " + id));
    }
}