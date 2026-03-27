package com.bikeexchange.config;

import com.bikeexchange.model.*;
import com.bikeexchange.model.Bike.BikeStatus;
import com.bikeexchange.model.Bike.InspectionStatus;
import com.bikeexchange.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private BikeRepository bikeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UserWalletRepository walletRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private OrderRuleConfigRepository orderRuleConfigRepository;
    @Autowired private PasswordEncoder passwordEncoder;

        @Value("${app.order-rule.defaults.return-window.days:14}")
        private int defaultReturnWindowDays;

        @Value("${app.order-rule.defaults.return-window.hours:0}")
        private int defaultReturnWindowHours;

        @Value("${app.order-rule.defaults.return-window.minutes:0}")
        private int defaultReturnWindowMinutes;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Starting Data Seeding (Preserving existing data)...");
        
        seedOrderRuleConfig();

        System.out.println("Seeding requested data if not exists...");

        // 1. Admin & Inspector
        User admin = seedUser("admin@bikeexchange.com", "System Admin", "0901000001", "Admin Office", User.UserRole.ADMIN, "ACTIVE", 1_000_000L);
        seedUser("inspector@bikeexchange.com", "Senior Inspector", "0901000005", "Inspection Center", User.UserRole.INSPECTOR, "ACTIVE", 1_000_000L);

        // 2. 3 Sellers
        User seller1 = seedUser("seller1@bikeexchange.com", "Seller One", "0901000002", "Street 1, HCM", User.UserRole.SELLER, "ACTIVE", 1_000_000L);
        User seller2 = seedUser("seller2@bikeexchange.com", "Seller Two", "0901000003", "Street 2, Da Nang", User.UserRole.SELLER, "ACTIVE", 1_000_000L);
        User seller3 = seedUser("seller3@bikeexchange.com", "Seller Three", "0901000004", "Street 3, Ha Noi", User.UserRole.SELLER, "ACTIVE", 1_000_000L);

        // 3. 4 Buyers
        User buyer1 = seedUser("buyer1@bikeexchange.com", "Buyer One", "0901000007", "HCM City", User.UserRole.BUYER, "ACTIVE", 1_000_000L);
        seedUser("buyer2@bikeexchange.com", "Buyer Two", "0901000008", "Da Nang City", User.UserRole.BUYER, "ACTIVE", 1_000_000L);
        seedUser("buyer3@bikeexchange.com", "Buyer Three", "0901000009", "Ha Noi City", User.UserRole.BUYER, "ACTIVE", 1_000_000L);
        seedUser("buyer4@bikeexchange.com", "Buyer Four", "0901000010", "Can Tho City", User.UserRole.BUYER, "ACTIVE", 1_000_000L);

        // 4. 5 Brands
        Brand trek = seedBrand("Trek", "Premium US bikes");
        Brand giant = seedBrand("Giant", "World's largest bike manufacturer");
        Brand specialized = seedBrand("Specialized", "Racing and MTB specialists");
        Brand bianchi = seedBrand("Bianchi", "Iconic Italian heritage");
        Brand scott = seedBrand("Scott", "High-performance Swiss engineering");

        // 5. 5 Categories
        Category road = seedCategory("Road", "Fast on asphalt", "https://img.road.jpg");
        Category mountain = seedCategory("Mountain", "Off-road trail", "https://img.mtb.jpg");
        Category city = seedCategory("City", "Daily commuting", "https://img.city.jpg");
        Category electric = seedCategory("Electric", "E-bike assistance", "https://img.ebike.jpg");
        Category gravel = seedCategory("Gravel", "All-terrain adventure", "https://img.gravel.jpg");

        // 6. Each seller creates 5 bikes (total 15) with price < 50,000
        List<User> sellers = List.of(seller1, seller2, seller3);
        List<Brand> brands = List.of(trek, giant, specialized, bianchi, scott);
        List<Category> categories = List.of(road, mountain, city, electric, gravel);

        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            for (int b = 1; b <= 5; b++) {
                Brand brand = brands.get((s + b) % brands.size());
                Category cat = categories.get((s + b) % categories.size());
                long price = 20_000L + (b * 5000L); // Prices: 25k, 30k, 35k, 40k, 45k
                
                String title = seller.getFullName() + " - Bike #" + b + " " + brand.getName();
                seedBike(seller, brand, Set.of(cat), title, 
                        "High quality " + brand.getName() + " for sale. Very good condition.",
                        "Model-" + b, 2022, price, 1000 * b, "GOOD", cat.getName().toUpperCase(), "M", "Vietnam", 
                        BikeStatus.ACTIVE, InspectionStatus.NONE);
            }
        }

        System.out.println("✅ Data Initialization Complete. 10 Users and 15 Bikes seeded.");
    }

    // ── Helpers (Keeping existing ones but cleaned up) ───────────────────────────────

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
        u.setIsVerified(true);
        if (role == User.UserRole.SELLER) {
            u.setShopName(fullName + " Store");
            u.setShopDescription("Uy tín và chất lượng.");
        }
        User saved = userRepository.save(u);
        
        UserWallet w = new UserWallet();
        w.setUser(saved);
        w.setAvailablePoints(availablePoints);
        w.setFrozenPoints(0L);
        walletRepository.save(w);
        
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
        
        if (bikeRepository.findBySellerId(seller.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().anyMatch(b -> b.getTitle().equals(title))) {
            return bikeRepository.findBySellerId(seller.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .stream().filter(b -> b.getTitle().equals(title)).findFirst().orElseThrow();
        }
            private void seedOrderRuleConfig() {
                OrderRuleConfig config = orderRuleConfigRepository.findById(OrderRuleConfig.SINGLETON_ID)
                        .orElse(null);
        
                if (config == null) {
                    config = new OrderRuleConfig();
                    config.setId(OrderRuleConfig.SINGLETON_ID);
                    config.setCommissionRate(0.02d);
                    config.setSellerUpgradeFee(50000L);
                    config.setReturnWindowDays(defaultReturnWindowDays);
                    config.setReturnWindowHours(defaultReturnWindowHours);
                    config.setReturnWindowMinutes(defaultReturnWindowMinutes);
                    config.setBikePostFee(5000L);
                    config.setInspectionFee(200000L);
                    orderRuleConfigRepository.save(config);
                    System.out.println("Seeded Default OrderRuleConfig");
                } else {
                    // Repair existing config if missing new fields
                    boolean updated = false;
                    if (config.getBikePostFee() == null || config.getBikePostFee() <= 0) {
                        config.setBikePostFee(5000L);
                        updated = true;
                    }
                    if (config.getInspectionFee() == null || config.getInspectionFee() <= 0) {
                        config.setInspectionFee(200000L);
                        updated = true;
                    }
                    if (config.getReturnWindowHours() == null || config.getReturnWindowHours() < 0) {
                        config.setReturnWindowHours(defaultReturnWindowHours);
                        updated = true;
                    }
                    if (config.getReturnWindowMinutes() == null || config.getReturnWindowMinutes() < 0) {
                        config.setReturnWindowMinutes(defaultReturnWindowMinutes);
                        updated = true;
                    }
                    if (config.getReturnWindowDays() == null || config.getReturnWindowDays() <= 0) {
                        config.setReturnWindowDays(defaultReturnWindowDays);
                        updated = true;
                    }
                    if (updated) {
                        orderRuleConfigRepository.save(config);
                        System.out.println("Updated existing OrderRuleConfig with missing fields");
                    }
                }
            }
        bike.setSeller(seller);
        bike.setBrand(brand);
        bike.setCategories(new HashSet<>(cats));
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
        bike.setViews(10);
        
        Bike savedBike = bikeRepository.save(bike);
        
        BikeMedia img = new BikeMedia();
        img.setBike(savedBike);
        img.setUrl("https://api.dicebear.com/7.x/identicon/svg?seed=" + title);
        img.setType(BikeMedia.MediaType.IMAGE);
        img.setSortOrder(1);
        savedBike.setMedia(new ArrayList<>(List.of(img)));
        
        return bikeRepository.save(savedBike);
    }


    private void seedOrderRuleConfig() {
        OrderRuleConfig config = orderRuleConfigRepository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElse(new OrderRuleConfig());
        config.setId(OrderRuleConfig.SINGLETON_ID);
        config.setCommissionRate(0.02d);
        config.setSellerUpgradeFee(50000L);
        config.setReturnWindowDays(14);
        config.setBikePostFee(5000L);
        config.setInspectionFee(200000L);
        orderRuleConfigRepository.save(config);
    }
    private void seedWishlist(User buyer, Bike bike) {
        if (wishlistRepository.findByBuyerIdAndBikeId(buyer.getId(), bike.getId()).isPresent()) return;
        Wishlist w = new Wishlist();
        w.setBuyer(buyer);
        w.setBike(bike);
        w.setAddedAt(LocalDateTime.now());
        wishlistRepository.save(w);
    }

    private Conversation seedConversation(User buyer, User seller, Bike bike) {
        return conversationRepository.findByBikeIdAndBuyerId(bike.getId(), buyer.getId())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setBuyer(buyer);
                    c.setSeller(seller);
                    c.setBike(bike);
                    c.setCreatedAt(LocalDateTime.now());
                    c.setUpdatedAt(LocalDateTime.now());
                    return conversationRepository.save(c);
                });
    }

    private void seedMessage(Conversation conversation, User sender, String content) {
        boolean exists = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream().anyMatch(m -> m.getSender().getId().equals(sender.getId()) && m.getContent().equals(content));
        if (exists) return;
        Message m = new Message();
        m.setConversation(conversation);
        m.setSender(sender);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        messageRepository.save(m);
    }
<<<<<<< HEAD
=======

    private void seedOrderRuleConfig() {
        OrderRuleConfig config = orderRuleConfigRepository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElse(null);
        
        if (config == null) {
            config = new OrderRuleConfig();
            config.setId(OrderRuleConfig.SINGLETON_ID);
            config.setCommissionRate(0.02d);
            config.setSellerUpgradeFee(50000L);
            config.setReturnWindowDays(defaultReturnWindowDays);
            config.setReturnWindowHours(defaultReturnWindowHours);
            config.setReturnWindowMinutes(defaultReturnWindowMinutes);
            config.setBikePostFee(5000L);
            config.setInspectionFee(200000L);
            orderRuleConfigRepository.save(config);
            System.out.println("Seeded Default OrderRuleConfig");
        } else {
            // Repair existing config if missing new fields
            boolean updated = false;
            if (config.getBikePostFee() == null || config.getBikePostFee() <= 0) {
                config.setBikePostFee(5000L);
                updated = true;
            }
            if (config.getInspectionFee() == null || config.getInspectionFee() <= 0) {
                config.setInspectionFee(200000L);
                updated = true;
            }
                        if (config.getReturnWindowHours() == null || config.getReturnWindowHours() < 0) {
                                config.setReturnWindowHours(defaultReturnWindowHours);
                                updated = true;
                        }
                        if (config.getReturnWindowMinutes() == null || config.getReturnWindowMinutes() < 0) {
                                config.setReturnWindowMinutes(defaultReturnWindowMinutes);
                                updated = true;
                        }
                        if (config.getReturnWindowDays() == null || config.getReturnWindowDays() <= 0) {
                                config.setReturnWindowDays(defaultReturnWindowDays);
                                updated = true;
                        }
            if (updated) {
                orderRuleConfigRepository.save(config);
                System.out.println("Updated existing OrderRuleConfig with missing fields");
            }
        }
    }
>>>>>>> f4ee16a (27)
}
