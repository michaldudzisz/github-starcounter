package mdudzisz.starcounter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * Class containing single repository information: username and stars count.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoModel {

    @JsonProperty("name")
    private String name;

    @JsonProperty("stargazers_count")
    private int stars;

    public GithubRepoModel(String name, int stars) {
        this.name = name;
        this.stars = stars;
    }

    public GithubRepoModel() {
    }
}
