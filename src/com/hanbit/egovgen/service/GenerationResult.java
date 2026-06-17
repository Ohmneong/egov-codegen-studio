package com.hanbit.egovgen.service;

import com.hanbit.egovgen.model.TableMeta;

import java.nio.file.Path;
import java.util.List;

/**
 * 생성 결과 묶음. 콘솔/화면 출력에 의존하지 않는 순수 데이터.
 * CLI는 이 값으로 콘솔을 찍고, GUI는 같은 값으로 화면을 채운다.
 *
 * @param table     파싱된 테이블 메타 (파싱 요약 출력용)
 * @param files     생성된 파일 경로 목록
 * @param outputDir 산출물 출력 루트(절대경로)
 * @param listUrl   목록 화면 접속 URL
 * @param registUrl 등록 화면 접속 URL
 */
public record GenerationResult(
        TableMeta table,
        List<Path> files,
        Path outputDir,
        String listUrl,
        String registUrl
) {}
