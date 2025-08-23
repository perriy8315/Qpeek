package org.qpeek.qpeek.domain.member.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * LoginId (값 객체)
 * <p>
 * <정책>
 * - 허용 문자: 소문자 a~z, 숫자 0~9
 * - 길이: 5 ~ 20자
 * - 금지: 대문자, 특수문자, 모든 공백(스페이스/탭/개행 등), null/empty/blank
 * - 입력 가공 금지: trim, toLowerCase 등 정규화 없이 그대로 검증/거절
 * <p>
 * <설계 메모>
 * - 최종 효과는 정규식 ^[a-z0-9]{5,20}$ 과 동일.
 */
@Getter
@Embeddable
@ToString(of = "value")
@EqualsAndHashCode(of = "value")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginId implements Serializable {

    @Serial
    private static final long serialVersionUID = 6108817878980919169L;

    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 20;

    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-z0-9]+$");
    private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s+$");                  // 전부 공백
    private static final Pattern WHITESPACE_CONTAIN = Pattern.compile("\\s");

    private static final String MSG_NULL = "loginId must not be null";
    private static final String MSG_EMPTY = "loginId must not be empty";
    private static final String MSG_LENGTH = "loginId length must be between " + MIN_LENGTH + " and " + MAX_LENGTH;
    private static final String MSG_BLANK = "loginId must not be blank (whitespace only is not allowed)";
    private static final String MSG_WHITE_SPACE = "loginId must not contain whitespace";
    private static final String MSG_ALLOWED_CHARS = "loginId must contain only lowercase letters and digits";


    @Column(name = "login_id", nullable = false, updatable = false, length = MAX_LENGTH)
    private String value;

    private LoginId(String value) {
        this.value = validLoginId(value);
    }

    public static LoginId of(String loginId) {
        return new LoginId(loginId);
    }

    private static String validLoginId(String value) {
        if (value == null) throw new IllegalArgumentException(MSG_NULL);
        if (value.isEmpty()) throw new IllegalArgumentException(MSG_EMPTY);

        if (WHITESPACE_ONLY.matcher(value).matches()) throw new IllegalArgumentException(MSG_BLANK);
        if (WHITESPACE_CONTAIN.matcher(value).find()) throw new IllegalArgumentException(MSG_WHITE_SPACE);

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) throw new IllegalArgumentException(MSG_LENGTH);
        if (!ALLOWED_CHARS.matcher(value).matches()) throw new IllegalArgumentException(MSG_ALLOWED_CHARS);

        return value;
    }
}