CREATE TABLE products (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    sku             VARCHAR(40)   NOT NULL,
    name            VARCHAR(120)  NOT NULL,
    category        VARCHAR(50)   NOT NULL DEFAULT 'uncategorized',
    price           DECIMAL(10,2) NOT NULL DEFAULT 0,            -- 금액은 부동소수점 대신 DECIMAL
    stock_quantity  INT           NOT NULL DEFAULT 0,
    is_active       TINYINT(1)    NOT NULL DEFAULT 1,            -- MySQL의 BOOLEAN = TINYINT(1)
    description     TEXT          NULL,                          -- NULL 허용(부분 업데이트 테스트용)
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,    -- UPDATE 시 자동 갱신
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku (sku)                             -- 중복 입력 테스트용 UNIQUE
) 