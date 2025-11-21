// src/main/java/est/DreamDecode/controller/UserProfileController.java
package est.DreamDecode.controller;

import est.DreamDecode.dto.*;
import est.DreamDecode.service.UserService;
import est.DreamDecode.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserService userService;

    /** 내 정보 조회 */
    @GetMapping
    public ResponseEntity<UserProfileResponse> getMe(Authentication authentication) {
        Long userId = SecurityUtil.getUserId(authentication);
        return ResponseEntity.ok(userService.getUserProfileResponse(userId));
    }

    /** 내 정보 수정 (이름/성별/생년월일) */
    @Operation(summary = "프로필 수정", description = "본인의 프로필 정보(이름, 성별, 생년월일)를 수정합니다. 이메일은 변경할 수 없습니다. 인증 필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패")
    })
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMe(
            Authentication authentication,
            @RequestBody @Valid UpdateProfileRequest body
    ) {
        Long userId = SecurityUtil.getUserId(authentication);
        UserProfileResponse updated = userService.updateProfile(userId, body);
        return ResponseEntity.ok(updated);
    }

    /** 현재 비밀번호 확인 */
    @Operation(summary = "현재 비밀번호 확인", description = "비밀번호 변경 전 현재 비밀번호를 확인합니다. 인증 필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 일치"),
            @ApiResponse(responseCode = "400", description = "비밀번호 불일치"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/check-password")
    public ResponseEntity<EmailVerificationResponse> checkPassword(
            Authentication authentication,
            @RequestBody @Valid CheckPasswordRequest body
    ) {
        Long userId = SecurityUtil.getUserId(authentication);
        boolean ok = userService.checkPassword(userId, body.currentPassword());
        if (!ok) {
            return ResponseEntity.badRequest()
                    .body(new EmailVerificationResponse("현재 비밀번호가 일치하지 않습니다."));
        }
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호가 확인되었습니다.")
        );
    }

    /** 비밀번호 변경 */
    @Operation(summary = "비밀번호 변경", description = "로그인한 상태에서 현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. 인증 필요.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치 또는 새 비밀번호 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/change-password")
    public ResponseEntity<EmailVerificationResponse> changePassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest body
    ) {
        Long userId = SecurityUtil.getUserId(authentication);
        userService.changePassword(userId, body.currentPassword(), body.newPassword());
        return ResponseEntity.ok(
                new EmailVerificationResponse("비밀번호가 변경되었습니다.")
        );
    }
}
