package com.klamann.oauth2client;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Controller
@RequestMapping("api/v1")
public class Controller {

    private static String CLIENTID = "oauth-client-1";

    private static String CLIENTSECRET = "oauth-client-secret-1";

    private static String REDIRECTURI = "http://localhost:8082/api/v1/callback";

    private static String HOME = "http://localhost:8082/api/v1/";

    private static String RESPONSETYPE = "code";

    private static String AUTHSERVERAUTHORIZEURL = "http://localhost:8081/api/v1/authorize";

    private static String AUTHSERVERTOKENENDPOINT = "http://localhost:8081/api/v1/token";

    private static String RESOURCEPATHAPI = "http://localhost:8080/api/v1/students";

    private Details authDetails;

    @Autowired
    public Controller(Details authDetails) {
        this.authDetails = authDetails;
    }

    @GetMapping
    public String home(Model model, HttpServletRequest request) throws UnsupportedEncodingException {

        Optional<String> accessTokenValue = Optional.ofNullable(authDetails.getAccessToken());
        Optional<String> scopeValue = Optional.ofNullable(authDetails.getScope());
        Optional<String> refreshToken = Optional.ofNullable(authDetails.getRefreshToken());


        String state = RandomStringUtils.random(8, true, false);

        authDetails.setState(state);

        Map<String, String> requestParams = Map.of("clientId", CLIENTID, "redirectUri", encodeValue(REDIRECTURI), "state", state, "responseType", RESPONSETYPE);

        model.addAttribute("clientId", CLIENTID);
        model.addAttribute("accessTokenValue", accessTokenValue.orElse("null"));
        model.addAttribute("scopeValue", scopeValue.orElse("null"));
        model.addAttribute("api", "v1/authorize");
        model.addAttribute("redirectUri", requestParams.get("redirectUri"));
        model.addAttribute("state", requestParams.get("state"));
        model.addAttribute("responseType", requestParams.get("responseType"));
        model.addAttribute("resourcePath", RESOURCEPATHAPI);
        model.addAttribute("refreshTokenValue", refreshToken.orElse("null"));
        model.addAttribute("refreshApi", "v1/refresh");

        return "home";
    }

    @GetMapping(path = "/authorize")
    public ModelAndView authorize(@RequestParam String clientId,
                                  @RequestParam String scope,
                                  @RequestParam String redirectUri,
                                  @RequestParam String state,
                                  @RequestParam String responseType,
                                  RedirectAttributes redirectAttributes) {

        scope = scope.replace(",", "");

        authDetails.setScope(scope);

        redirectAttributes.addAttribute("clientId", clientId);
        redirectAttributes.addAttribute("scope", scope);
        redirectAttributes.addAttribute("state", state);
        redirectAttributes.addAttribute("redirectUri", redirectUri);
        redirectAttributes.addAttribute("responseType", responseType);
        return new ModelAndView("redirect:" + AUTHSERVERAUTHORIZEURL);

    }

    @GetMapping(path = "/callback")
    public ModelAndView callback(@RequestParam String state, @RequestParam String code) {
        if (!state.equals(authDetails.getState())) {
            throw new IllegalStateException("States stimmen nicht.");
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(CLIENTID, CLIENTSECRET);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("code",code);
        map.add("redirect_uri", REDIRECTURI);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<TokenResponse> response =
                restTemplate.exchange(AUTHSERVERTOKENENDPOINT,
                        HttpMethod.POST,
                        entity,
                        TokenResponse.class);

        System.out.println(response.getBody());

        TokenResponse tokenResponse = response.getBody();

        authDetails.setAccessToken(tokenResponse.getAccess_token());
        authDetails.setRefreshToken(tokenResponse.getRefresh_token());

        return new ModelAndView("redirect:" + HOME);
    }

    @GetMapping(path="/refresh")
    public ModelAndView refreshToken(@RequestParam String refreshToken){
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(CLIENTID, CLIENTSECRET);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
        map.add("redirect_uri", REDIRECTURI);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<TokenResponse> response =
                restTemplate.exchange(AUTHSERVERTOKENENDPOINT,
                        HttpMethod.POST,
                        entity,
                        TokenResponse.class);

        System.out.println(response.getBody());

        TokenResponse tokenResponse = response.getBody();

        authDetails.setAccessToken(tokenResponse.getAccess_token());
        authDetails.setRefreshToken(tokenResponse.getRefresh_token());

        return new ModelAndView("redirect:" + HOME);
    }

    private String encodeValue(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
}
