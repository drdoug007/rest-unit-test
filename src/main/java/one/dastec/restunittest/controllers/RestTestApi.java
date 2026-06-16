package one.dastec.restunittest.controllers;

import one.dastec.restunittest.services.RestTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RestTestApi {

    private final RestTestService restTestService;

    public RestTestApi(RestTestService restTestService) {
        this.restTestService = restTestService;
    }

    @GetMapping(path = "runtest/{testName}", produces = "text/markdown; charset=UTF-8")
    public String runTest(@PathVariable("testName") String testName) {
        var testPath = "httptestfiles/" + testName + ".http";
        return restTestService.runTest(testPath);
    }
}
