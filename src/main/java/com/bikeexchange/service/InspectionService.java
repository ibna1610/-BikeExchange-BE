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
    public InspectionRequest requestInspection(Long requesterId, Long listingId) {
        Bike listing = bikeRepository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing missing"));

        if (!listing.getSeller().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Only seller can request inspection for their listing");
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
        tx.setReferenceId("Inspection req for Bike: " + listingId);
        pointTxRepo.save(tx);

        // Update listing
        listing.setInspectionStatus(Bike.InspectionStatus.REQUESTED);
        bikeRepository.save(listing);

        // Create Inspection record
        InspectionRequest inspection = new InspectionRequest();
        inspection.setListing(listing);
        inspection.setStatus(InspectionRequest.RequestStatus.REQUESTED);
        inspection.setFeePoints(INSPECTION_FEE);
        inspection.setCreatedAt(LocalDateTime.now());

        InspectionRequest saved = inspectionRepository.save(inspection);
        historyService.log("inspection", saved.getId(), "requested", requesterId, null);
        historyService.log("bike", listing.getId(), "inspection_requested", requesterId, null);
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

        InspectionReport saved = reportRepository.save(report);
        historyService.log("inspection", inspection.getId(), "report_submitted", inspectorId, null);
        historyService.log("report", saved.getId(), "created", inspectorId, null);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InspectionReport adminApproveReport(Long reportId) {
        InspectionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setAdminDecision(InspectionRequest.RequestStatus.APPROVED);
        reportRepository.save(report);

        InspectionRequest inspection = report.getRequest();
        inspection.setStatus(InspectionRequest.RequestStatus.APPROVED);
        inspection.setUpdatedAt(LocalDateTime.now());
        inspectionRepository.save(inspection);

        Bike listing = inspection.getListing();
        listing.setInspectionStatus(Bike.InspectionStatus.APPROVED);
        listing.setStatus(Bike.BikeStatus.VERIFIED);
        bikeRepository.save(listing);

        // Release commission to inspector
        UserWallet inspectorWallet = walletRepository.findByUserIdForUpdate(inspection.getInspector().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspector Wallet missing"));
        UserWallet sellerWallet = walletRepository.findByUserIdForUpdate(listing.getSeller().getId())
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

        historyService.log("report", report.getId(), "approved", null, null);
        historyService.log("bike", listing.getId(), "verified", null, null);
        return report;
    }
}
