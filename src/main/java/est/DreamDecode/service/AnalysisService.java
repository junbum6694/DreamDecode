package est.DreamDecode.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import est.DreamDecode.domain.Scene;
import est.DreamDecode.dto.AlanApiResponse;
import est.DreamDecode.dto.AlanResetRequest;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.dto.DreamAnalysisResponse;
import est.DreamDecode.dto.SceneAnalysis;
import est.DreamDecode.dto.SentimentResult;
import est.DreamDecode.exception.DreamAnalysisException;
import est.DreamDecode.exception.DreamNotFoundException;
import est.DreamDecode.repository.AnalysisRepository;
import est.DreamDecode.repository.DreamRepository;
import est.DreamDecode.repository.SceneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AnalysisRepository analysisRepository;
    private final DreamRepository dreamRepository;
    private final SceneRepository sceneRepository;
    private final NaturalLanguageService naturalLanguageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisResponse addOrUpdateAnalysis(Long dreamId, boolean isPost){
        Dream dream = dreamRepository.findById(dreamId)
                .orElseThrow(() -> new DreamNotFoundException(dreamId));

        if(!isPost) {
            sceneRepository.deleteByDreamId(dreamId); // 기존 장면 삭제 후 추가
        }

        String dreamContent = dream.getContent();
        DreamAnalysisResponse dreamAnalysis = dreamAnalyzeByPython(dreamContent);
        if(dreamAnalysis == null || dreamAnalysis.getAnalysis() == null){
            throw new DreamAnalysisException("Failed to analyze dream content from external service for dream id " + dreamId);
        }

        // Stream API를 사용하여 Scene 리스트 생성
        List<Scene> scenesToPersist = dreamAnalysis.getAnalysis().stream()
                .map(sceneAnalysis -> {
                    Scene scene = new Scene();
                    scene.setContent(sceneAnalysis.getScene());
                    scene.setEmotion(sceneAnalysis.getEmotion());
                    scene.setInterpretation(sceneAnalysis.getInterpretation());
                    scene.setDream(dream);
                    return scene;
                })
                .collect(Collectors.toList());

        if (!scenesToPersist.isEmpty()) {
            sceneRepository.saveAll(scenesToPersist);
        }

        String insight = dreamAnalysis.getInsight();
        String suggestion = dreamAnalysis.getSuggestion();
        String categories = String.join(",", dreamAnalysis.getCategories());
        String tags = String.join(",", dreamAnalysis.getTags());
        String summary = dreamAnalysis.getSummary();

        // GCP Natural Language API를 사용하여 꿈 내용의 실제 감정 분석 수행
        SentimentResult sentimentResult;
        try {
            sentimentResult = naturalLanguageService.analyzeSentiment(summary);
        } catch (RuntimeException e) {
            throw new DreamAnalysisException("Failed to analyze sentiment for dream id " + dreamId, e);
        }
        double sentiment = sentimentResult.getScore();
        double magnitude = sentimentResult.getMagnitude();

        if(isPost) {
            Analysis analysis = new Analysis();
            analysis.setInsight(insight);
            analysis.setSuggestion(suggestion);
            analysis.setSummary(summary);
            analysis.setSentiment(sentiment);
            analysis.setMagnitude(magnitude);
            analysis.setDream(dream);
            dream.updateCatAndTags(categories, tags);
            return new AnalysisResponse(analysisRepository.save(analysis));
        }
        else {
            Analysis analysis = getAnalysisByDreamId(dreamId);
            analysis.updateAnalysis(
                    insight,
                    suggestion,
                    summary,
                    sentiment,
                    magnitude
            );
            dream.updateCatAndTags(categories, tags);
            return new AnalysisResponse(analysis);
        }

    }

    public Analysis getAnalysisByDreamId(Long dreamId){
        return analysisRepository.findByDreamId(dreamId)
                .orElseThrow(() -> new DreamAnalysisException("No analysis found for dream id " + dreamId));
    }

    // 프롬프트
    public DreamAnalysisResponse dreamAnalyzeByPython(String dreamContent){
        String clientId = "515d3756-783e-484d-a04b-b7121c99fbb7";

        String prompt = """
                당신은 사용자의 꿈을 심리적으로 해석하고, 감정적 의미와 통찰을 제시하는 역할을 맡고 있습니다.
                아래의 지침에 따라 꿈 내용을 분석해 주세요.
                
                꿈에서 등장한 주요 장면을 2~4개로 나누고, 각 장면마다 느껴진 감정과 그 감정이 나타난 이유(심리적 의미)를 분석하세요.
                
                꿈 전체를 관통하는 심리적 흐름이나 무의식적인 메시지를 설명하세요.
                
                꿈의 감정을 긍정적으로 다스리거나 회복하기 위한 현실적인 조언을 제시하세요.
                
                꿈의 내용을 대표할 수 있는 주제를 하나 이상의 카테고리로 분류하세요.
                (예: 불안 / 성장 / 관계 / 도전 / 상실 / 자아 / 자유 / 변화 / 사랑 / 기억 / 두려움 등)
                
                꿈의 주요 키워드를 기반으로 3~5개의 태그를 생성하세요.
                
                마지막으로, 꿈 전반에 걸쳐 느껴지는 정서를 자연스럽게 풀어서 표현한 100자 미만의 문장으로 작성하세요.
                
                반드시 아래 JSON 형식의 구조를 지키면서 응답하세요.
                JSON 키 이름은 analysis, scene, emotion, interpretation, insight, suggestion, categories, tags, summary로 반드시 유지하고, 다른 키는 추가하지 마세요.
                
                {{
                    "analysis": [
                        {{
                            "scene": "장면1 요약",
                            "emotion": "주된 감정1",
                            "interpretation": "이 감정이 나타난 이유나 의미"
                        }},
                        {{
                            "scene": "장면2 요약",
                            "emotion": "주된 감정2",
                            "interpretation": "이 감정이 나타난 이유나 의미"
                        }},
                        ...
                    ],
                    "insight": "꿈 전체의 심리적 해석과 감정 경향 요약",
                    "suggestion": "감정을 다스리거나 회복하기 위한 조언",
                    "categories": ["주요 주제 카테고리들"],
                    "tags": ["연관 태그들"],
                    "summary": "꿈 전반에 걸쳐 느껴지는 정서를 자연스럽게 풀어서 표현한 100자 미만의 문장"
                }}
                
                꿈의 내용은 다음과 같습니다:
                """
                + dreamContent;

        DreamAnalysisResponse result = singleAlanChat(clientId, prompt);
        resetAlanState(clientId);
        return result;
    }

    private DreamAnalysisResponse singleAlanChat(String clientId, String prompt){
        String url = "https://kdt-api-function.azurewebsites.net/api/v1/question";
        RestTemplate restTemplate = new RestTemplate();

        String requestUrl = url + "?client_id=" + clientId + "&content=" + prompt;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        try{
            AlanApiResponse apiResponse = objectMapper.readValue(response.getBody(), AlanApiResponse.class);
            String content = apiResponse.getContent();
            if (content == null) {
                throw new DreamAnalysisException("API response content is null");
            }
            content = content.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();
            return objectMapper.readValue(content, DreamAnalysisResponse.class);
        } catch(JsonProcessingException e){
            throw new DreamAnalysisException("Failed to parse API response", e);
        }
    }

    private void resetAlanState(String clientId){
        String url = "https://kdt-api-function.azurewebsites.net/api/v1/reset-state";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            AlanResetRequest request = new AlanResetRequest(clientId);
            String requestBody = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
        } catch(JsonProcessingException e){
            throw new DreamAnalysisException("Failed to create reset request", e);
        }
    }
}
