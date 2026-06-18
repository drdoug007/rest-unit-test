package one.dastec.restunittest.controllers;

import one.dastec.restunittest.services.RestTestService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RestTestApi {

    private final RestTestService restTestService;

    public RestTestApi(RestTestService restTestService) {
        this.restTestService = restTestService;
    }

    @GetMapping("/tests")
    public List<String> listTests() throws IOException {
        Path path = Paths.get("src/main/resources/httptestfiles");
        if (!Files.exists(path)) {
            // Fallback for JAR execution or if src/main/resources is not available as a filesystem path
             return List.of("cardealer");
        }
        try (var stream = Files.list(path)) {
            return stream
                    .filter(p -> p.toString().endsWith(".http"))
                    .map(p -> p.getFileName().toString().replace(".http", ""))
                    .toList();
        }
    }

    @GetMapping(path = "runtest/{testName}", produces = "text/markdown; charset=UTF-8")
    public String runTest(@PathVariable("testName") String testName) {
        return restTestService.runTest(testName);
    }

    @GetMapping(path = "test/{testName}", produces = "text/plain; charset=UTF-8")
    public String getTestSource(@PathVariable("testName") String testName) {
        return restTestService.getTestSource(testName);
    }

    @PostMapping(path = "runtest/custom", produces = "text/markdown; charset=UTF-8")
    public String runTestCustom(@RequestBody String content) {
        return restTestService.runTestWithContent("Custom Test", content);
    }

    @GetMapping(path = "/fetch-external", produces = "text/plain; charset=UTF-8")
    public String fetchExternal(@RequestParam("url") String url) {
        return restTestService.fetchExternalUrl(url);
    }
}
