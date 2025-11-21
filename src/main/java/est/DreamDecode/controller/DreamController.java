package est.DreamDecode.controller;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import est.DreamDecode.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DreamController {
  private final DreamService dreamService;

  // 전체 조회 (페이지네이션)
  @GetMapping("/api/dream")
  public ResponseEntity<Page<DreamResponse>> getPublicDreams(
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getAllPublicDreams(page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 카테고리로 조회 (페이지네이션)
  @GetMapping("/api/dream/category/{category}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByCategory(
      @PathVariable("category") String category,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByCategory(category, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 태그로 조회 (페이지네이션)
  @GetMapping("/api/dream/tag/{tag}")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTag(
      @PathVariable("tag") String tag,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTag(tag, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 제목으로 조회 (페이지네이션)
  @GetMapping("/api/dream/title")
  public ResponseEntity<Page<DreamResponse>> getDreamsByTitle(
      @RequestParam("title") String title,
      @RequestParam(value = "page", defaultValue = "0") int page) {
    Page<DreamResponse> dreamPage = dreamService.getDreamsByTitle(title, page);
    return ResponseEntity.ok(dreamPage); // 200 OK + JSON 반환
  }

  // 단일 조회
  @GetMapping("/api/dream/{id}")
  public ResponseEntity<DreamResponse> getDream(@PathVariable("id") Long dreamId) {
    DreamResponse dream = dreamService.getDreamById(dreamId);
    return ResponseEntity.ok(dream); // 200 OK + JSON 반환
  }

  // 등록
  @PostMapping("/api/dream")
  @ResponseBody
  public ResponseEntity<Dream> saveDream(
      Authentication authentication,
      @RequestBody DreamRequest request) {
    Long userId = SecurityUtil.getUserId(authentication);
    Dream savedDream = dreamService.saveDream(userId, request);
    return ResponseEntity.status(201).body(savedDream);// 201 Created, 저장된 객체 반환
  }

  // 수정
  @PutMapping("/api/dream/{id}")
  @ResponseBody
  public DreamResponse updateDream(
      Authentication authentication,
      @PathVariable("id") Long dreamId,
      @RequestBody DreamRequest request) {
    Long userId = SecurityUtil.getUserId(authentication);
    return dreamService.updateDream(userId, dreamId, request); // 200 OK + 업데이트된 객체 반환
  }

  // 삭제
  @DeleteMapping("/api/dream/{id}")
  @ResponseBody
  public ResponseEntity<Void> deleteDream(
      Authentication authentication,
      @PathVariable("id") Long dreamId) {
    Long userId = SecurityUtil.getUserId(authentication);
    dreamService.deleteDream(userId, dreamId);
    return ResponseEntity.noContent().build(); // 204 No Content
  }

  // 내가 쓴 꿈 기본(최신) 목록 - 기본 4개, limit 지정 가능
  @GetMapping("/api/dream/my")
  public ResponseEntity<List<DreamResponse>> getMyDreams(
      Authentication authentication,
      @RequestParam(value = "limit", required = false) Integer limit) {
    Long userId = SecurityUtil.getUserId(authentication);
    List<DreamResponse> dreams = dreamService.getMyDreams(userId, limit);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

  // 내가 쓴 꿈 전체 조회
  @GetMapping("/api/dream/my/all")
  public ResponseEntity<List<DreamResponse>> getMyAllDreams(Authentication authentication) {
    Long userId = SecurityUtil.getUserId(authentication);
    List<DreamResponse> dreams = dreamService.getMyAllDreams(userId);
    return ResponseEntity.ok(dreams); // 200 OK + JSON 반환
  }

}
