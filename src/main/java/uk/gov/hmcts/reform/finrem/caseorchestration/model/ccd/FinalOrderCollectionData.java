package uk.gov.hmcts.reform.finrem.caseorchestration.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class FinalOrderCollectionData {
    @JsonProperty("id")
    private String id;
    @JsonProperty("value")
    private FinalOrderDocument  finalOrderDocuments;
}
