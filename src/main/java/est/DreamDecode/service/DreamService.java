package est.DreamDecode.service;

import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.dto.DreamRequest;
import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.exception.DreamNotFoundException;
import est.DreamDecode.repository.DreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DreamService {
  private final DreamRepository dreamRepository;
  private final NaturalLanguageService nlpService;
  private final AnalysisService analysisService;
  private static final int PAGE_SIZE = 9;

  public List<DreamResponse> getAllPublicDreams() {
    List<DreamResponse> dreams = dreamRepository.findAllByPublishedTrue().stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public Page<DreamResponse> getAllPublicDreams(int page) {
    Pageable pageable = PageRequest.of(page, PAGE_SIZE);
    Page<Dream> dreamPage = dreamRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
    return dreamPage.map(DreamResponse::from);
  }

  public List<DreamResponse> getDreamsByCategory(String category) {
    List<DreamResponse> dreams = dreamRepository.findByCategoriesContaining(category).stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public Page<DreamResponse> getDreamsByCategory(String category, int page) {
    Pageable pageable = PageRequest.of(page, PAGE_SIZE);
    Page<Dream> dreamPage = dreamRepository.findByCategoriesContaining(category, pageable);
    return dreamPage.map(DreamResponse::from);
  }

  public List<DreamResponse> getDreamsByTag(String tag) {
    List<DreamResponse> dreams = dreamRepository.findByTagsContaining(tag).stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public Page<DreamResponse> getDreamsByTag(String tag, int page) {
    Pageable pageable = PageRequest.of(page, PAGE_SIZE);
    Page<Dream> dreamPage = dreamRepository.findByTagsContaining(tag, pageable);
    return dreamPage.map(DreamResponse::from);
  }

  public List<DreamResponse> getDreamsByTitle(String title) {
    List<DreamResponse> dreams = dreamRepository.findByTitleContaining(title).stream()
                                         .map(DreamResponse::from)
                                         .toList();
    return dreams;
  }

  public Page<DreamResponse> getDreamsByTitle(String title, int page) {
    Pageable pageable = PageRequest.of(page, PAGE_SIZE);
    Page<Dream> dreamPage = dreamRepository.findByTitleContaining(title, pageable);
    return dreamPage.map(DreamResponse::from);
  }

  public DreamResponse getDreamById(Long id) {
    Dream dream = dreamRepository.findById(id)
                         .orElseThrow(() -> new DreamNotFoundException(id));
    return DreamResponse.from(dream);
  }

  public Dream saveDream(Long userId, DreamRequest request) {
    request.setUserId(userId);
    Dream dream = dreamRepository.save(request.toEntity());

    Long dreamId = dream.getId();
    analysisService.addOrUpdateAnalysis(dreamId, true); // Alan api로 꿈 내용 전송 및 NLP 감정 분석 수행

    return dream;
  }

  @Transactional
  public DreamResponse updateDream(Long userId, Long dreamId, DreamRequest request) {
    Dream dream = dreamRepository.findById(dreamId)
                          .orElseThrow(() -> new DreamNotFoundException(dreamId));

    boolean contentUpdated = request.getContent() != null && !dream.getContent().equals(request.getContent());

    // DTO에서 가져온 값으로 엔티티 업데이트
    if (request.getTitle() != null) {
      dream.setTitle(request.getTitle());
    }
    if (request.getContent() != null) {
      dream.setContent(request.getContent());
    }
    dream.setUserId(userId);
    dream.setPublished(request.isPublished());

    if(contentUpdated) {
        analysisService.addOrUpdateAnalysis(dreamId, false);
    }

    return DreamResponse.from(dream);
  }

  public void deleteDream(Long userId, Long dreamId) {
    dreamRepository.deleteById(dreamId);
  }

  //  현재 로그인한 사용자의 꿈 목록을 가져옵니다 (최신순, 전체 목록)
  public List<DreamResponse> getMyAllDreams(Long userId) {
    List<Dream> dreams = dreamRepository.findByUserIdOrderByCreatedAtDesc(userId);
    return dreams.stream()
                 .map(DreamResponse::from)
                 .toList();
  }

  // 현재 로그인한 사용자의 꿈 목록을 가져옵니다 (최신순, limit 만큼)
  public List<DreamResponse> getMyDreams(Long userId, Integer limit) {
    int resolvedLimit = (limit == null || limit <= 0) ? 4 : limit;
    Pageable pageable = PageRequest.of(0, resolvedLimit);
    Page<Dream> dreamPage = dreamRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return dreamPage.getContent()
                    .stream()
                    .map(DreamResponse::from)
                    .toList();
  }
}
