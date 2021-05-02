package mdudzisz.starcounter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mdudzisz.starcounter.service.GithubConnector;
import mdudzisz.starcounter.model.GithubPageableRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Request controller of application. Exposes two GET endpoints:
 * <base url>/list/{username}?{params} and <base url>/count/{username}.
 */
@Controller
@RequestMapping("")
public class RequestController {

    final static String listMapping = "/list";
    final static String countMapping = "/count";

    @Autowired
    private GithubConnector webConnector;

    /**
     * Lists user repositories as name - star count pairs.
     * @param username Taken from request path Github user name.
     * @param queryMap Map of allowed query parameters with their values as String.
     * @return One page of user repositories data (name and star count) and navigating url in response's
     * HTTP headers for next pages.
     */
    @GetMapping(value = listMapping + "/{username}", produces = {"application/JSON"})
    public ResponseEntity<String> listUserRepos(
            @PathVariable("username") String username,
            @RequestParam Map<String, String> queryMap) {

        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        try {
            validateQuery(queryMap);

            GithubPageableRequestResult result = webConnector.getReposNamesAndStars(username, queryMap);

            String jsonBody = new ObjectMapper().writeValueAsString(result.getReposInfosOnPage());

            List<Link> nextPagesLinks = result.getPageLinks();
            HttpHeaders headers = parseHeadersFromLinks(nextPagesLinks, baseUrl + listMapping + "/" + username);

            return new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Counts sum of user's stars in all repositories.
     * @param username Taken from request path Github user name.
     * @return User name and user stars count as JSON object.
     */
    @GetMapping(value = countMapping + "/{username}", produces = {"application/JSON"})
    @ResponseBody
    public ResponseEntity<String> countUserStars(@PathVariable("username") String username) {

        try {
            int starCount = webConnector.getUserStarCount(username);

            String responseBody = prepareCountResponseBody(username, starCount);

            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String prepareCountResponseBody(String username, int starCount) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyObject = mapper.createObjectNode();
        bodyObject.put("username", username);
        bodyObject.put("star_count", starCount);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bodyObject);
    }

    private HttpHeaders parseHeadersFromLinks(List<Link> linksList, String mappingUrl) throws RuntimeException {
        HttpHeaders headers = new HttpHeaders();
        linksList = changeBaseUrlFromGithubToLocal(linksList, mappingUrl);

        List<String> strings = linksList.stream().map(Link::toString).collect(Collectors.toList());
        headers.put("link", strings);
        return headers;
    }

    private List<Link> changeBaseUrlFromGithubToLocal(List<Link> linksList, String mappingUrl) throws RuntimeException {

        return linksList.stream().map(link -> {
            try {
                return link.withHref(mappingUrl + "?" + new URL(link.getHref()).getQuery());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private void validateQuery(Map<String, String> queryMap) throws HttpClientErrorException {
        Optional<Map.Entry<String, String>> wrongOptional = queryMap.entrySet().stream()
                .filter(queryParam -> !queryParam.getKey().equals("per_page") && !queryParam.getKey().equals("page"))
                .findAny();

        if (wrongOptional.isPresent())
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Unsupported query params.");
    }
}
