package io.github.phiglang.phig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConformanceTest {

    private static final Path TESTDATA_DIR = Paths.get("../../testdata");
    private static final ObjectMapper JSON = new ObjectMapper();

    @TestFactory
    Stream<DynamicTest> conformance() throws IOException {
        if (!Files.isDirectory(TESTDATA_DIR)) {
            return Stream.empty();
        }

        List<DynamicTest> tests = new ArrayList<>();
        try (var entries = Files.list(TESTDATA_DIR)) {
            entries.filter(p -> p.toString().endsWith(".phig"))
                    .sorted()
                    .forEach(phigPath -> {
                        String fileName = phigPath.getFileName().toString();
                        String testName = fileName.substring(0, fileName.length() - 5);

                        tests.add(DynamicTest.dynamicTest(testName, () -> {
                            String phigData = Files.readString(phigPath);

                            if (testName.endsWith("_FAIL")) {
                                assertThrows(PhigException.class, () -> {
                                    PhigReader.parse(phigData);
                                }, "expected parse error for " + testName);
                                return;
                            }

                            Path jsonPath = TESTDATA_DIR.resolve(testName + ".json");
                            assertTrue(Files.exists(jsonPath),
                                    "missing expected JSON file: " + jsonPath);

                            PhigNode tree = PhigReader.parse(phigData);
                            Object actual = tree.toObject();

                            String jsonData = Files.readString(jsonPath);
                            Object expected = JSON.readValue(jsonData, Object.class);

                            assertEquals(expected, actual, testName);
                        }));
                    });
        }
        return tests.stream();
    }
}
