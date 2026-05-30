USE [master];
GO

/*IF EXISTS (SELECT name FROM sys.databases WHERE name = N'AutoWashProDB')
BEGIN
    ALTER DATABASE [AutoWashProDB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [AutoWashProDB];
END
GO
*/
CREATE DATABASE [AutoWashProDB];
GO

USE [AutoWashProDB];
GO

/* =========================
   1. USERS
========================= */
CREATE TABLE Users (
    UserID        INT IDENTITY(1,1) PRIMARY KEY,
    full_name     NVARCHAR(100) NOT NULL,
    email         NVARCHAR(100) NOT NULL UNIQUE,
    phone         NVARCHAR(20) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    role          NVARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER','STAFF','MANAGER','ADMIN')),
    status        NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','BANNED')),
    avatar_url    NVARCHAR(500) NULL,
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),
    last_login    DATETIME NULL
);
GO

/* =========================
   2. MEMBER TIERS
========================= */
CREATE TABLE MemberTiers (
    TierID                INT IDENTITY(1,1) PRIMARY KEY,
    name                  NVARCHAR(50) NOT NULL UNIQUE,
    min_points            INT NOT NULL DEFAULT 0,
    min_washes            INT NOT NULL DEFAULT 0,
    min_spend             DECIMAL(18,0) NOT NULL DEFAULT 0,
    points_per_100k_vnd   INT NOT NULL DEFAULT 1,
    discount_percent      DECIMAL(5,2) NOT NULL DEFAULT 0,
    advance_booking_hours INT NOT NULL DEFAULT 24,
    max_bookings_per_day  INT NOT NULL DEFAULT 1,
    priority              INT NOT NULL DEFAULT 0,
    created_at            DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* =========================
   3. CUSTOMERS
========================= */
CREATE TABLE Customers (
    CustomerID     INT IDENTITY(1,1) PRIMARY KEY,
    UserID         INT NOT NULL UNIQUE,
    address        NVARCHAR(255) NULL,
    tier_id        INT NULL,
    loyalty_points INT NOT NULL DEFAULT 0,
    total_spend    DECIMAL(18,0) NOT NULL DEFAULT 0,
    total_washes   INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID),
    FOREIGN KEY (tier_id) REFERENCES MemberTiers(TierID)
);
GO

/* =========================
   4. EMPLOYEES
   Dùng cho STAFF và MANAGER
========================= */
CREATE TABLE Employees (
    EmployeeID INT IDENTITY(1,1) PRIMARY KEY,
    UserID     INT NOT NULL UNIQUE,
    position   NVARCHAR(100) NULL,
    salary     DECIMAL(12,2) NULL,
    branch     NVARCHAR(100) NULL,
    hire_date  DATE NULL,
    is_active  BIT NOT NULL DEFAULT 1,  -- Nhân viên đang làm việc hay đã nghỉ
    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

/* =========================
   TRIGGER: tạo Customer
========================= */
CREATE TRIGGER trg_CreateCustomer
ON Users AFTER INSERT
AS
BEGIN
    INSERT INTO Customers (UserID, tier_id)
    SELECT UserID, 1
    FROM inserted
    WHERE role = 'CUSTOMER';
END;
GO

/* =========================
   TRIGGER: tạo Employee
========================= */
CREATE TRIGGER trg_CreateEmployee
ON Users AFTER INSERT
AS
BEGIN
    INSERT INTO Employees (UserID)
    SELECT UserID
    FROM inserted
    WHERE role IN ('STAFF','MANAGER');
END;
GO

/* =========================
   5. PASSWORD RESETS
========================= */
CREATE TABLE PasswordResets (
    ResetID    INT IDENTITY(1,1) PRIMARY KEY,
    UserID     INT NOT NULL,
    token      NVARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    used       BIT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

/* =========================
   [MỚI] 5B. REFRESH TOKENS
   Dùng cho JWT authentication
========================= */
CREATE TABLE RefreshTokens (
    TokenID    INT IDENTITY(1,1) PRIMARY KEY,
    UserID     INT NOT NULL,
    token      NVARCHAR(500) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    revoked    BIT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

/* =========================
   6. VEHICLE TYPES
========================= */
CREATE TABLE VehicleTypes (
    VehicleTypeID INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(50) NOT NULL UNIQUE,
    description   NVARCHAR(255) NULL,
    is_active     BIT NOT NULL DEFAULT 1
);
GO

/* =========================
   7. VEHICLES
========================= */
CREATE TABLE Vehicles (
    VehicleID     INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID    INT NOT NULL,
    VehicleTypeID INT NOT NULL,
    license_plate NVARCHAR(20) NOT NULL UNIQUE,
    brand         NVARCHAR(100) NULL,
    model         NVARCHAR(100) NULL,
    color         NVARCHAR(50) NULL,
    image_url     NVARCHAR(500) NULL,
    is_default    BIT NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (VehicleTypeID) REFERENCES VehicleTypes(VehicleTypeID)
);
GO

/* =========================
   8. SERVICES
========================= */
CREATE TABLE Services (
    ServiceID   INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(150) NOT NULL,
    description NVARCHAR(MAX) NULL,
    category    NVARCHAR(20) NOT NULL CHECK (category IN ('WASH','DETAILING','COATING','ADD_ON')),
    is_active   BIT NOT NULL DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* =========================
   9. SERVICE PRICES
========================= */
CREATE TABLE ServicePrices (
    ServicePriceID   INT IDENTITY(1,1) PRIMARY KEY,
    ServiceID        INT NOT NULL,
    VehicleTypeID    INT NOT NULL,
    price            DECIMAL(12,2) NOT NULL,
    duration_minutes INT NOT NULL,
    is_active        BIT NOT NULL DEFAULT 1,

    UNIQUE (ServiceID, VehicleTypeID),
    FOREIGN KEY (ServiceID) REFERENCES Services(ServiceID),
    FOREIGN KEY (VehicleTypeID) REFERENCES VehicleTypes(VehicleTypeID)
);
GO

/* =========================
   [MỚI] SERVICE LANES
   Tạo trước Bookings vì Bookings có FK → ServiceLanes
========================= */
CREATE TABLE ServiceLanes (
    LaneID             INT IDENTITY(1,1) PRIMARY KEY,
    name               NVARCHAR(50) NOT NULL,   -- VD: "Làn VIP", "Làn Thường 1"
    lane_type          NVARCHAR(10) NOT NULL     -- VIP / STANDARD
                       CHECK (lane_type IN ('VIP','STANDARD')),
    is_available       BIT NOT NULL DEFAULT 1,  -- Đang trống hay đang có xe
    current_booking_id INT NULL,                -- Booking đang xử lý (đặt trước)
    current_walkin_id  INT NULL,                -- WalkIn đang xử lý (vãng lai)
    entered_at         DATETIME NULL,           -- Thời điểm xe vào làn
    updated_at         DATETIME NOT NULL DEFAULT GETDATE()
    -- FK current_booking_id và current_walkin_id thêm sau bằng ALTER TABLE
);
GO

/* =========================
   10. TIME SLOTS
========================= */
CREATE TABLE TimeSlots (
    SlotID       INT IDENTITY(1,1) PRIMARY KEY,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    max_bookings INT NOT NULL DEFAULT 3,
    is_active    BIT NOT NULL DEFAULT 1,

    CHECK (end_time > start_time)
);
GO

/* =========================
   11. BOOKINGS
   [CẬP NHẬT] Thêm promotion_id, redemption_id, notes
========================= */
CREATE TABLE Bookings (
    BookingID     INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID    INT NOT NULL,
    VehicleID     INT NOT NULL,
    SlotID        INT NOT NULL,
    booking_date  DATE NOT NULL,
    status        NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')),
    booking_type  NVARCHAR(10) NOT NULL DEFAULT 'ADVANCE'
                  CHECK (booking_type IN ('ADVANCE','WALK_IN')), -- đặt trước / vãng lai
    qr_code       NVARCHAR(255) NULL UNIQUE,
    notes         NVARCHAR(500) NULL,         -- [MỚI] ghi chú của khách
    cancel_reason NVARCHAR(500) NULL,
    checkin_at    DATETIME NULL,
    start_at      DATETIME NULL,
    complete_at   DATETIME NULL,
    employee_id   INT NULL,
    lane_id       INT NULL,              -- Làn rửa xe được phân công
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (VehicleID) REFERENCES Vehicles(VehicleID),
    FOREIGN KEY (SlotID) REFERENCES TimeSlots(SlotID),
    FOREIGN KEY (employee_id) REFERENCES Employees(EmployeeID)
    -- FK lane_id sẽ thêm sau bằng ALTER TABLE
);
GO

/* =========================
   12. BOOKING SERVICES
========================= */
CREATE TABLE BookingServices (
    BookingServiceID    INT IDENTITY(1,1) PRIMARY KEY,
    BookingID           INT NOT NULL,
    ServicePriceID      INT NOT NULL,
    price_at_booking    DECIMAL(12,2) NOT NULL,
    duration_at_booking INT NOT NULL,

    UNIQUE (BookingID, ServicePriceID),
    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID),
    FOREIGN KEY (ServicePriceID) REFERENCES ServicePrices(ServicePriceID)
);
GO

/* =========================
   [MỚI] WALK-IN CUSTOMERS
   Khách vãng lai không đặt lịch trước
========================= */
CREATE TABLE WalkInCustomers (
    WalkInID       INT IDENTITY(1,1) PRIMARY KEY,
    phone          NVARCHAR(20) NULL,          -- SĐT để tìm lại lần sau
    full_name      NVARCHAR(100) NULL,         -- Tên khách (không bắt buộc)
    license_plate  NVARCHAR(20) NULL,          -- Biển số xe
    vehicle_type   NVARCHAR(20) NULL           -- MOTORBIKE / CAR
                   CHECK (vehicle_type IN ('MOTORBIKE','CAR')),
    lane_id        INT NULL,                   -- Làn được phân công
    employee_id    INT NULL,                   -- Nhân viên phụ trách
    status         NVARCHAR(20) NOT NULL DEFAULT 'WAITING'
                   CHECK (status IN ('WAITING','IN_PROGRESS','COMPLETED','CANCELLED')),
    service_total  DECIMAL(12,2) NULL,         -- Tổng tiền dịch vụ
    payment_method NVARCHAR(20) NULL
                   CHECK (payment_method IN ('CASH','PAYOS','BANK_TRANSFER')),
    payment_status NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                   CHECK (payment_status IN ('PENDING','PAID','CANCELLED')),
    note           NVARCHAR(500) NULL,         -- Ghi chú của nhân viên
    created_at     DATETIME NOT NULL DEFAULT GETDATE(),
    completed_at   DATETIME NULL,

    FOREIGN KEY (lane_id)     REFERENCES ServiceLanes(LaneID),
    FOREIGN KEY (employee_id) REFERENCES Employees(EmployeeID)
);
GO

/* =========================
   13. PROMOTIONS
========================= */
CREATE TABLE Promotions (
    PromotionID        INT IDENTITY(1,1) PRIMARY KEY,
    code               NVARCHAR(50) NOT NULL UNIQUE,
    name               NVARCHAR(150) NOT NULL,
    discount_type      NVARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT','FIXED')),
    discount_value     DECIMAL(12,2) NOT NULL,
    min_order_value    DECIMAL(12,2) NOT NULL DEFAULT 0,
    max_discount       DECIMAL(12,2) NULL,
    applicable_tier_id INT NULL,
    auto_apply         BIT NOT NULL DEFAULT 0,
    usage_limit        INT NULL,
    used_count         INT NOT NULL DEFAULT 0,
    start_at           DATETIME NOT NULL,
    end_at             DATETIME NOT NULL,
    is_active          BIT NOT NULL DEFAULT 1,
    created_at         DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (applicable_tier_id) REFERENCES MemberTiers(TierID),
    CHECK (end_at > start_at)
);
GO

/* =========================
   14. TRANSACTIONS
========================= */
CREATE TABLE Transactions (
    TransactionID   INT IDENTITY(1,1) PRIMARY KEY,
    BookingID       INT NOT NULL UNIQUE,
    CustomerID      INT NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    final_amount    DECIMAL(12,2) NOT NULL,
    payment_method  NVARCHAR(20) NOT NULL DEFAULT 'CASH'
                    CHECK (payment_method IN ('CASH','BANK_TRANSFER','PAYOS','WALLET')),
    payment_status  NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (payment_status IN ('PENDING','PAID','REFUNDED','FAILED','CANCELLED')),
    promotion_id    INT NULL,
    points_used     INT NOT NULL DEFAULT 0,
    paid_at         DATETIME NULL,
    created_at      DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (promotion_id) REFERENCES Promotions(PromotionID)
);
GO

/* =========================
   [MỚI] 14B. PAYMENT LOGS
   Lưu chi tiết giao dịch online (MoMo, VNPay, ...)
========================= */
CREATE TABLE PaymentLogs (
    PaymentLogID  INT IDENTITY(1,1) PRIMARY KEY,
    TransactionID INT NOT NULL,
    gateway       NVARCHAR(20) NOT NULL CHECK (gateway IN ('PAYOS','BANK_TRANSFER','WALLET')),
    gateway_ref   NVARCHAR(100) NULL,       -- mã tham chiếu từ cổng thanh toán
    amount        DECIMAL(12,2) NOT NULL,
    status        NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    raw_response  NVARCHAR(MAX) NULL,       -- JSON response từ cổng thanh toán
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (TransactionID) REFERENCES Transactions(TransactionID)
);
GO

/* =========================
   15. PROMOTION USAGES
========================= */
CREATE TABLE PromotionUsages (
    UsageID       INT IDENTITY(1,1) PRIMARY KEY,
    PromotionID   INT NOT NULL,
    CustomerID    INT NOT NULL,
    TransactionID INT NOT NULL,
    used_at       DATETIME NOT NULL DEFAULT GETDATE(),

    UNIQUE (PromotionID, TransactionID),
    FOREIGN KEY (PromotionID) REFERENCES Promotions(PromotionID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (TransactionID) REFERENCES Transactions(TransactionID)
);
GO

/* =========================
   16. LOYALTY LOGS
========================= */
CREATE TABLE LoyaltyLogs (
    LogID         INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID    INT NOT NULL,
    points_change INT NOT NULL,
    reason        NVARCHAR(255) NOT NULL,
    ref_type      NVARCHAR(20) NOT NULL CHECK (ref_type IN ('BOOKING','MANUAL','REDEEM','EXPIRE')),
    ref_id        INT NULL,
    expires_at    DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT GETDATE(),
    created_by    INT NULL,

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (created_by) REFERENCES Users(UserID)
);
GO

/* =========================
   17. TIER HISTORY
========================= */
CREATE TABLE TierHistory (
    HistoryID     INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID    INT NOT NULL,
    old_tier_id   INT NULL,
    new_tier_id   INT NOT NULL,
    change_reason NVARCHAR(30) NOT NULL CHECK (change_reason IN ('AUTO_UPGRADE','AUTO_DOWNGRADE','MANUAL')),
    changed_at    DATETIME NOT NULL DEFAULT GETDATE(),
    changed_by    INT NULL,

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (old_tier_id) REFERENCES MemberTiers(TierID),
    FOREIGN KEY (new_tier_id) REFERENCES MemberTiers(TierID),
    FOREIGN KEY (changed_by) REFERENCES Users(UserID)
);
GO

/* =========================
   18. REWARDS
========================= */
CREATE TABLE Rewards (
    RewardID         INT IDENTITY(1,1) PRIMARY KEY,
    name             NVARCHAR(150) NOT NULL,
    description      NVARCHAR(MAX) NULL,
    points_required  INT NOT NULL,
    reward_type      NVARCHAR(20) NOT NULL CHECK (reward_type IN ('DISCOUNT','FREE_SERVICE','GIFT')),
    discount_value   DECIMAL(12,2) NULL,
    service_price_id INT NULL,
    image_url        NVARCHAR(500) NULL,
    is_active        BIT NOT NULL DEFAULT 1,
    stock            INT NULL,
    expires_at       DATETIME NULL,
    created_at       DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (service_price_id) REFERENCES ServicePrices(ServicePriceID)
);
GO

/* =========================
   19. REWARD REDEMPTIONS
========================= */
CREATE TABLE RewardRedemptions (
    RedemptionID   INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID     INT NOT NULL,
    RewardID       INT NOT NULL,
    points_spent   INT NOT NULL,
    status         NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING','APPLIED','EXPIRED','CANCELLED')),
    booking_id     INT NULL,
    transaction_id INT NULL,
    redeemed_at    DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (RewardID) REFERENCES Rewards(RewardID),
    FOREIGN KEY (booking_id) REFERENCES Bookings(BookingID),
    FOREIGN KEY (transaction_id) REFERENCES Transactions(TransactionID)
);
GO

/* =========================
   20. NOTIFICATIONS
========================= */
CREATE TABLE Notifications (
    NotificationID INT IDENTITY(1,1) PRIMARY KEY,
    UserID         INT NOT NULL,
    type           NVARCHAR(30) NOT NULL
                   CHECK (type IN ('BOOKING_STATUS','REMINDER','PROMOTION','REWARD','TIER_UPGRADE','SUGGESTION')),
    title          NVARCHAR(255) NOT NULL,
    body           NVARCHAR(MAX) NULL,
    ref_type       NVARCHAR(50) NULL,
    ref_id         INT NULL,
    is_read        BIT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

/* =========================
   21. ACTIVITY LOGS
========================= */
CREATE TABLE ActivityLogs (
    LogID       INT IDENTITY(1,1) PRIMARY KEY,
    UserID      INT NULL,
    action      NVARCHAR(100) NOT NULL,
    entity_type NVARCHAR(50) NULL,
    entity_id   INT NULL,
    detail_json NVARCHAR(MAX) NULL,
    ip_address  NVARCHAR(50) NULL,
    created_at  DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

/* =========================
   23. CUSTOMER SUGGESTIONS
========================= */
CREATE TABLE CustomerSuggestions (
    SuggestionID    INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID      INT NOT NULL,
    suggestion_type NVARCHAR(30) NOT NULL CHECK (suggestion_type IN ('PROMOTION','REWARD','SERVICE')),
    title           NVARCHAR(255) NOT NULL,
    description     NVARCHAR(MAX) NULL,
    ref_id          INT NULL,
    score           DECIMAL(5,2) NULL,
    is_clicked      BIT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);
GO

/* =========================
   [MỚI] 24. FEEDBACKS
   Khách đánh giá sau khi hoàn tất dịch vụ
========================= */
CREATE TABLE Feedbacks (
    FeedbackID  INT IDENTITY(1,1) PRIMARY KEY,
    BookingID   INT NOT NULL UNIQUE,
    CustomerID  INT NOT NULL,
    rating      TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     NVARCHAR(1000) NULL,
    is_visible  BIT NOT NULL DEFAULT 1,   -- ẩn/hiện đánh giá (admin quản lý)
    created_at  DATETIME NOT NULL DEFAULT GETDATE(),

    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);
GO

/*
	CONTACTS
*/
CREATE TABLE Contacts (
    ContactID   INT IDENTITY(1,1) PRIMARY KEY,
    full_name   NVARCHAR(100) NOT NULL,
    email       NVARCHAR(100) NOT NULL,
    phone       NVARCHAR(20) NULL,
    message     NVARCHAR(MAX) NOT NULL,
    status      NVARCHAR(20) NOT NULL DEFAULT 'UNREAD'
                CHECK (status IN ('UNREAD','READ','RESOLVED')),
    reply       NVARCHAR(MAX) NULL,
    handled_by  INT NULL FOREIGN KEY REFERENCES Users(UserID),
    handled_at  DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT GETDATE()
);

/*
	CONTACTMESSAGES
*/
CREATE TABLE ContactMessages (
    MessageID   INT IDENTITY(1,1) PRIMARY KEY,
    ContactID   INT NOT NULL FOREIGN KEY REFERENCES Contacts(ContactID),
    sender_type NVARCHAR(20) NOT NULL CHECK (sender_type IN ('CUSTOMER','STAFF')),
    message     NVARCHAR(MAX) NOT NULL,
    sender_id   INT NULL FOREIGN KEY REFERENCES Users(UserID),
    created_at  DATETIME NOT NULL DEFAULT GETDATE()
);




/* ============================================================
   ALTER TABLE — Thêm FK vòng tròn sau khi tất cả bảng đã tạo
============================================================ */

-- Bookings.lane_id → ServiceLanes
ALTER TABLE Bookings
    ADD CONSTRAINT FK_Bookings_Lane
    FOREIGN KEY (lane_id) REFERENCES ServiceLanes(LaneID);
GO

-- ServiceLanes.current_booking_id → Bookings
ALTER TABLE ServiceLanes
    ADD CONSTRAINT FK_ServiceLanes_Booking
    FOREIGN KEY (current_booking_id) REFERENCES Bookings(BookingID);
GO

-- ServiceLanes.current_walkin_id → WalkInCustomers
ALTER TABLE ServiceLanes
    ADD CONSTRAINT FK_ServiceLanes_WalkIn
    FOREIGN KEY (current_walkin_id) REFERENCES WalkInCustomers(WalkInID);
GO

/* ============================================================
   SEED DATA
============================================================ */


INSERT INTO ServiceLanes (name, lane_type) VALUES
(N'Làn VIP',       'VIP'),
(N'Làn Thường 1',  'STANDARD'),
(N'Làn Thường 2',  'STANDARD'),
(N'Làn Thường 3',  'STANDARD'); -- dự phòng giờ cao điểm
GO

INSERT INTO MemberTiers
(name, min_points, min_washes, min_spend, points_per_100k_vnd, discount_percent, advance_booking_hours, max_bookings_per_day, priority)
VALUES
-- name = mã nội bộ (EN), advance_booking_hours giữ nguyên bản cũ
(N'Member',   0,    0,  0,         1, 0,  24,  1, 0),   -- Thường  : đặt trước 24h
(N'Silver',   500,  10, 5000000,   2, 5,  48,  2, 1),   -- Bạc     : đặt trước 48h
(N'Gold',     2000, 30, 20000000,  3, 10, 72,  3, 2),   -- Vàng    : đặt trước 72h
(N'Platinum', 5000, 60, 50000000,  5, 15, 168, 5, 3);   -- Bạch Kim: đặt trước 168h (7 ngày)
GO

INSERT INTO VehicleTypes (name, description)
VALUES
(N'MOTORBIKE', N'Xe máy'),
(N'CAR',       N'Xe hơi / ô tô');
GO

INSERT INTO TimeSlots (start_time, end_time, max_bookings)
VALUES
('07:00','08:00', 4),
('08:00','09:00', 4),
('09:00','10:00', 4),
('10:00','11:00', 4),
('13:00','14:00', 4),
('14:00','15:00', 4),
('15:00','16:00', 4),
('16:00','17:00', 4);
GO

INSERT INTO Services (name, description, category)
VALUES
(N'Rửa xe cơ bản',    N'Rửa ngoại thất cơ bản',                 'WASH'),
(N'Rửa xe cao cấp',   N'Rửa xe kỹ, kèm hút bụi nội thất',       'WASH'),
(N'Vệ sinh nội thất', N'Vệ sinh toàn bộ nội thất xe',            'DETAILING'),
(N'Đánh bóng xe',     N'Đánh bóng bề mặt sơn',                   'DETAILING'),
(N'Phủ wax',          N'Phủ wax bảo vệ sơn',                     'COATING'),
(N'Phủ ceramic',      N'Phủ ceramic bảo vệ sơn xe lâu dài',      'COATING');
GO

INSERT INTO ServicePrices (ServiceID, VehicleTypeID, price, duration_minutes)
VALUES
(1, 1,  30000,   15),
(1, 2,  100000,  30),
(2, 1,  60000,   25),
(2, 2,  200000,  45),
(3, 2,  150000,  60),
(4, 2,  500000,  90),
(5, 1,  80000,   30),
(5, 2,  400000,  60),
(6, 2,  1500000, 180);
GO

INSERT INTO Users (full_name, email, phone, password_hash, role, status)
VALUES
(N'Admin',          'admin@autowash.vn',   '0900000001', 'password123', 'ADMIN',    'ACTIVE'),
(N'Manager Minh',   'manager@autowash.vn', '0900000002', 'password123', 'MANAGER',  'ACTIVE'),
(N'Nhân viên A',    'staff@autowash.vn',   '0900000003', 'password123', 'STAFF',    'ACTIVE'),
(N'Nguyễn Văn An',  'an@gmail.com',        '0901111111', 'password123', 'CUSTOMER', 'ACTIVE'),
(N'Trần Thị Bình',  'binh@gmail.com',      '0902222222', 'password123', 'CUSTOMER', 'ACTIVE');
GO

UPDATE Employees
SET position  = N'Branch Manager',
    salary    = 15000000,
    branch    = N'Chi nhánh 1',
    hire_date = GETDATE()
WHERE UserID = (SELECT UserID FROM Users WHERE email = 'manager@autowash.vn');

UPDATE Employees
SET position  = N'Wash Staff',
    salary    = 8000000,
    branch    = N'Chi nhánh 1',
    hire_date = GETDATE()
WHERE UserID = (SELECT UserID FROM Users WHERE email = 'staff@autowash.vn');
GO

UPDATE Customers
SET address = N'Đồng Nai'
WHERE UserID = (SELECT UserID FROM Users WHERE email = 'an@gmail.com');

UPDATE Customers
SET address = N'TP. Hồ Chí Minh'
WHERE UserID = (SELECT UserID FROM Users WHERE email = 'binh@gmail.com');
GO

INSERT INTO Vehicles (CustomerID, VehicleTypeID, license_plate, brand, model, color, is_default)
VALUES
(1, 1, N'60-B1 12345', N'Honda',  N'Vision', N'Đen',   1),
(1, 2, N'60A-99999',   N'Toyota', N'Vios',   N'Trắng', 0),
(2, 1, N'59-C1 88888', N'Yamaha', N'Janus',  N'Xanh',  1);
GO

INSERT INTO Promotions
(code, name, discount_type, discount_value, min_order_value, max_discount, applicable_tier_id, auto_apply, usage_limit, start_at, end_at)
VALUES
(N'WELCOME10', N'Chào mừng khách hàng mới', 'PERCENT', 10, 0,      50000,  NULL, 0, 100, GETDATE(), DATEADD(MONTH,1,GETDATE())),
(N'SILVER50',  N'Ưu đãi hạng Silver',        'FIXED',   50000, 100000, NULL,   2,    1, 200, GETDATE(), DATEADD(MONTH,3,GETDATE())),
(N'WEEKEND20', N'Cuối tuần giảm 20%',         'PERCENT', 20, 50000,  100000, NULL, 1, 500, GETDATE(), DATEADD(MONTH,1,GETDATE()));
GO

INSERT INTO Rewards (name, description, points_required, reward_type, discount_value, stock)
VALUES
(N'Giảm 20.000đ',      N'Giảm 20.000đ cho lần sử dụng tiếp theo', 100, 'DISCOUNT', 20000, NULL),
(N'Giảm 50.000đ',      N'Giảm 50.000đ cho lần sử dụng tiếp theo', 250, 'DISCOUNT', 50000, NULL),
(N'Quà tặng móc khóa', N'Móc khóa thương hiệu AutoWash Pro',       200, 'GIFT',     NULL,  50);
GO

/* ============================================================
   VIEWS
============================================================ */

GO

CREATE VIEW vw_CustomerOverview AS
SELECT
    u.UserID,
    u.full_name,
    u.email,
    u.phone,
    c.CustomerID,
    c.loyalty_points,
    c.total_spend,
    c.total_washes,
    mt.name AS tier_name,
    mt.discount_percent,
    mt.advance_booking_hours,
    mt.max_bookings_per_day
FROM Users u
JOIN Customers c ON u.UserID = c.UserID
LEFT JOIN MemberTiers mt ON c.tier_id = mt.TierID;
GO

CREATE VIEW vw_BookingDetail AS
SELECT
    b.BookingID,
    b.booking_date,
    b.status,
    b.notes,
    u.full_name AS customer_name,
    u.phone,
    v.license_plate,
    vt.name AS vehicle_type,
    v.brand,
    v.model,
    ts.start_time,
    ts.end_time,
    su.full_name AS employee_name,
    t.final_amount,
    t.payment_status,
    t.payment_method
FROM Bookings b
JOIN Customers c ON b.CustomerID = c.CustomerID
JOIN Users u ON c.UserID = u.UserID
JOIN Vehicles v ON b.VehicleID = v.VehicleID
JOIN VehicleTypes vt ON v.VehicleTypeID = vt.VehicleTypeID
JOIN TimeSlots ts ON b.SlotID = ts.SlotID
LEFT JOIN Employees e ON b.employee_id = e.EmployeeID
LEFT JOIN Users su ON e.UserID = su.UserID
LEFT JOIN Transactions t ON b.BookingID = t.BookingID;
GO

CREATE VIEW vw_DailyRevenue AS
SELECT
    CAST(b.booking_date AS DATE) AS report_date,
    COUNT(b.BookingID)           AS total_bookings,
    SUM(t.final_amount)          AS total_revenue,
    SUM(t.discount_amount)       AS total_discount
FROM Bookings b
JOIN Transactions t ON b.BookingID = t.BookingID
WHERE b.status = 'COMPLETED'
  AND t.payment_status = 'PAID'
GROUP BY CAST(b.booking_date AS DATE);
GO

CREATE VIEW vw_ActivePoints AS
SELECT
    CustomerID,
    SUM(points_change) AS active_points
FROM LoyaltyLogs
WHERE expires_at IS NULL OR expires_at > GETDATE()
GROUP BY CustomerID;
GO

/* [MỚI] View thống kê đánh giá trung bình theo dịch vụ */
GO

CREATE VIEW vw_ServiceRating AS
SELECT
    s.ServiceID,
    s.name AS service_name,
    s.category,
    COUNT(f.FeedbackID)  AS total_reviews,
    AVG(CAST(f.rating AS DECIMAL(3,2))) AS avg_rating
FROM Feedbacks f
JOIN Bookings b ON f.BookingID = b.BookingID
JOIN BookingServices bs ON b.BookingID = bs.BookingID
JOIN ServicePrices sp ON bs.ServicePriceID = sp.ServicePriceID
JOIN Services s ON sp.ServiceID = s.ServiceID
WHERE f.is_visible = 1
GROUP BY s.ServiceID, s.name, s.category;
GO

/* ============================================================
   SELECT ALL  (để kiểm tra nhanh)
============================================================ */
 --SELECT * FROM Users;
-- SELECT * FROM MemberTiers;
-- SELECT * FROM Customers;
-- SELECT * FROM Employees;
-- SELECT * FROM VehicleTypes;
-- SELECT * FROM Vehicles;
-- SELECT * FROM Services;
-- SELECT * FROM ServicePrices;
-- SELECT * FROM TimeSlots;
-- SELECT * FROM ServiceLanes;
-- SELECT * FROM Bookings;
-- SELECT * FROM BookingServices;
-- SELECT * FROM Promotions;
-- SELECT * FROM Transactions;
-- SELECT * FROM PaymentLogs;
-- SELECT * FROM PromotionUsages;
-- SELECT * FROM LoyaltyLogs;
-- SELECT * FROM TierHistory;
-- SELECT * FROM Rewards;
-- SELECT * FROM RewardRedemptions;
-- SELECT * FROM Feedbacks;
-- SELECT * FROM Notifications;
-- SELECT * FROM ActivityLogs;
-- SELECT * FROM CustomerSuggestions;
-- SELECT * FROM WalkInCustomers;
-- SELECT * FROM PasswordResets;
-- SELECT * FROM RefreshTokens;	
-- SELECT * FROM Contacts;
-- SELECT * FROM ContactMessages;
-- SELECT * FROM vw_CustomerOverview;
-- SELECT * FROM vw_BookingDetail;
-- SELECT * FROM vw_DailyRevenue;
-- SELECT * FROM vw_ActivePoints;
-- SELECT * FROM vw_ServiceRating;
-- SELECT * FROM vw_BookingStatus;
-- SELECT * FROM vw_LaneStatus;



SELECT email, password_hash FROM Users;


	UPDATE Customers 
	SET total_washes = 60,
		total_spend = 50000000
	WHERE CustomerID = 2;

ALTER TABLE Contacts 
ALTER COLUMN message NVARCHAR(MAX) NULL;

SELECT TransactionID, payment_status, paid_at 
FROM Transactions 
ORDER BY TransactionID DESC