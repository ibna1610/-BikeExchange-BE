package com.bikeexchange.config;

import com.bikeexchange.model.*;
import com.bikeexchange.model.Bike.BikeStatus;
import com.bikeexchange.model.Bike.InspectionStatus;
import com.bikeexchange.model.InspectionRequest.RequestStatus;
import com.bikeexchange.model.Order.OrderStatus;
import com.bikeexchange.model.PointTransaction.TransactionStatus;
import com.bikeexchange.model.PointTransaction.TransactionType;
import com.bikeexchange.repository.*;
import com.bikeexchange.model.Post;
import com.bikeexchange.model.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private UserWalletRepository walletRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BikeRepository bikeRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InspectionRepository inspectionRepository;
    @Autowired private ReportRepository inspectionReportRepository;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserReportRepository userReportRepository;
    @Autowired private PointTransactionRepository pointTransactionRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ComponentRepository componentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // ── 1. Users ──────────────────────────────────────────────────────────
        User admin      = seedUser("admin@bikeexchange.com",      "Nguyễn Quản Trị",   "0901000001", "12 Lê Lợi, Q.1, TP.HCM",          User.UserRole.ADMIN,     "ACTIVE", 0L);
        User seller1    = seedUser("seller1@bikeexchange.com",    "Trần Văn Bán",       "0901000002", "45 Nguyễn Huệ, Q.1, TP.HCM",       User.UserRole.SELLER,    "ACTIVE", 50_000L);
        User seller2    = seedUser("seller2@bikeexchange.com",    "Lê Thị Shop",        "0901000003", "88 Điện Biên Phủ, Q.3, TP.HCM",    User.UserRole.SELLER,    "ACTIVE", 30_000L);
        User seller3    = seedUser("seller3@bikeexchange.com",    "Phạm Xe Đạp",        "0901000004", "22 Cách Mạng Tháng 8, Q.10, TP.HCM",User.UserRole.SELLER,   "ACTIVE", 15_000L);
        User inspector1 = seedUser("inspector@bikeexchange.com",  "Võ Kiểm Định",       "0901000005", "5 Trường Chinh, Q.TB, TP.HCM",     User.UserRole.INSPECTOR, "ACTIVE", 5_000L);
        User inspector2 = seedUser("inspector2@bikeexchange.com", "Đặng Kỹ Thuật",      "0901000006", "99 Tô Hiến Thành, Q.10, TP.HCM",   User.UserRole.INSPECTOR, "ACTIVE", 3_000L);
        User buyer1     = seedUser("buyer1@bikeexchange.com",     "Nguyễn Mua Hàng",    "0901000007", "17 Bùi Thị Xuân, Q.1, TP.HCM",     User.UserRole.BUYER,     "ACTIVE", 100_000L);
        User buyer2     = seedUser("buyer2@bikeexchange.com",     "Hoàng Thị Lan",      "0901000008", "30 Nam Kỳ Khởi Nghĩa, Q.3, TP.HCM",User.UserRole.BUYER,     "ACTIVE", 80_000L);
        User buyer3     = seedUser("buyer3@bikeexchange.com",     "Đinh Văn Khách",     "0901000009", "55 Hai Bà Trưng, Q.1, TP.HCM",     User.UserRole.BUYER,     "ACTIVE", 60_000L);
        User buyer4     = seedUser("buyer4@bikeexchange.com",     "Bùi Xuân Mua",       "0901000010", "11 Lý Thường Kiệt, Q.5, TP.HCM",   User.UserRole.BUYER,     "ACTIVE", 20_000L);
        // legacy accounts
        seedUser("seller@bikeexchange.com",    "Sample Seller",    "0987654321", "System Default Address", User.UserRole.SELLER,    "ACTIVE", 1_000L);
        seedUser("buyer@bikeexchange.com",     "Sample Buyer",     "0987654321", "System Default Address", User.UserRole.BUYER,     "ACTIVE", 1_000L);

        // ── 2. Brands ─────────────────────────────────────────────────────────
        Brand trek       = seedBrand("Trek",        "Hãng xe đạp hàng đầu Mỹ, nổi tiếng với xe road và mountain");
        Brand giant      = seedBrand("Giant",       "Thương hiệu xe đạp lớn nhất thế giới xuất xứ Đài Loan");
        Brand specialized= seedBrand("Specialized", "Thương hiệu cao cấp từ Mỹ, chuyên xe road & MTB");
        Brand bianchi    = seedBrand("Bianchi",     "Hãng xe đạp lâu đời nhất thế giới, đến từ Ý");
        Brand scott      = seedBrand("Scott",       "Thương hiệu Thụy Sĩ nổi tiếng với xe leo núi");
        Brand merida     = seedBrand("Merida",      "Hãng xe đạp Đài Loan chất lượng cao");
        Brand polygon    = seedBrand("Polygon",     "Thương hiệu xe đạp từ Indonesia, phổ biến ở Đông Nam Á");
        Brand fuji       = seedBrand("Fuji",        "Hãng xe Nhật Bản với lịch sử hơn 100 năm");

        // ── 3. Categories ─────────────────────────────────────────────────────
        Category road      = seedCategory("Road",          "Xe đạp đường trường tốc độ cao",         "https://cdn.example.com/cat-road.jpg");
        Category mountain  = seedCategory("Mountain",      "Xe đạp leo núi địa hình",                "https://cdn.example.com/cat-mountain.jpg");
        Category city      = seedCategory("City/Urban",    "Xe đạp đô thị, đi lại hàng ngày",       "https://cdn.example.com/cat-city.jpg");
        Category bmx       = seedCategory("BMX",           "Xe đạp nhào lộn, biểu diễn",             "https://cdn.example.com/cat-bmx.jpg");
        Category gravel    = seedCategory("Gravel",        "Xe đa địa hình, kết hợp road & MTB",    "https://cdn.example.com/cat-gravel.jpg");
        Category folding   = seedCategory("Folding",       "Xe đạp gấp, tiện mang theo",             "https://cdn.example.com/cat-folding.jpg");
        Category electric  = seedCategory("Electric",      "Xe đạp điện hỗ trợ pê-đan",             "https://cdn.example.com/cat-electric.jpg");
        Category kids      = seedCategory("Kids",          "Xe dành cho trẻ em",                     "https://cdn.example.com/cat-kids.jpg");

        // ── 4. Bikes ──────────────────────────────────────────────────────────
        // Seller 1 – bikes in various statuses
        Bike b1 = seedBike(seller1, trek,  Set.of(road),     "Trek Émonda SL 6 2022",
                "Xe đường trường nhẹ, khung carbon, groupset Shimano 105. Đã đi 2.000 km, còn rất mới.",
                "Émonda SL 6", 2022, 45_000L, 2000, "EXCELLENT", "ROAD", "54cm", "TP.HCM", BikeStatus.ACTIVE,    InspectionStatus.NONE);
        Bike b2 = seedBike(seller1, giant, Set.of(mountain), "Giant Trance X 29 2021",
                "MTB full-suspension 29 inch, phuộc Fox, đã thay bố thắng mới. Đi 3.500 km.",
                "Trance X 29", 2021, 35_000L, 3500, "GOOD",      "MTB",  "M",    "TP.HCM", BikeStatus.VERIFIED,  InspectionStatus.APPROVED);
        Bike b3 = seedBike(seller1, bianchi, Set.of(road, gravel), "Bianchi Infinito CV 2023",
                "Khung carbon endurance, công nghệ CV giảm rung, Shimano Ultegra Di2. Còn mới 98%.",
                "Infinito CV", 2023, 70_000L, 500, "LIKE_NEW",   "ROAD", "52cm", "Hà Nội", BikeStatus.ACTIVE,    InspectionStatus.NONE);
        Bike b4 = seedBike(seller1, trek,  Set.of(city),     "Trek FX 3 Disc 2022 – City",
                "Xe commuter nhẹ, thắng đĩa, đã thêm fender và giá đỡ sau. Đi 4.200 km.",
                "FX 3 Disc", 2022, 18_000L, 4200, "GOOD",        "CITY", "M",    "TP.HCM", BikeStatus.SOLD,      InspectionStatus.NONE);

        // Seller 2 – bikes
        Bike b5 = seedBike(seller2, specialized, Set.of(road),     "Specialized Allez Sprint Comp 2022",
                "Khung E5 Premium Alloy cứng, thắng đĩa, Shimano 105 R7000. Rất phù hợp crit racing.",
                "Allez Sprint Comp", 2022, 28_000L, 1800, "GOOD",  "ROAD", "54cm", "Hà Nội",  BikeStatus.ACTIVE,    InspectionStatus.REQUESTED);
        Bike b6 = seedBike(seller2, scott, Set.of(mountain),        "Scott Scale 940 2021",
                "MTB hardtail 29 inch, khung Alloy HMX, phuộc RockShox Judy. Đi 5.000 km.",
                "Scale 940", 2021, 22_000L, 5000, "FAIR",          "MTB",  "L",    "Đà Nẵng", BikeStatus.DRAFT,     InspectionStatus.NONE);
        Bike b7 = seedBike(seller2, giant, Set.of(road, gravel),    "Giant Revolt Advanced 2 2023",
                "Gravel bike khung carbon, groupset Shimano GRX, bánh 700c. Chỉ đi 800 km.",
                "Revolt Advanced 2", 2023, 42_000L, 800, "LIKE_NEW","GRAVEL","M",  "TP.HCM",  BikeStatus.VERIFIED,  InspectionStatus.APPROVED);
        Bike b8 = seedBike(seller2, polygon, Set.of(city, folding), "Polygon Urbano 5 2022",
                "Xe gấp nhôm, 7 tốc độ Shimano, phuộc trước. Thích hợp đi làm.",
                "Urbano 5", 2022, 8_500L, 2200, "GOOD",            "FOLDING","S", "TP.HCM",  BikeStatus.ACTIVE,    InspectionStatus.NONE);

        // Seller 3 – bikes
        Bike b9  = seedBike(seller3, merida, Set.of(mountain),      "Merida Big.Nine 300 2021",
                "Hardtail MTB 29 inch, nhôm, Shimano Altus 16 tốc độ. Đi 6.000 km, cần tra nhớt phuộc.",
                "Big.Nine 300", 2021, 12_000L, 6000, "FAIR",       "MTB",  "M",    "Cần Thơ", BikeStatus.ACTIVE,    InspectionStatus.NONE);
        Bike b10 = seedBike(seller3, fuji, Set.of(road),            "Fuji Roubaix 1.3 2020",
                "Xe đường trường nhôm cứng cáp, Shimano Tiagra 20 tốc độ. Đi 8.000 km.",
                "Roubaix 1.3", 2020, 9_000L, 8000, "FAIR",         "ROAD", "56cm", "Hà Nội",  BikeStatus.CANCELLED, InspectionStatus.NONE);
        Bike b11 = seedBike(seller3, trek, Set.of(road),            "Trek Domane AL 5 2022",
                "Endurance road bike nhôm, thắng đĩa, Shimano 105. Mới đi 1.200 km.",
                "Domane AL 5", 2022, 24_000L, 1200, "GOOD",         "ROAD", "54cm", "TP.HCM",  BikeStatus.ACTIVE,    InspectionStatus.IN_PROGRESS);

        // ── 5. Orders ─────────────────────────────────────────────────────────
        // Completed order – buyer1 mua b2 (VERIFIED) từ seller1
        Order o1 = seedOrder(buyer1, b2, 35_000L, OrderStatus.COMPLETED, "IDEM-001",
                LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(13));

        // Completed order – buyer2 mua b4 (SOLD) từ seller1
        Order o2 = seedOrder(buyer2, b4, 18_000L, OrderStatus.COMPLETED, "IDEM-002",
                LocalDateTime.now().minusDays(15), LocalDateTime.now().minusDays(8));

        // Completed order – buyer3 mua b7 (VERIFIED) từ seller2
        Order o3 = seedOrder(buyer3, b7, 42_000L, OrderStatus.COMPLETED, "IDEM-003",
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(3));

        // Escrowed order – buyer1 đang chờ seller giao
        Order o4 = seedOrder(buyer1, b1, 45_000L, OrderStatus.ESCROWED, "IDEM-004",
                null, LocalDateTime.now().minusDays(2));

        // Delivered order – seller giao xong, chờ buyer confirm
        Order o5 = seedOrder(buyer2, b5, 28_000L, OrderStatus.DELIVERED, "IDEM-005",
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(5));

        // Return requested
        Order o6 = seedOrder(buyer4, b3, 70_000L, OrderStatus.RETURN_REQUESTED, "IDEM-006",
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(8));

        // Disputed
        Order o7 = seedOrder(buyer3, b9,  12_000L, OrderStatus.DISPUTED, "IDEM-007",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(4));

        // Cancelled
        Order o8 = seedOrder(buyer4, b10,  9_000L, OrderStatus.CANCELLED, "IDEM-008",
                null, LocalDateTime.now().minusDays(7));

        // ── 6. Inspection requests ────────────────────────────────────────────
        InspectionRequest ir1 = seedInspectionRequest(b2, inspector1, RequestStatus.INSPECTED,
                500L, LocalDate.now().minusDays(25), "MORNING",
                "45 Nguyễn Huệ, Q.1", "0901000002", "Kiểm tra tổng thể xe trước bán",
                LocalDateTime.now().minusDays(25), LocalDateTime.now().minusDays(24));

        InspectionRequest ir2 = seedInspectionRequest(b7, inspector2, RequestStatus.APPROVED,
                500L, LocalDate.now().minusDays(18), "AFTERNOON",
                "88 Điện Biên Phủ, Q.3", "0901000003", "Kiểm tra xe mới về",
                LocalDateTime.now().minusDays(18), LocalDateTime.now().minusDays(17));

        InspectionRequest ir3 = seedInspectionRequest(b5, inspector1, RequestStatus.ASSIGNED,
                500L, LocalDate.now().plusDays(2), "MORNING",
                "45 Nguyễn Huệ, Q.1", "0901000003", "Yêu cầu kiểm định trước khi đăng bán",
                null, LocalDateTime.now().minusDays(1));

        InspectionRequest ir4 = seedInspectionRequest(b11, inspector2, RequestStatus.IN_PROGRESS,
                500L, LocalDate.now(), "AFTERNOON",
                "22 Cách Mạng Tháng 8, Q.10", "0901000004", "Đang kiểm tra tại nhà",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));

        InspectionRequest ir5 = seedInspectionRequest(b9, null, RequestStatus.REQUESTED,
                500L, LocalDate.now().plusDays(3), "MORNING",
                "55 Hai Bà Trưng, Q.1", "0901000004", "Chờ phân công kiểm định viên",
                null, LocalDateTime.now());

        InspectionRequest ir6 = seedInspectionRequest(b1, inspector1, RequestStatus.REJECTED,
                500L, LocalDate.now().minusDays(30), "MORNING",
                "45 Nguyễn Huệ, Q.1", "0901000002", "Xe không đạt tiêu chuẩn",
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(29));

        // ── 7. Inspection reports ─────────────────────────────────────────────
        seedInspectionReport(ir1, "TỐT – không uốn cong, không nứt", "Shimano 105 đầy đủ, còn ~70%", "Bánh trước vành còn tốt, lốp ~60%", 82, RequestStatus.APPROVED,
                "Xe đạt chất lượng, khuyến nghị thay lốp trong 500 km tới");
        seedInspectionReport(ir2, "RẤT TỐT – carbon không có vết nứt", "GRX đầy đủ, còn ~90%",         "Bánh 700c mới, lốp còn ~90%",       94, RequestStatus.APPROVED,
                "Xe như mới, không phát hiện lỗi. Đạt chuẩn VERIFIED");
        seedInspectionReport(ir6, "CÒN OK – nhưng có vết trầy nhỏ",   "Nhớt phuộc cạn, cần thay",   "Vành sau bị bẹp nhẹ",               45, RequestStatus.REJECTED,
                "Từ chối: vành sau biến dạng, cần sửa trước khi đăng bán");

        // ── 8. Disputes ────────────────────────────────────────────────────────
        seedDispute(o7, buyer3, "Hàng nhận được khác mô tả, bố thắng không còn hoạt động đúng",
                Dispute.DisputeStatus.OPEN, null, null);
        seedDispute(o6, buyer4, "Người bán không hợp tác xác nhận hoàn hàng sau khi đã gửi lại",
                Dispute.DisputeStatus.INVESTIGATING, "Admin đang xem xét bằng chứng hai bên", LocalDateTime.now().minusHours(12));

        // ── 9. Reviews ────────────────────────────────────────────────────────
        seedReview(buyer1, seller1, o1, 5, "Seller rất chuyên nghiệp, xe đúng mô tả, giao hàng nhanh. Rất hài lòng!");
        seedReview(buyer2, seller1, o2, 4, "Xe tốt, seller nhiệt tình. Giao hàng hơi chậm một ngày nhưng vẫn ok.");
        seedReview(buyer3, seller2, o3, 5, "Xe hoàn hảo, đúng tình trạng mô tả. Seller rất uy tín, sẽ mua lại lần sau.");

        // ── 10. User Reports ──────────────────────────────────────────────────
        seedUserReport(buyer1, b6, null,    Report.ReportType.FRAUD,               "Ảnh xe không khớp với thực tế, nghi ngờ hàng giả",                    Report.ReportStatus.PENDING,   null);
        seedUserReport(buyer2, b10, null,   Report.ReportType.FAKE_ITEM,           "Xe rao bán nhưng không có thực, gọi không ai bắt máy",                 Report.ReportStatus.REVIEWING, "Admin đang liên hệ người bán");
        seedUserReport(buyer3, null, seller3, Report.ReportType.SPAM,              "Tài khoản này spam tin nhắn đề nghị mua ngoài app",                   Report.ReportStatus.RESOLVED,  "Đã cảnh báo người dùng");
        seedUserReport(buyer4, b8, null,    Report.ReportType.INAPPROPRIATE,       "Mô tả xe có ngôn ngữ không phù hợp",                                  Report.ReportStatus.PENDING,   null);
        seedUserReport(buyer1, null, seller2, Report.ReportType.OFFENSIVE_LANGUAGE,"Người bán dùng ngôn ngữ xúc phạm trong chat",                         Report.ReportStatus.REJECTED,  "Xem xét lại không đủ bằng chứng");

        // ── 11. Point transactions ────────────────────────────────────────────
        seedPointTx(buyer1, 100_000L, TransactionType.DEPOSIT,       "DEP-B1-001", TransactionStatus.SUCCESS, "Nạp điểm qua VNPay");
        seedPointTx(buyer1,  45_000L, TransactionType.ESCROW_HOLD,   "ORD-4",       TransactionStatus.SUCCESS, "Khóa điểm đặt mua xe Trek Émonda");
        seedPointTx(buyer1,  35_000L, TransactionType.ESCROW_HOLD,   "ORD-1",       TransactionStatus.SUCCESS, "Khóa điểm đặt mua xe Giant Trance");
        seedPointTx(seller1, 34_300L, TransactionType.ESCROW_RELEASE,"ORD-1",       TransactionStatus.SUCCESS, "Nhận điểm (sau 2% hoa hồng) Đơn #1");
        seedPointTx(buyer2,  80_000L, TransactionType.DEPOSIT,       "DEP-B2-001", TransactionStatus.SUCCESS, "Nạp điểm qua VNPay");
        seedPointTx(buyer2,  18_000L, TransactionType.ESCROW_HOLD,   "ORD-2",       TransactionStatus.SUCCESS, "Khóa điểm đặt mua xe Trek FX");
        seedPointTx(seller1, 17_640L, TransactionType.ESCROW_RELEASE,"ORD-2",       TransactionStatus.SUCCESS, "Nhận điểm (sau 2% hoa hồng) Đơn #2");
        seedPointTx(buyer3,  60_000L, TransactionType.DEPOSIT,       "DEP-B3-001", TransactionStatus.SUCCESS, "Nạp điểm qua VNPay");
        seedPointTx(buyer3,  42_000L, TransactionType.ESCROW_HOLD,   "ORD-3",       TransactionStatus.SUCCESS, "Khóa điểm đặt mua xe Giant Revolt");
        seedPointTx(seller2, 41_160L, TransactionType.ESCROW_RELEASE,"ORD-3",       TransactionStatus.SUCCESS, "Nhận điểm (sau 2% hoa hồng) Đơn #3");
        seedPointTx(buyer1,   500L,  TransactionType.SPEND,          "INSP-1",      TransactionStatus.SUCCESS, "Phí yêu cầu kiểm định xe Giant Trance");
        seedPointTx(inspector1, 500L, TransactionType.EARN,          "INSP-1",      TransactionStatus.SUCCESS, "Phí kiểm định nhận được");
        seedPointTx(buyer4,  20_000L, TransactionType.DEPOSIT,       "DEP-B4-001", TransactionStatus.SUCCESS, "Nạp điểm qua VNPay");

        // ── 12. Components (linh kiện) ──────────────────────────────────────
        seedComponent("Shimano 105 R7000",       "Groupset 11 tốc độ tầm trung cao, phổ biến nhất trong xe road");
        seedComponent("Shimano Ultegra R8000",    "Groupset 11 tốc độ cao cấp, nhẹ hơn 105, bán chuyên");
        seedComponent("Shimano GRX 810",          "Groupset gravel chuyên dụng, đĩa, 11 tốc độ");
        seedComponent("Shimano Tiagra 4700",      "Groupset 10 tốc độ entry-level, bền bỉ");
        seedComponent("Shimano Altus M315",       "Groupset MTB entry-level 8 tốc độ");
        seedComponent("SRAM Rival eTap AXS",      "Groupset wireless road cao cấp, 12 tốc độ");
        seedComponent("Fox 34 Float",             "Phuộc MTB 34mm 130mm travel, Kashima coating");
        seedComponent("RockShox Judy Silver",     "Phuộc MTB solo air 100mm, nhẹ và ổn định");
        seedComponent("Mavic Aksium Elite",       "Bộ bánh road alloy 700c, đủ dùng cho hầu hết rider");
        seedComponent("DT Swiss M1900",           "Vành MTB alloy 29 inch, tubeless ready");
        seedComponent("Continental Grand Prix 5000", "Lốp road 700x25c, bám đường tốt, lăn nhẹ");
        seedComponent("Maxxis Ardent 29x2.25",   "Lốp MTB đa năng, bám địa hình tốt");
        seedComponent("Fizik Arione R3",          "Yên xe road carbon rail, ergonomic");
        seedComponent("PRO Vibe 31.8 400mm",      "Ghi-đông road alloy nhẹ, drop 128mm");
        seedComponent("Shimano MT200",            "Thắng đĩa hydraulic entry MTB");

        // ── 13. Posts (tin đăng) ──────────────────────────────────────────────
        // Mỗi bike ACTIVE/VERIFIED cần có 1 Post ACTIVE
        seedPost(seller1, b1, "🚴 Xe đạp road Trek Émonda carbon, đi nhẹ như gió! Bán gấp do không có thời gian đạp.",      Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        seedPost(seller1, b2, "🏔️ Giant Trance full-sus 29er, đã qua kiểm định. Đi rừng hay trail đều Ok!",              Post.ListingType.VERIFIED, Post.PostStatus.ACTIVE);
        seedPost(seller1, b3, "✨ Bianchi Infinito mới 98%, xe endurance hoàn hảo cho các chuyến dài.",                   Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        seedPost(seller2, b5, "🔵 Specialized Allez Sprint nhôm cứng, thích hợp crit race & đua nhóm.",                   Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        seedPost(seller2, b7, "🌿 Giant Revolt gravel carbon, mới 95%, đã kiểm định. Đi đường đất hay asphalt đều tốt!",  Post.ListingType.VERIFIED, Post.PostStatus.ACTIVE);
        seedPost(seller2, b8, "🏙️ Polygon Urbano gấp gọn, 7 tốc độ, phù hợp đi làm kết hợp xe bus/tàu điện.",           Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        seedPost(seller3, b9, "⛰️ Merida Big.Nine MTB 29er nhôm, giá sinh viên, thích hợp trail nhẹ.",                   Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        seedPost(seller3, b11,"🔴 Trek Domane road nhôm, thắng đĩa, đi mới 1.200km. Xe đẹp, giá hợp lý!",               Post.ListingType.STANDARD, Post.PostStatus.ACTIVE);
        // Post đã cancelled (xe sold/cancelled)
        seedPost(seller1, b4, "Trek FX 3 Disc city bike – đã bán",         Post.ListingType.STANDARD, Post.PostStatus.CANCELLED);
        seedPost(seller3, b10,"Fuji Roubaix 2020 – đã tháo tin",           Post.ListingType.STANDARD, Post.PostStatus.CANCELLED);

        System.out.println("✅ DataInitializer: seed hoàn tất.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User seedUser(String email, String fullName, String phone, String address,
                          User.UserRole role, String status, Long availablePoints) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("password123"));
        u.setFullName(fullName);
        u.setPhone(phone);
        u.setAddress(address);
        u.setRole(role);
        u.setStatus(status);
        u.setIsVerified(true);
        if (role == User.UserRole.SELLER) {
            u.setShopName(fullName + " Shop");
            u.setShopDescription("Chuyên mua bán xe đạp chất lượng cao tại TP.HCM");
            u.setUpgradedToSellerAt(LocalDateTime.now().minusDays(60));
        }
        User saved = userRepository.save(u);
        if (!walletRepository.existsById(saved.getId())) {
            UserWallet w = new UserWallet();
            w.setUser(saved);
            w.setAvailablePoints(availablePoints);
            w.setFrozenPoints(0L);
            walletRepository.save(w);
        }
        System.out.println("Seeded user: " + email);
        return saved;
    }

    private Brand seedBrand(String name, String description) {
        return brandRepository.findByName(name).orElseGet(() -> {
            Brand b = new Brand();
            b.setName(name);
            b.setDescription(description);
            return brandRepository.save(b);
        });
    }

    private Category seedCategory(String name, String description, String imgUrl) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setDescription(description);
            c.setImgUrl(imgUrl);
            return categoryRepository.save(c);
        });
    }

    private Bike seedBike(User seller, Brand brand, Set<Category> cats, String title,
                          String description, String model, int year, long pricePoints,
                          int mileage, String condition, String bikeType,
                          String frameSize, String location,
                          BikeStatus status, InspectionStatus inspStatus) {
        // idempotency: skip if seller already has a bike with this exact title
        boolean exists = bikeRepository.findBySellerId(seller.getId(),
                org.springframework.data.domain.Pageable.unpaged())
                .stream().anyMatch(b -> b.getTitle().equals(title));
        if (exists) {
            return bikeRepository.findBySellerId(seller.getId(),
                    org.springframework.data.domain.Pageable.unpaged())
                    .stream().filter(b -> b.getTitle().equals(title)).findFirst().orElseThrow();
        }
        Bike bike = new Bike();
        bike.setSeller(seller);
        bike.setBrand(brand);
        bike.setCategories(cats);
        bike.setTitle(title);
        bike.setDescription(description);
        bike.setModel(model);
        bike.setYear(year);
        bike.setPricePoints(pricePoints);
        bike.setMileage(mileage);
        bike.setCondition(condition);
        bike.setBikeType(bikeType);
        bike.setFrameSize(frameSize);
        bike.setLocation(location);
        bike.setStatus(status);
        bike.setInspectionStatus(inspStatus);
        bike.setFeatures("Thắng đĩa, ghi-đông carbon, yên xe chất lượng cao");
        bike.setViews((int)(Math.random() * 200) + 10);
        return bikeRepository.save(bike);
    }

    private Order seedOrder(User buyer, Bike bike, long amount, OrderStatus status,
                            String idempotencyKey, LocalDateTime deliveredAt, LocalDateTime createdAt) {
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) return
                orderRepository.findAll().stream()
                        .filter(o -> idempotencyKey.equals(o.getIdempotencyKey()))
                        .findFirst().orElseThrow();
        Order o = new Order();
        o.setBuyer(buyer);
        o.setBike(bike);
        o.setAmountPoints(amount);
        o.setStatus(status);
        o.setIdempotencyKey(idempotencyKey);
        o.setDeliveredAt(deliveredAt);
        o.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        o.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(o);
    }

    private InspectionRequest seedInspectionRequest(Bike bike, User inspector, RequestStatus status,
                                                     long feePoints, LocalDate preferredDate,
                                                     String timeSlot, String address, String phone,
                                                     String notes, LocalDateTime startedAt, LocalDateTime createdAt) {
        boolean exists = inspectionRepository.findAll().stream()
                .anyMatch(r -> r.getBike().getId().equals(bike.getId()) && r.getStatus() == status);
        if (exists) return inspectionRepository.findAll().stream()
                .filter(r -> r.getBike().getId().equals(bike.getId()) && r.getStatus() == status)
                .findFirst().orElseThrow();
        InspectionRequest ir = new InspectionRequest();
        ir.setBike(bike);
        ir.setInspector(inspector);
        ir.setStatus(status);
        ir.setFeePoints(feePoints);
        ir.setPreferredDate(preferredDate);
        ir.setPreferredTimeSlot(timeSlot);
        ir.setAddress(address);
        ir.setContactPhone(phone);
        ir.setNotes(notes);
        ir.setStartedAt(startedAt);
        ir.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        ir.setUpdatedAt(LocalDateTime.now());
        if (status == RequestStatus.INSPECTED || status == RequestStatus.APPROVED || status == RequestStatus.REJECTED) {
            ir.setCompletedAt(createdAt != null ? createdAt.plusHours(3) : LocalDateTime.now());
        }
        return inspectionRepository.save(ir);
    }

    private void seedInspectionReport(InspectionRequest request, String frame, String groupset,
                                      String wheel, int score, RequestStatus decision, String comments) {
        if (inspectionReportRepository.findByRequestId(request.getId()).isPresent()) return;
        InspectionReport rpt = new InspectionReport();
        rpt.setRequest(request);
        rpt.setFrameCondition(frame);
        rpt.setGroupsetCondition(groupset);
        rpt.setWheelCondition(wheel);
        rpt.setOverallScore(score);
        rpt.setAdminDecision(decision);
        rpt.setComments(comments);
        inspectionReportRepository.save(rpt);
    }

    private void seedDispute(Order order, User reporter, String reason,
                             Dispute.DisputeStatus status, String resolutionNote, LocalDateTime resolvedAt) {
        boolean exists = disputeRepository.findByOrderId(order.getId()).stream()
                .anyMatch(d -> d.getReason().equals(reason));
        if (exists) return;
        Dispute d = new Dispute();
        d.setOrder(order);
        d.setReporter(reporter);
        d.setReason(reason);
        d.setStatus(status);
        d.setResolutionNote(resolutionNote);
        d.setResolvedAt(resolvedAt);
        disputeRepository.save(d);
    }

    private void seedReview(User reviewer, User seller, Order order, int rating, String comment) {
        if (reviewRepository.existsByOrderId(order.getId())) return;
        Review r = new Review();
        r.setReviewer(reviewer);
        r.setSeller(seller);
        r.setOrder(order);
        r.setRating(rating);
        r.setComment(comment);
        reviewRepository.save(r);
    }

    private void seedUserReport(User reporter, Bike bike, User reportedUser,
                                Report.ReportType type, String description,
                                Report.ReportStatus status, String adminNote) {
        boolean exists = userReportRepository.findAll().stream()
                .anyMatch(r -> r.getReporter().getId().equals(reporter.getId())
                        && r.getDescription().equals(description));
        if (exists) return;
        Report rpt = new Report();
        rpt.setReporter(reporter);
        rpt.setBike(bike);
        rpt.setUser(reportedUser);
        rpt.setReportType(type);
        rpt.setDescription(description);
        rpt.setStatus(status);
        rpt.setAdminNote(adminNote);
        if (status == Report.ReportStatus.RESOLVED || status == Report.ReportStatus.REJECTED) {
            rpt.setResolvedAt(LocalDateTime.now().minusDays(1));
        }
        userReportRepository.save(rpt);
    }

    private void seedPointTx(User user, long amount, TransactionType type,
                              String referenceId, TransactionStatus status, String remarks) {
        if (pointTransactionRepository.findByReferenceId(referenceId).isPresent()) return;
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setReferenceId(referenceId);
        tx.setStatus(status);
        tx.setRemarks(remarks);
        pointTransactionRepository.save(tx);
    }

    private void seedComponent(String name, String description) {
        boolean exists = componentRepository.findAll().stream()
                .anyMatch(c -> c.getName().equals(name));
        if (exists) return;
        Component c = new Component();
        c.setName(name);
        c.setDescription(description);
        componentRepository.save(c);
    }

    private void seedPost(User seller, Bike bike, String caption,
                          Post.ListingType listingType, Post.PostStatus status) {
        boolean exists = postRepository.findBySellerId(seller.getId(),
                org.springframework.data.domain.Pageable.unpaged())
                .stream().anyMatch(p -> p.getBike().getId().equals(bike.getId()));
        if (exists) return;
        Post p = new Post();
        p.setSeller(seller);
        p.setBike(bike);
        p.setCaption(caption);
        p.setListingType(listingType);
        p.setStatus(status);
        postRepository.save(p);
    }
}
