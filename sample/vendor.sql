DROP TABLE IF EXISTS vendor;

CREATE TABLE vendor (
    vendor_id          BIGINT AUTO_INCREMENT COMMENT '거래처 ID',
    vendor_code        VARCHAR(20) NOT NULL COMMENT '거래처 코드',
    vendor_name        VARCHAR(100) NOT NULL COMMENT '거래처명',
    ceo_name           VARCHAR(50) NOT NULL COMMENT '대표자명',
    business_no        VARCHAR(20) NOT NULL COMMENT '사업자등록번호',
    corporate_no       VARCHAR(20) COMMENT '법인등록번호',
    business_type      VARCHAR(100) COMMENT '업태',
    business_item      VARCHAR(100) COMMENT '종목',

    phone              VARCHAR(20) COMMENT '대표 전화번호',
    fax                VARCHAR(20) COMMENT '팩스번호',
    email              VARCHAR(100) COMMENT '대표 이메일',
    homepage           VARCHAR(200) COMMENT '홈페이지',

    zip_code           VARCHAR(10) COMMENT '우편번호',
    address            VARCHAR(255) COMMENT '기본 주소',
    detail_address     VARCHAR(255) COMMENT '상세 주소',

    manager_name       VARCHAR(50) COMMENT '담당자명',
    manager_phone      VARCHAR(20) COMMENT '담당자 연락처',
    manager_email      VARCHAR(100) COMMENT '담당자 이메일',

    contract_date      DATE COMMENT '계약일',
    credit_limit       DECIMAL(15,2) DEFAULT 0 COMMENT '여신 한도',

    grade              CHAR(1) DEFAULT 'B' COMMENT '거래처 등급(A/B/C)',
    use_yn             CHAR(1) DEFAULT 'Y' COMMENT '사용 여부(Y/N)',

    memo               TEXT COMMENT '비고',

    created_by         VARCHAR(30) COMMENT '등록자',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',

    updated_by         VARCHAR(30) COMMENT '수정자',
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (vendor_id),
    UNIQUE KEY uk_vendor_code (vendor_code),
    UNIQUE KEY uk_business_no (business_no),

    INDEX idx_vendor_name (vendor_name),
    INDEX idx_manager_name (manager_name),
    INDEX idx_use_yn (use_yn),
    INDEX idx_grade (grade)
)
COMMENT='거래처 관리';
