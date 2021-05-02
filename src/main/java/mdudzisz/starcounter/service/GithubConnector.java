package mdudzisz.starcounter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mdudzisz.starcounter.model.GithubPageableRequestResult;
import mdudzisz.starcounter.model.GithubRepoModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class which purpose is to retrieve specified by user data from Github.
 */
@Service
public class GithubConnector {

    private static final URL apiUrl = initializeGithubApiUrl();

    private final RestTemplate template;

    public GithubConnector() {
        template = new RestTemplate();
    }

    /**
     * Function fetching one specified page of data from Github.
     * @param username Github user name whose repositories should be listed.
     * @param queryMap Map of allowed query parameters with their values as String.
     * @return Page of pairs -  name of repository and its star count, and additionally navigable github urls
     * for this user. See {@link mdudzisz.starcounter.model.GithubPageableRequestResult}
     * @throws HttpClientErrorException When unable to retrieve data from Github.
     * @throws JsonProcessingException When there is an internal error parsing Github response.
     */
    public GithubPageableRequestResult getReposNamesAndStars(String username, Map<String, String> queryMap)
            throws HttpClientErrorException, JsonProcessingException {

        ResponseEntity<String> response;

        String url = parseUrl(username, queryMap);

        response = fetchUserReposDataWithUrl(url);
        List<GithubRepoModel> reposInfos = new LinkedList<>(parseGithubRepoModels(response));
        List<Link> pagesLinks = getHeaderLinks(response);

        return new GithubPageableRequestResult(reposInfos, pagesLinks);
    }

    /**
     *
     * @param username Github user name whose stars should be counted.
     * @return Number of user's stars.
     * @throws HttpClientErrorException When unable to retrieve data from Github.
     * @throws JsonProcessingException When there is an internal error parsing Github response.
     */
    public int getUserStarCount(String username) throws HttpClientErrorException, JsonProcessingException {

        int starCount = 0;

        ResponseEntity<String> response;

        response = fetchUserReposDataWithUsername(username);
        starCount += countStarsOnPage(response);
        List<Link> pageLinks = getHeaderLinks(response);

        while (linksContainNext(pageLinks)) {
            Optional<String> nextPageUrlOptional = getNextPageUrl(pageLinks);
            response = fetchUserReposDataWithUrl(nextPageUrlOptional.orElseThrow());
            starCount += countStarsOnPage(response);
            pageLinks = getHeaderLinks(response);
        }

        return starCount;
    }

    private String parseUrl(String username, Map<String, String> queryMap) {
        final String urlPrefix = "users/";
        final String urlSuffix = "/repos";

        String queryString = parseQueryString(queryMap);

        return apiUrl + urlPrefix + username + urlSuffix + "?" + queryString;
    }

    private String parseQueryString(Map<String, String> queryMap) {
        List<String> keyValuePairs = queryMap.entrySet().stream().map(el -> el.getKey() + "=" + el.getValue())
                .collect(Collectors.toList());

        Optional<String> queryStringOptional = keyValuePairs.stream().reduce((tmp, next) -> tmp + "&" + next);

        return queryStringOptional.orElse("");
    }

    private ResponseEntity<String> fetchUserReposDataWithUsername(String username)
            throws RestClientException {
        final int per_page = 100;
        final String urlPrefix = "users/";
        final String urlSuffix = "/repos?per_page=" + per_page;

        return template.getForEntity(apiUrl + urlPrefix + username + urlSuffix, String.class);
    }

    private ResponseEntity<String> fetchUserReposDataWithUrl(String url) throws RestClientException {
        return template.getForEntity(url, String.class);
    }

    private boolean linksContainNext(List<Link> links) {
        return links.stream().anyMatch(link -> link.getRel().value().equals("next"));
    }

    private Optional<String> getNextPageUrl(List<Link> pageLinks) {

        Optional<Link> nextPageLinkOptional = pageLinks.stream()
                .filter(link -> link.getRel().value().equals("next")).findAny();

        if (nextPageLinkOptional.isEmpty())
            return Optional.empty();
        else
            return Optional.of(nextPageLinkOptional.get().getHref());
    }

    private List<Link> getHeaderLinks(ResponseEntity<String> response) {
        List<String> plainStringLinks = response.getHeaders().get(HttpHeaders.LINK);
        List<Link> links = new LinkedList<>();

        if (plainStringLinks != null) {
            plainStringLinks.forEach(s -> {
                String[] singleLinks = getSeparateLinks(s);
                for (String singleLink : singleLinks) {
                    links.add(Link.valueOf(singleLink));
                }
            });
        }

        return links;
    }

    private String[] getSeparateLinks(String headerLinks) {
        return headerLinks.split(",");
    }

    private List<GithubRepoModel> parseGithubRepoModels(ResponseEntity<String> response)
            throws JsonProcessingException {
        return new ObjectMapper().readValue(response.getBody(), new TypeReference<>(){});
    }

    private int countStarsOnPage(HttpEntity<String> response) throws JsonProcessingException {

        int starsOnPage;

        JsonNode rootNode = new ObjectMapper().readTree(response.getBody());

        List<JsonNode> starNodes = rootNode.findValues("stargazers_count");
        starsOnPage = starNodes.stream().mapToInt(JsonNode::intValue).sum();

        return starsOnPage;
    }

    private static URL initializeGithubApiUrl() {
        try {
            return new URL("https://api.github.com/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new AssertionError();
        }
    }
}
