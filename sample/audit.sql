-- 감사 컬럼 포함 테스트용 DDL
CREATE TABLE `LETTN_NOTICE` (
    `NOTICE_ID`         VARCHAR(20)  NOT NULL COMMENT '공지ID',
    `TITLE`             VARCHAR(200) NOT NULL COMMENT '제목',
    `CONTENT`           VARCHAR(2000)         COMMENT '내용',
    `USE_AT`            CHAR(1)      DEFAULT 'Y' COMMENT '사용여부',
    `FRST_REGISTER_ID`  VARCHAR(20)           COMMENT '최초등록자ID',
    `FRST_REGIST_PNTTM` DATETIME              COMMENT '최초등록시점',
    `LAST_UPDUSR_ID`    VARCHAR(20)           COMMENT '최종수정자ID',
    `LAST_UPDT_PNTTM`   DATETIME              COMMENT '최종수정시점',
    PRIMARY KEY (`NOTICE_ID`)
);
