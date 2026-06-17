package com.hanbit.egovgen.service;

import java.nio.file.Path;

/**
 * 미리보기 항목: 생성될 파일 경로와, 그 자리에 이미 파일이 있는지 여부.
 *
 * @param path   생성될 파일 경로
 * @param exists 같은 경로에 기존 파일이 있어 덮어쓰게 되는지
 */
public record PreviewEntry(Path path, boolean exists) {}
