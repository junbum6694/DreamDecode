package est.DreamDecode.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * 현재 로그인한 사용자의 ID를 반환합니다.
     * SecurityContextHolder에서 Authentication을 가져와 userId를 추출합니다.
     *
     * @return 현재 로그인한 사용자의 ID
     * @throws IllegalStateException 인증 정보가 없거나 지원되지 않는 타입인 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof String principalStr) {
            return Long.valueOf(principalStr);
        }
        throw new IllegalStateException("지원되지 않는 인증 주체 타입: " + principal.getClass());
    }

    /**
     * Authentication 객체에서 사용자 ID를 추출합니다.
     *
     * @param authentication Authentication 객체
     * @return 사용자 ID
     * @throws IllegalArgumentException 인증 정보가 없는 경우
     */
    public static Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return (Long) authentication.getPrincipal(); // JwtTokenFilter 에서 넣어준 userId
    }
}

