package mdudzisz.starcounter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoModel {

    @JsonProperty("name")
    private String name;

    @JsonProperty("stargazers_count")
    private int stars;
}
