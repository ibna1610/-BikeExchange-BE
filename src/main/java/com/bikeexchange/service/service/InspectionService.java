package com.bikeexchange.service.service;

import com.bikeexchange.dto.request.InspectionReportDto;
import com.bikeexchange.dto.request.InspectionRequestDto;
import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * Inspection Management Service
 * - Request inspections (escrow points, status transitions)
 * - Assign inspectors
 * - Submit reports with media
 * - Approve reports (release commission, mark bike VERIFIED)
 * - Audit logging for key actions
 */
@Service
public class InspectionService {

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private UserWalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointTransactionRepository pointTxRepo;

    @Autowired
    private HistoryService historyService;

    private static final Long INSPECTION_FEE = 100L; // 100 points (~100k VND)

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionRequest requestInspection(Long requesterId, InspectionRequestDto dto) {
        if (requesterId == null) {
            throw new IllegalArgumentException("Requester ID is required");
        }

        Long bikeId = dto.getBikeId();
        Bike bike = bikeRepository.findByIdForUpdate(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bike not found with ID: " + bikeId));

        if (bike.getSeller() == null || !bike.getSeller().getId().equals(requesterId)) {
            Long ownerId = (bike.getSeller() != null) ? bike.getSeller().getId() : null;
            throw new IllegalArgumentException(String.format(
                    "Only the seller (ID: %s) can request inspection for this bike. Provided requester ID: %s",
                    ownerId, requesterId));
        }

        UserWallet wallet = walletRepository.findByUserIdForUpdate(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet missing"));

        if (wallet.getAvailablePoints() < INSPECTION_FEE) {
            throw new InsufficientBalanceException(
                    "Not enough points to request inspection. Required: " + INSPECTION_FEE);
        }

        // Deduct points
        wallet.setAvailablePoints(wallet.getAvailablePoints() - INSPECTION_FEE);
        wallet.setFrozenPoints(wallet.getFrozenPoints() + INSPECTION_FEE);
        walletRepository.save(wallet);

        // Transaction log
        PointTransaction tx = new PointTransaction();
        tx.setUser(wallet.getUser());
        tx.setAmount(INSPECTION_FEE);
        tx.setType(PointTransaction.TransactionType.ESCROW_HOLD);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId("Inspection req for Bike: " + bikeId);
        pointTxRepo.save(tx);

        // Update bike
        bike.setInspectionStatus(Bike.InspectionStatus.REQUESTED);
        bikeRepository.save(bike);

        // Create Inspection record with scheduling info
        InspectionRequest inspection = new InspectionRequest();
        inspection.setBike(bike);
        inspection.setStatus(InspectionRequest.RequestStatus.REQUESTED);
        inspection.setFeePoints(INSPECTION_FEE);
        inspection.setCreatedAt(LocalDateTime.now());

        // Set scheduling / availability fields from DTO
        inspection.setPreferredDate(dto.getPreferredDate());
        inspection.setPreferredTimeSlot(dto.getPreferredTimeSlot());
        inspection.setAddress(dto.getAddress());
        inspection.setContactPhone(dto.getContactPhone());
        inspection.setNotes(dto.getNotes());

        InspectionRequest saved = inspectionRepository.save(inspection);
        historyService.log("inspection", saved.getId(), "requested", requesterId, null);
        historyService.log("bike", bike.getId(), "inspection_requested", requesterId, null);
        return saved;
    }

    /**
     * @deprecated Use {@link #requestInspection(Long, InspectionRequestDto)}
     *             instead.
     */
    @Deprecated
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionRequest requestInspection(Long requesterId, Long bikeId) {
        InspectionRequestDto dto = new InspectionRequestDto();
        dto.setBikeId(bikeId);
        return requestInspection(requesterId, dto);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionRequest updateInspectionStatus(Long inspectionId, InspectionRequest.RequestStatus status, Long userId) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (inspection.getStatus() == InspectionRequest.RequestStatus.APPROVED || 
            inspection.getStatus() == InspectionRequest.RequestStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot update status. Inspection has already been finalized (APPROVED/REJECTED) by admin.");
        }

        inspection.setStatus(status);
        inspection.setUpdatedAt(LocalDateTime.now());

        // If status is ASSIGNED, and no inspector assigned yet, assign the current user
        if (status == InspectionRequest.RequestStatus.ASSIGNED && inspection.getInspector() == null) {
            User inspector = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inspector user not found"));
            inspection.setInspector(inspector);
        }

        if (status == InspectionRequest.RequestStatus.IN_PROGRESS) {
            inspection.setStartedAt(LocalDateTime.now());
        } else if (status == InspectionRequest.RequestStatus.INSPECTED) {
            inspection.setCompletedAt(LocalDateTime.now());
        }

        // Sync with Bike status
        Bike bike = inspection.getBike();
        switch (status) {
            case REQUESTED -> bike.setInspectionStatus(Bike.InspectionStatus.REQUESTED);
            case ASSIGNED, IN_PROGRESS -> bike.setInspectionStatus(Bike.InspectionStatus.IN_PROGRESS);
            case INSPECTED -> bike.setInspectionStatus(Bike.InspectionStatus.IN_PROGRESS); // Still in progress (waiting for admin)
            case APPROVED -> bike.setInspectionStatus(Bike.InspectionStatus.APPROVED);
            case REJECTED -> bike.setInspectionStatus(Bike.InspectionStatus.REJECTED);
        }
        if (status == InspectionRequest.RequestStatus.REJECTED) {
            performRefund(inspection);
        }

        bikeRepository.save(bike);
        return inspectionRepository.save(inspection);
    }

    private void performRefund(InspectionRequest inspection) {
        Bike bike = inspection.getBike();
        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(bike.getSeller().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller Wallet missing"));

        Long fee = inspection.getFeePoints();
        if (fee > 0) {
            sellerWallet.setFrozenPoints(sellerWallet.getFrozenPoints() - fee);
            sellerWallet.setAvailablePoints(sellerWallet.getAvailablePoints() + fee);
            walletRepository.save(sellerWallet);

            PointTransaction tx = new PointTransaction();
            tx.setUser(sellerWallet.getUser());
            tx.setAmount(fee);
            tx.setType(PointTransaction.TransactionType.EARN); // Using EARN as fallback for REFUND if DB schema is not updated
            tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
            String ref = "Inspection refund for Bike: " + bike.getId() + " - Req: " + inspection.getId();
            tx.setReferenceId(ref);
            tx.setRemarks("REFUND: " + ref);
            pointTxRepo.save(tx);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionRequest assignInspector(Long inspectionId, Long inspectorId) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector user not found"));

        inspection.setInspector(inspector);
        return updateInspectionStatus(inspectionId, InspectionRequest.RequestStatus.ASSIGNED, inspectorId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport submitReport(Long inspectionId, Long inspectorId, InspectionReportDto request) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (inspection.getInspector() == null || !inspection.getInspector().getId().equals(inspectorId)) {
            throw new IllegalArgumentException("Only the assigned inspector can submit report");
        }

        if (inspection.getStatus() == InspectionRequest.RequestStatus.APPROVED || 
            inspection.getStatus() == InspectionRequest.RequestStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot update report. Inspection has already been finalized (APPROVED/REJECTED) by admin.");
        }

        updateInspectionStatus(inspectionId, InspectionRequest.RequestStatus.INSPECTED, inspectorId);

        // Check if report already exists to avoid duplicate entry error
        InspectionReport report = reportRepository.findByRequestId(inspectionId)
                .orElse(new InspectionReport());
        
        report.setRequest(inspection);
        report.setComments(request.getComments());
        report.setOverallScore(request.getOverallScore());
        report.setFrameCondition(request.getFrameCondition());
        report.setGroupsetCondition(request.getGroupsetCondition());
        report.setWheelCondition(request.getWheelCondition());

        // Attach medias if present
        if (request.getMedias() != null) {
            if (report.getMedias() == null) {
                report.setMedias(new java.util.ArrayList<>());
            } else {
                report.getMedias().clear();
            }
            
            for (int i = 0; i < request.getMedias().size(); i++) {
                var mr = request.getMedias().get(i);
                InspectionReportMedia m = new InspectionReportMedia();
                m.setReport(report);
                m.setUrl(mr.getUrl());
                m.setType(InspectionReportMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                m.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                report.getMedias().add(m);
            }
        }

        InspectionReport saved = reportRepository.save(report);
        historyService.log("inspection", inspection.getId(), "report_submitted", inspectorId, null);
        historyService.log("report", saved.getId(), "created", inspectorId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport adminApproveInspection(Long inspectionId, Long adminId) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        InspectionReport report = reportRepository.findByRequestId(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("No report found for this inspection"));

        report.setAdminDecision(InspectionRequest.RequestStatus.APPROVED);
        reportRepository.save(report);

        inspection.setStatus(InspectionRequest.RequestStatus.APPROVED);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);

        Bike bike = inspection.getBike();
        bike.setInspectionStatus(Bike.InspectionStatus.APPROVED);
        bike.setStatus(Bike.BikeStatus.VERIFIED);
        bikeRepository.save(bike);

        // Release commission to inspector
        UserWallet inspectorWallet = walletRepository.findByUserIdForUpdate(inspection.getInspector().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspector Wallet missing"));
        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(bike.getSeller().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller Wallet missing"));

        Long fee = inspection.getFeePoints();
        Long commission = (long) (fee * 0.8); // 80% to inspector, 20% system

        sellerWallet.setFrozenPoints(sellerWallet.getFrozenPoints() - fee);
        walletRepository.save(sellerWallet);

        inspectorWallet.setAvailablePoints(inspectorWallet.getAvailablePoints() + commission);
        walletRepository.save(inspectorWallet);

        PointTransaction tx = new PointTransaction();
        tx.setUser(inspectorWallet.getUser());
        tx.setAmount(commission);
        tx.setType(PointTransaction.TransactionType.EARN);
        tx.setStatus(PointTransaction.TransactionStatus.SUCCESS);
        tx.setReferenceId("Inspection fee: " + inspection.getId());
        pointTxRepo.save(tx);

        historyService.log("inspection", inspection.getId(), "approved", adminId, null);
        historyService.log("report", report.getId(), "approved", adminId, null);
        historyService.log("bike", bike.getId(), "verified", adminId, null);
        return report;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport adminRejectInspection(Long inspectionId, Long adminId, String reason) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        // Update inspection request status
        inspection.setStatus(InspectionRequest.RequestStatus.REJECTED);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);

        // Sync with Bike status
        Bike bike = inspection.getBike();
        bike.setInspectionStatus(Bike.InspectionStatus.REJECTED);
        bikeRepository.save(bike);

        // Refund fee to seller wallet
        performRefund(inspection);

        historyService.log("inspection", inspection.getId(), "rejected", adminId, reason);
        historyService.log("bike", bike.getId(), "inspection_rejected", adminId, reason);

        // Handle report if exists
        InspectionReport report = reportRepository.findByRequestId(inspectionId).orElse(null);
        if (report != null) {
            report.setAdminDecision(InspectionRequest.RequestStatus.REJECTED);
            reportRepository.save(report);
            historyService.log("report", report.getId(), "rejected", adminId, reason);
            return report;
        }

        return null; // Return null if no report existed, but the rejection is successful
    }

    /**
     * @deprecated Use {@link #adminApproveInspection(Long, Long)} with adminId
     *             instead.
     */
    @Deprecated
    public InspectionReport adminApproveInspection(Long inspectionId) {
        return adminApproveInspection(inspectionId, null);
    }
    
    @Transactional(readOnly = true)
    public InspectionReport getReportByBikeId(Long bikeId) {
        return reportRepository.findFirstByRequest_Bike_IdOrderByCreatedAtDesc(bikeId)
                .orElseThrow(() -> new ResourceNotFoundException("No inspection report found for bike ID: " + bikeId));
    }
}
