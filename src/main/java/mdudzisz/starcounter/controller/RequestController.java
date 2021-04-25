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


@Controller
@RequestMapping("")
public class RequestController {

    final static String listMapping = "/list";
    final static String countMapping = "/count";

    @Autowired
    private GithubConnector webConnector;

    @GetMapping(value = listMapping + "/{username}", produces = {"application/JSON"})
    public ResponseEntity<String> listUserRepos(
            @PathVariable("username") String username,
            @RequestParam Map<String, String> queryMap) {

        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        try {
            validateQuery(queryMap);
            GithubPageableRequestResult result = webConnector.getReposNamesAndStars(username, queryMap);
            List<Link> links = result.getLinks();
            String jsonBody = new ObjectMapper().writeValueAsString(result.getReposInfosOnPage());
            HttpHeaders headers = parseHeadersFromLinks(links, baseUrl + listMapping + "/" + username);

            return new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping(value = countMapping + "/{username}", produces = {"application/JSON"})
    @ResponseBody
    public String countUserStars(@PathVariable("username") String username) {

        try {
            int starCount = webConnector.getUserStarCount(username);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode response = mapper.createObjectNode();
            response.put("username", username);
            response.put("star_count", starCount);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private HttpHeaders parseHeadersFromLinks(List<Link> linksList, String mappingUrl) throws RuntimeException {

        HttpHeaders headers = new HttpHeaders();
        linksList = changeBaseUrl(linksList, mappingUrl);

        List<String> strings = linksList.stream().map(Link::toString).collect(Collectors.toList());
        headers.put("link", strings);
        return headers;
    }

    private List<Link> changeBaseUrl(List<Link> linksList, String mappingUrl) throws RuntimeException {

        return linksList.stream().map(el -> {
            try {
                return el.withHref(mappingUrl + "?" + new URL(el.getHref()).getQuery());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private void validateQuery(Map<String, String> queryMap) throws HttpClientErrorException {
        Optional<Map.Entry<String, String>> wrongOptional = queryMap.entrySet().stream()
                .filter(el -> !el.getKey().equals("per_page") && !el.getKey().equals("page"))
                .findAny();

        if (wrongOptional.isPresent())
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "Unsupported query params.");
    }
}
