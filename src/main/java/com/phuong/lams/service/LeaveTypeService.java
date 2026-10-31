package com.phuong.lams.service;

import com.phuong.lams.dto.LeaveTypeForm;
import com.phuong.lams.dto.LeaveTypeRow;
import com.phuong.lams.entity.LeaveType;
import com.phuong.lams.exception.BusinessException;
import com.phuong.lams.repository.LeaveRequestRepository;
import com.phuong.lams.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Nghiệp vụ quản lý loại nghỉ phép dành cho ADMIN
@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceService leaveBalanceService;

    @Transactional(readOnly = true)
    public List<LeaveTypeRow> layDanhSach() {
        return leaveTypeRepository.findAllByOrderByIdAsc().stream()
                .map(t -> new LeaveTypeRow(t.getId(), t.getName(), t.getDaysPerYear(),
                        t.getAppliesTo().getLabel(), t.isActive()))
                .toList();
    }

    // Lấy dữ liệu loại nghỉ đưa lên form sửa
    @Transactional(readOnly = true)
    public LeaveTypeForm layDeSua(Long id) {
        LeaveType loai = timTheoId(id);
        LeaveTypeForm form = new LeaveTypeForm();
        form.setName(loai.getName());
        form.setDaysPerYear(loai.getDaysPerYear());
        form.setAppliesTo(loai.getAppliesTo());
        return form;
    }

    @Transactional
    public void them(LeaveTypeForm form) {
        String ten = form.getName().trim();
        if (leaveTypeRepository.existsByNameIgnoreCase(ten)) {
            throw new BusinessException("name", "Loại nghỉ \"" + ten + "\" đã tồn tại");
        }
        LeaveType loai = new LeaveType();
        loai.setName(ten);
        loai.setDaysPerYear(form.getDaysPerYear());
        loai.setAppliesTo(form.getAppliesTo());
        LeaveType daLuu = leaveTypeRepository.save(loai);
        // Cấp số dư cho những người thuộc đối tượng của loại nghỉ mới
        leaveBalanceService.capSoDuChoLoaiNghi(daLuu);
    }

    // Đổi số ngày chỉ áp dụng cho người mới và năm mới, số dư đã cấp giữ nguyên
    @Transactional
    public void capNhat(Long id, LeaveTypeForm form) {
        LeaveType loai = timTheoId(id);
        String ten = form.getName().trim();
        if (leaveTypeRepository.existsByNameIgnoreCaseAndIdNot(ten, id)) {
            throw new BusinessException("name", "Loại nghỉ \"" + ten + "\" đã tồn tại");
        }
        loai.setName(ten);
        loai.setDaysPerYear(form.getDaysPerYear());
        loai.setAppliesTo(form.getAppliesTo());
        if (loai.isActive()) {
            // Mở rộng đối tượng (ví dụ từ Chỉ nữ sang Tất cả) thì người mới thuộc đối tượng được bổ sung số dư
            leaveBalanceService.capSoDuChoLoaiNghi(loai);
        }
    }

    // Ngừng dùng hoặc dùng lại, trả về trạng thái mới (true là đang dùng)
    @Transactional
    public boolean doiTrangThai(Long id) {
        LeaveType loai = timTheoId(id);
        loai.setActive(!loai.isActive());
        if (loai.isActive()) {
            // Dùng lại: người nào còn thiếu số dư thì được bổ sung
            leaveBalanceService.capSoDuChoLoaiNghi(loai);
        }
        return loai.isActive();
    }

    // Chỉ xóa được khi chưa có đơn nghỉ nào và chưa ai dùng ngày phép của loại nghỉ này
    @Transactional
    public void xoa(Long id) {
        LeaveType loai = timTheoId(id);
        if (leaveRequestRepository.existsByLeaveTypeId(id)) {
            throw new BusinessException("Không thể xóa: loại nghỉ \"" + loai.getName()
                    + "\" đã có đơn xin nghỉ. Hãy chọn Ngừng dùng");
        }
        if (leaveBalanceService.coNguoiDaDung(id)) {
            throw new BusinessException("Không thể xóa: loại nghỉ \"" + loai.getName()
                    + "\" đã có người sử dụng ngày phép. Hãy chọn Ngừng dùng");
        }
        // Chưa có đơn và chưa ai dùng nên dọn số dư đã cấp rồi mới xóa loại nghỉ
        leaveBalanceService.xoaSoDuCuaLoaiNghi(id);
        leaveTypeRepository.delete(loai);
    }

    private LeaveType timTheoId(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy loại nghỉ có mã " + id));
    }
}