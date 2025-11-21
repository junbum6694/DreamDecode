package est.DreamDecode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SceneAnalysis {
    @JsonProperty("scene")
    private String scene;

    @JsonProperty("emotion")
    private String emotion;

    @JsonProperty("interpretation")
    private String interpretation;
}

