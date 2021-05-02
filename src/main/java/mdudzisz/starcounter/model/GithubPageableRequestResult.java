package mdudzisz.starcounter.model;

import lombok.Data;
import org.springframework.hateoas.Link;

import java.util.List;

/**
 * Class representing one page of Github repository with stars listing.
 * Holds list of {@link mdudzisz.starcounter.model.GithubRepoModel} and page navigating urls within Github
 * which need to be changed into app urls in order to be used by app user.
 */
@Data
public class GithubPageableRequestResult {
    /**
     * Repository information in a list.
     */
    private List<GithubRepoModel> reposInfosOnPage;
    /**
     * Urls navigating over next possible Github queries.
     */
    private List<Link> pageLinks;

    public GithubPageableRequestResult() {
    }

    public GithubPageableRequestResult(List<GithubRepoModel> reposInfosOnPage, List<Link> pageLinks) {
        this.reposInfosOnPage = reposInfosOnPage;
        this.pageLinks = pageLinks;
    }
}
