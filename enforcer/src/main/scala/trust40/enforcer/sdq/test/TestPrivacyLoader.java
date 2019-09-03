package trust40.enforcer.sdq.test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import trust40.enforcer.sdq.PrivacyLevel;
import trust40.enforcer.sdq.io.PrivacyLoader;

class TestPrivacyLoader {
	static Path t;

	@BeforeAll
	static void init() {
		t = Paths.get("test.csv");
		try (BufferedWriter writer = Files.newBufferedWriter(t, Charset.forName("UTF-8"))) {
			writer.write("Data00;public\n");
			writer.write("Data01;internal_use\n");
			writer.write("Data02;sensitive\n");
			writer.write("Data03;highly_sensitive\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO-Error");
		}
	}
	@Test
	void test() {
		try {
			Map<String, PrivacyLevel> map = new PrivacyLoader(t.toString()).getPrivacyMap();
			assertAll("test Privacylevel",
					()-> assertEquals(PrivacyLevel.PUBLIC, map.get("Data00")),
					()-> assertEquals(PrivacyLevel.INTERNAL_USE, map.get("Data01")),
					()-> assertEquals(PrivacyLevel.SENSITIVE, map.get("Data02")),
					()-> assertEquals(PrivacyLevel.HIGHLY_SENSITIVE, map.get("Data03"))
			);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO-Error");
			
		}
		
	}
	@AfterAll
	static void clean() {
		try {
			Files.delete(t);
		} catch (IOException e) {
			e.printStackTrace();
			fail("IO-Error: can't delete privacy File");
		}
	}

}
