package net.thisptr;


import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;

import org.junit.Rule;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.File;

public class JacksonJqMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    /**
     * @throws Exception if any
     */
    @Test
    public void testJqMojo() throws Exception {
       File inputFile = new File("src/test/resources/example-project");
       String filter = ".host |= \"https://my_custom_url.com/petstore\"";

       JacksonJqMojo jacksonJqMojo = new JacksonJqMojo();
       jacksonJqMojo.setInputFile(inputFile);
       jacksonJqMojo.setFilter(filter);
       jacksonJqMojo.execute();
    }


}

