package mdudzisz.starcounter.model;

import lombok.Data;
import org.springframework.hateoas.Link;

import java.util.List;

@Data
public class GithubPageableRequestResult {
    private List<GithubRepoModel> reposInfosOnPage;
    private List<Link> links;

    public GithubPageableRequestResult() {
    }

    public GithubPageableRequestResult(List<GithubRepoModel> reposInfosOnPage, List<Link> links) {
        this.reposInfosOnPage = reposInfosOnPage;
        this.links = links;
    }
}
