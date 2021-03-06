/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.file;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;

/**
 * @version 
 */
public class FromFileMoveFileIfProcessFailsTest extends ContextTestSupport {

    @Override
    protected void setUp() throws Exception {
        deleteDirectory("./target/movefile");
        super.setUp();
    }

    public void testPollFileAndShouldNotBeMoved() throws Exception {
        template.sendBodyAndHeader("file://target/movefile", "Hello World", Exchange.FILE_NAME, "hello.txt");

        MockEndpoint mock = getMockEndpoint("mock:foo");
        mock.expectedBodiesReceived("Hello World");
        mock.expectedFileExists("target/movefile/error/hello.txt", "Hello World");

        mock.assertIsSatisfied();
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("file://target/movefile?moveFailed=error")
                        .convertBodyTo(String.class).to("mock:foo").process(
                            new Processor() {
                                public void process(Exchange exchange) throws Exception {
                                    throw new IllegalArgumentException("Forced by unittest");
                                }
                            });
            }
        };
    }


}