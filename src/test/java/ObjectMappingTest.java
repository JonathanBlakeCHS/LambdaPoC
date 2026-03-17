import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.DataPackage;
import org.example.dto.Downloads;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

public class ObjectMappingTest {
    @Test
    public void testtestjsonReads() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DataPackage val = mapper.readValue(Paths.get("/Users/jblake4/LambdaPoC/src/main/resources/test.json").toFile(), DataPackage.class);
        for (Downloads download : val.downloads()) {
            System.out.println(download);
        }
        System.out.println(val);
    }
}
