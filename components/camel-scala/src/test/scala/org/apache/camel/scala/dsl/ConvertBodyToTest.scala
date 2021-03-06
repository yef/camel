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
package org.apache.camel.scala.dsl

import builder.RouteBuilder
import org.junit.Test

/**
 * Test case for convert body to
 */
class ConvertBodyToTest extends ScalaTestSupport {

  @Test
  def testConvertBody() = {
    val mock = getMockEndpoint("mock:b")
    mock.expectedMessageCount(1);
    mock.message(0).body().isInstanceOf(classOf[java.lang.Integer]);

    // send a string
    template.sendBody("direct:b", "74")

    assertMockEndpointsSatisfied()
  }
  
  val builder =
    //START SNIPPET: e1
    new RouteBuilder {
       "direct:b" ==> {
         convertBodyTo(classOf[java.lang.Integer])
         to("log:b")
         to("mock:b")
       }
    }
    //END SNIPPET: e1

}
