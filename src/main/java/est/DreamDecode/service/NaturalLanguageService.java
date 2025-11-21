package est.DreamDecode.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.LanguageServiceSettings;
import com.google.cloud.language.v1.Sentiment;
import est.DreamDecode.dto.SentimentResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class NaturalLanguageService {

  // application.yml에서 설정한 키 파일 경로 주입
  @Value("${gcp.credentials-path}")
  private String credentialsPath;

  private LanguageServiceClient languageServiceClient;

  /**
   * Spring Bean 초기화 시 LanguageServiceClient를 생성합니다.
   * LanguageServiceClient는 리소스를 사용하므로, 한 번만 생성하여 재사용하는 것이 좋습니다.
   */


  @PostConstruct
  public void init() throws IOException {
    // 1. 서비스 계정 JSON 파일로 인증 정보(Credentials) 로드
    GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));

    // 2. LanguageServiceClient 설정 및 생성
    LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
                                               .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                                               .build();

    languageServiceClient = LanguageServiceClient.create(settings);
  }

  //Spring Bean 소멸 시 LanguageServiceClient의 리소스를 해제합니다.

  @PreDestroy
  public void close() {
    if (languageServiceClient != null) {
      languageServiceClient.close();
    }
  }

  /**
   * 텍스트에 대한 감정 분석을 수행합니다.
   * @param text 분석할 텍스트
   * @return 감정 분석 결과 JSON 문자열
   */
  public SentimentResult analyzeSentiment(String text) {
    // 분석할 Document 객체 생성
    Document doc = Document.newBuilder()
                           .setContent(text)
                           .setType(Document.Type.PLAIN_TEXT)
                           .setLanguage("ko") // 언어 설정 (한국어: ko, 영어: en 등)
                           .build();

    // API 호출
    Sentiment sentiment = languageServiceClient.analyzeSentiment(doc).getDocumentSentiment();

    // 결과 반환
    return new SentimentResult(sentiment.getScore(),sentiment.getMagnitude());
  }
}
