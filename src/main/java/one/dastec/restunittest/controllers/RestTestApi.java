package one.dastec.restunittest.controllers;

import one.dastec.restunittest.models.CustomTestRequest;
import one.dastec.restunittest.models.SingleRequest;
import one.dastec.restunittest.services.CryptoService;
import one.dastec.restunittest.services.RestTestService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RestTestApi {

    private final RestTestService restTestService;
    private final CryptoService cryptoService;

    public RestTestApi(RestTestService restTestService, CryptoService cryptoService) {
        this.restTestService = restTestService;
        this.cryptoService = cryptoService;
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

    @PostMapping(path = "runtest/{testName}", produces = "text/markdown; charset=UTF-8")
    public String runTestPost(@PathVariable("testName") String testName, 
                              @RequestBody(required = false) Map<String, Object> globals,
                              @RequestParam(value = "debug", defaultValue = "false") boolean debug) {
        if (globals == null) return restTestService.runTestWithGlobals(testName, null, debug);
        return restTestService.runTestWithGlobals(testName, globals, debug);
    }

    @PostMapping(path = "runtest/single", produces = "text/markdown; charset=UTF-8")
    public String runSingleRequest(@RequestBody SingleRequest request,
                                   @RequestParam(value = "debug", defaultValue = "false") boolean debug) {
        return restTestService.runSingleRequest(request, debug);
    }

    public String runSingleRequest(SingleRequest request) {
        return runSingleRequest(request, false);
    }

    @GetMapping(path = "test/{testName}", produces = "text/plain; charset=UTF-8")
    public String getTestSource(@PathVariable("testName") String testName) {
        return restTestService.getTestSource(testName);
    }

    @PostMapping(path = "runtest/custom", produces = "text/markdown; charset=UTF-8")
    public String runTestCustom(@RequestBody CustomTestRequest request,
                                @RequestParam(value = "debug", defaultValue = "false") boolean debug) {
        return restTestService.runTestWithContent(request.getName(), request.getContent(), request.getGlobals(), debug);
    }

    public String runTestCustom(CustomTestRequest request) {
        return runTestCustom(request, false);
    }

    @GetMapping(path = "/fetch-external", produces = "text/plain; charset=UTF-8")
    public String fetchExternal(@RequestParam("url") String url) {
        return restTestService.fetchExternalUrl(url);
    }

    @PostMapping(path = "/crypto/encrypt", consumes = "text/plain", produces = "text/plain")
    public String encrypt(@RequestBody String value) {
        return "{enc}" + cryptoService.encrypt(value);
    }
}
