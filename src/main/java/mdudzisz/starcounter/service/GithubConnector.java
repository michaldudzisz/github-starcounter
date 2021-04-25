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

@Service
public class GithubConnector {

    private static final URL apiUrl = initializeGithubApiUrl();

    private final RestTemplate template;

    public GithubConnector() {
        template = new RestTemplate();
    }

    public GithubPageableRequestResult getReposNamesAndStars(String username, Map<String, String> queryMap)
            throws HttpClientErrorException, JsonProcessingException {

        List<GithubRepoModel> reposInfos = new LinkedList<>();

        ResponseEntity<String> response;

        String url = parseUrl(username, queryMap);

        response = fetchUserReposDataWithUrl(url);
        reposInfos.addAll(parseGithubRepoModels(response));
        List<Link> links = getHeaderLinks(response);

        return new GithubPageableRequestResult(reposInfos, links);
    }

    public int getUserStarCount(String username) throws HttpClientErrorException, JsonProcessingException {

        int starCount = 0;

        ResponseEntity<String> response;

        response = fetchUserReposDataWithUsername(username);
        starCount += countStarsOnPage(response);
        List<Link> links = getHeaderLinks(response);

        while (linksContainNext(links)) {
            String nextPageUrl = getNextPageUrl(links);
            response = fetchUserReposDataWithUrl(nextPageUrl);
            starCount += countStarsOnPage(response);
            links = getHeaderLinks(response);
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

    private String getNextPageUrl(List<Link> links) {

        Optional<Link> nextPageLinkOptional = links.stream()
                .filter(link -> link.getRel().value().equals("next")).findAny();

        if (nextPageLinkOptional.isEmpty())
            return "";
        else
            return nextPageLinkOptional.get().getHref();
    }

    private List<Link> getHeaderLinks(ResponseEntity<String> response) {
        List<String> plainStringLinks = response.getHeaders().get(HttpHeaders.LINK);
        List<Link> links = new LinkedList<>();

        if (plainStringLinks != null) {
            plainStringLinks.forEach(s -> {
                String[] singleLinks = s.split(",");
                for (String singleLink : singleLinks) {
                    links.add(Link.valueOf(singleLink));
                }
            });
        }

        return links;
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
