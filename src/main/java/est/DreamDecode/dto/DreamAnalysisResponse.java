package est.DreamDecode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DreamAnalysisResponse {
    @JsonProperty("analysis")
    private List<SceneAnalysis> analysis;

    @JsonProperty("insight")
    private String insight;

    @JsonProperty("suggestion")
    private String suggestion;

    @JsonProperty("categories")
    private List<String> categories;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("summary")
    private String summary;
}

