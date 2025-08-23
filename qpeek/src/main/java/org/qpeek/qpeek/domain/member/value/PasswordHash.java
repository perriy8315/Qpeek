package org.qpeek.qpeek.domain.member.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.regex.Pattern;

/**
 * PasswordHash (값 객체)
 * <p>
 * <정책>
 * - 이 클래스는 이미 해싱된 비밀번호 문자열만 캡슐화한다. (원문 비밀번호의 규칙 검증/해싱은 서비스 레이어 책임)
 * - 공백 문자가 전혀 없는 문자열. (대/소문자, 숫자, 특수문자, 비-ASCII 포함은 제한하지 않음)
 * - 금지 정책 : null, empty(""), blank(전부 공백), 공백 포함, 길이(MAX_LENGTH) 초과.
 * <p>
 * <설계 메모>
 * - toString()은 항상 "PasswordHash(****)" 반환 (로그/디버그 노출을 방지)
 * - JSON 응답 차단은 엔티티 측 (Member.passwordHash 에서 @JsonIgnore 로 처리
 */
@Getter
@Embeddable
@EqualsAndHashCode(of = "value")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordHash {

    private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s+$");                  // 전부 공백
    private static final Pattern WHITESPACE_CONTAIN = Pattern.compile("\\s");

    private static final int MAX_LENGTH = 255;

    private static final String MSG_NULL = "passwordHash must not be null";
    private static final String MSG_EMPTY = "passwordHash must not be empty";
    private static final String MSG_LENGTH = "passwordHash length must be <= " + MAX_LENGTH;
    private static final String MSG_BLANK = "passwordHash must not be blank";
    private static final String MSG_WHITE_SPACE = "passwordHash must not contain whitespace";


    @Column(name = "password_hash", nullable = false, length = MAX_LENGTH)
    private String value;

    private PasswordHash(String value) {
        this.value = validPasswordHash(value);
    }

    public static PasswordHash of(String passwordHash) {
        return new PasswordHash(passwordHash);
    }

    private String validPasswordHash(String value) {
        if (value == null) throw new IllegalArgumentException(MSG_NULL);
        if (value.isEmpty()) throw new IllegalArgumentException(MSG_EMPTY);
        if (value.length() > MAX_LENGTH) throw new IllegalArgumentException(MSG_LENGTH);

        if (WHITESPACE_ONLY.matcher(value).matches()) throw new IllegalArgumentException(MSG_BLANK);
        if (WHITESPACE_CONTAIN.matcher(value).find()) throw new IllegalArgumentException(MSG_WHITE_SPACE);
        return value;
    }

    @Override
    public String toString() {
        return "PasswordHash(****)";
    }
}