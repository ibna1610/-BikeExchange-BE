package com.bikeexchange.service;

import com.bikeexchange.dto.request.InspectionReportDto;
import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.exception.ResourceNotFoundException;
import com.bikeexchange.model.*;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public InspectionRequest requestInspection(Long requesterId, Long bikeId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("Requester ID is required");
        }

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

        // Create Inspection record
        InspectionRequest inspection = new InspectionRequest();
        inspection.setBike(bike);
        inspection.setStatus(InspectionRequest.RequestStatus.REQUESTED);
        inspection.setFeePoints(INSPECTION_FEE);
        inspection.setCreatedAt(LocalDateTime.now());

        InspectionRequest saved = inspectionRepository.save(inspection);
        historyService.log("inspection", saved.getId(), "requested", requesterId, null);
        historyService.log("bike", bike.getId(), "inspection_requested", requesterId, null);
        return saved;
    }

    public InspectionRequest assignInspector(Long inspectionId, Long inspectorId) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        User inspector = userRepository.findById(inspectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector user not found"));

        inspection.setInspector(inspector);
        inspection.setStatus(InspectionRequest.RequestStatus.ASSIGNED);
        inspection.setUpdatedAt(LocalDateTime.now());

        InspectionRequest saved = inspectionRepository.save(inspection);
        historyService.log("inspection", saved.getId(), "assigned", inspectorId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport submitReport(Long inspectionId, Long inspectorId, InspectionReportDto request) {
        InspectionRequest inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found"));

        if (inspection.getInspector() == null || !inspection.getInspector().getId().equals(inspectorId)) {
            throw new IllegalArgumentException("Only the assigned inspector can submit report");
        }

        inspection.setStatus(InspectionRequest.RequestStatus.INSPECTED);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspection.setCompletedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);

        InspectionReport report = new InspectionReport();
        report.setRequest(inspection);
        report.setComments(request.getComments());
        report.setOverallScore(request.getOverallScore());
        report.setFrameCondition(request.getFrameCondition());
        report.setGroupsetCondition(request.getGroupsetCondition());
        report.setWheelCondition(request.getWheelCondition());

        // Attach medias if present
        if (request.getMedias() != null && !request.getMedias().isEmpty()) {
            List<InspectionReportMedia> medias = new java.util.ArrayList<>();
            for (int i = 0; i < request.getMedias().size(); i++) {
                var mr = request.getMedias().get(i);
                InspectionReportMedia m = new InspectionReportMedia();
                m.setReport(report);
                m.setUrl(mr.getUrl());
                m.setType(InspectionReportMedia.MediaType.valueOf(mr.getType().toUpperCase()));
                m.setSortOrder(mr.getSortOrder() != null ? mr.getSortOrder() : i);
                medias.add(m);
            }
            report.setMedias(medias);
        }

        InspectionReport saved = reportRepository.save(report);
        historyService.log("inspection", inspection.getId(), "report_submitted", inspectorId, null);
        historyService.log("report", saved.getId(), "created", inspectorId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport adminApproveInspection(Long inspectionId) {
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

        historyService.log("inspection", inspection.getId(), "approved", null, null);
        historyService.log("report", report.getId(), "approved", null, null);
        historyService.log("bike", bike.getId(), "verified", null, null);
        return report;
    }
}
