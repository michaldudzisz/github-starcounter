package mdudzisz.starcounter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mdudzisz.starcounter.model.GithubPageableRequestResult;
import mdudzisz.starcounter.model.GithubRepoModel;
import mdudzisz.starcounter.service.GithubConnector;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@WebMvcTest(RequestController.class)
class RequestControllerTest {

    @Autowired
    private MockMvc client;

    @MockBean
    private GithubConnector githubConnector;

    static List<GithubRepoModel> repositories;

    static {
        // GithubConnector::getReposNamesAndStars base result:
        repositories = Stream.of(
                new GithubRepoModel("repo1", 1),
                new GithubRepoModel("repo2", 2))
                .collect(Collectors.toList());
    }

    @Test
    public void listUserRepos_OnePage() throws Exception {
        String username = "someone";
        Map<String, String> emptyQueryMap = new HashMap<>();

        List<Link> pageLinks = new LinkedList<>(); // empty list for links - no navigating links
        GithubPageableRequestResult serviceResult = new GithubPageableRequestResult(repositories, pageLinks);

        given(githubConnector.getReposNamesAndStars(username, emptyQueryMap)).willReturn(serviceResult);

        // perform tested method
        MockHttpServletResponse controllerResponse = client.perform(get("/list/" + username))
                .andReturn().getResponse();

        assertNull(controllerResponse.getHeader("link"),
                "There should be no links in http link header response for one page result.");

        ObjectMapper jsonMapper = new ObjectMapper();

        String body = controllerResponse.getContentAsString();

        List<GithubRepoModel> responseParsed = jsonMapper.readValue(body, new TypeReference<>() {
        });
        assertEquals(repositories, responseParsed,
                "Returned body list of controller should be same as value returned by service.");
    }

    @Test
    public void listUserRepos_UnsupportedQuery() throws Exception {
        String username = "someone";
        String unsupportedQueryString = "per_page=2&unsupported=2";
        Map<String, String> unsupportedQueryMap = Map.of("per_page", "2", "unsupported", "2");

        List<Link> pageLinks = new LinkedList<>(); // empty list for links - no navigating links
        GithubPageableRequestResult serviceResult = new GithubPageableRequestResult(repositories, pageLinks);

        given(githubConnector.getReposNamesAndStars(username, unsupportedQueryMap)).willReturn(serviceResult);

        // perform tested method
        MockHttpServletResponse controllerResponse = client.perform(
                get("/list/" + username + "?" + unsupportedQueryString)
        ).andReturn().getResponse();

        assertEquals(controllerResponse.getStatus(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    void listUserRepos_MiddleOfPages() throws Exception {
        String username = "someone";
        Map<String, String> queryMap = Map.of("per_page", "2", "page", "3");

        List<Link> pageGithubLinks = List.of(
                Link.of("https://api.github.com/user/49537887/repos?per_page=2&page=2", "prev"),
                Link.of("https://api.github.com/user/49537887/repos?per_page=2&page=4", "next"),
                Link.of("https://api.github.com/user/49537887/repos?per_page=2&page=4", "last"),
                Link.of("https://api.github.com/user/49537887/repos?per_page=2&page=1", "first"));


        GithubPageableRequestResult serviceResult = new GithubPageableRequestResult(repositories, pageGithubLinks);

        given(githubConnector.getReposNamesAndStars(username, queryMap)).willReturn(serviceResult);

        // perform tested method
        MvcResult mvcResult = client.perform(get("/list/" + username + "?per_page=2&page=3")).andReturn();

        MockHttpServletResponse controllerResponse = mvcResult.getResponse();
        String baseUrl = mvcResult.getRequest().getRequestURL().toString();

        Set<Link> pageAppLinks = Set.of(
                Link.of(baseUrl + "?per_page=2&page=2", "prev"),
                Link.of(baseUrl + "?per_page=2&page=4", "next"),
                Link.of(baseUrl + "?per_page=2&page=4", "last"),
                Link.of(baseUrl + "?per_page=2&page=1", "first"));

        assertEquals(pageAppLinks, new HashSet<>(getLinksFromHeader(controllerResponse)),
                "Response header links should match those expected.");

        ObjectMapper jsonMapper = new ObjectMapper();
        String body = controllerResponse.getContentAsString();
        List<GithubRepoModel> responseParsed = jsonMapper.readValue(body, new TypeReference<>() {
        });
        assertEquals(repositories, responseParsed,
                "Returned body list of controller should be same as value returned by service.");
    }

    @Test
    void countUserStars() throws Exception {
        String username = "someone";
        int starCount = 3;

        given(githubConnector.getUserStarCount(username)).willReturn(starCount);

        // perform tested method
        MockHttpServletResponse controllerResponse = client.perform(get("/count/" + username))
                .andReturn().getResponse();

        JsonNode resultBody = new ObjectMapper().readTree(controllerResponse.getContentAsString());

        assertEquals(username, resultBody.findValue("username").asText());
        assertEquals(resultBody.findValue("star_count").asInt(), starCount);
    }

    private List<Link> getLinksFromHeader(MockHttpServletResponse controllerResponse) {
        List<String> plainStringLinks = controllerResponse.getHeaders(HttpHeaders.LINK);
        List<Link> links = new LinkedList<>();

        if (!plainStringLinks.isEmpty()) {
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
}