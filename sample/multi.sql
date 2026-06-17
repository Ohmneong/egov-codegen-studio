-- 여러 테이블 일괄 생성 검증용 (DDL 하나에 CREATE TABLE 여러 개)

CREATE TABLE category (
    category_id   BIGINT AUTO_INCREMENT COMMENT '분류 ID',
    category_name VARCHAR(100) NOT NULL COMMENT '분류명',
    use_yn        CHAR(1) DEFAULT 'Y' COMMENT '사용 여부(Y/N)',
    PRIMARY KEY (category_id)
);

CREATE TABLE product (
    product_id    BIGINT AUTO_INCREMENT COMMENT '상품 ID',
    product_name  VARCHAR(200) NOT NULL COMMENT '상품명',
    price         DECIMAL(15,2) DEFAULT 0 COMMENT '판매가',
    category_id   BIGINT COMMENT '분류 ID',
    use_yn        CHAR(1) DEFAULT 'Y' COMMENT '사용 여부(Y/N)',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    PRIMARY KEY (product_id)
);
