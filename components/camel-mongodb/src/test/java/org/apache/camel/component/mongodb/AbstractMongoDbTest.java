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
package org.apache.camel.component.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.Properties;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.mongodb.util.JSON;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.IOHelper;
import org.apache.camel.util.ObjectHelper;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class AbstractMongoDbTest extends CamelTestSupport {

    protected static Mongo mongo;
    protected static DB db;
    protected static DBCollection testCollection;
    protected static DBCollection dynamicCollection;
    
    protected static String dbName;
    protected static String testCollectionName;
    protected static String dynamicCollectionName;

    protected static Properties properties;
    
    protected ApplicationContext applicationContext;
    

    /**
     * Checks whether Mongo is running using the connection URI defined in the mongodb.test.properties file
     * @throws IOException 
     */
    @BeforeClass
    public static void checkMongoRunning() throws IOException {
        properties = new Properties();
        InputStream is = MongoDbConversionsTest.class.getResourceAsStream("/mongodb.test.properties");
        properties.load(is);
        // ping Mongo and populate db and collection
        try {
            mongo = new Mongo(new MongoURI(properties.getProperty("mongodb.connectionURI")));
            mongo.getDatabaseNames();
            dbName = properties.getProperty("mongodb.testDb");
            db = mongo.getDB(dbName);
        } catch (Exception e) {
            Assume.assumeNoException(e);
        }
        
    }

    @Before
    public void initTestCase() {
        // Refresh the test collection - drop it and recreate it. We don't do this for the database because MongoDB would create large
        // store files each time
        testCollectionName = properties.getProperty("mongodb.testCollection");
        testCollection = db.getCollection(testCollectionName);
        testCollection.drop();
        testCollection = db.getCollection(testCollectionName);
        
        dynamicCollectionName = testCollectionName.concat("Dynamic");
        dynamicCollection = db.getCollection(dynamicCollectionName);
        dynamicCollection.drop();
        dynamicCollection = db.getCollection(dynamicCollectionName);

    }

    @After
    public void cleanup() {
        testCollection.drop();
        dynamicCollection.drop();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext("org/apache/camel/component/mongodb/mongoComponentTest.xml");
        CamelContext ctx = SpringCamelContext.springCamelContext(applicationContext);
        PropertiesComponent pc = new PropertiesComponent("classpath:mongodb.test.properties");
        ctx.addComponent("properties", pc);
        return ctx;
    }

    protected void pumpDataIntoTestCollection() {
        // there should be 100 of each
        String[] scientists = {"Einstein", "Darwin", "Copernicus", "Pasteur", "Curie", "Faraday", "Newton", "Bohr", "Galilei", "Maxwell"};
        for (int i = 1; i <= 1000; i++) {
            int index = i % scientists.length;
            Formatter f = new Formatter();
            String doc = f.format("{\"_id\":\"%d\", \"scientist\":\"%s\", \"fixedField\": \"fixedValue\"}", i, scientists[index]).toString();
            IOHelper.close(f);
            testCollection.insert((DBObject) JSON.parse(doc), WriteConcern.SAFE);
        }
        assertEquals("Data pumping of 1000 entries did not complete entirely", 1000L, testCollection.count());
    }

    protected CamelMongoDbException extractAndAssertCamelMongoDbException(Object result, String message) {
        assertTrue("Result is not an Exception", result instanceof Throwable);
        assertTrue("Result is not an CamelExecutionException", result instanceof CamelExecutionException);
        Throwable exc = ((CamelExecutionException) result).getCause();
        assertTrue("Result is not an CamelMongoDbException", exc instanceof CamelMongoDbException);
        CamelMongoDbException camelExc = ObjectHelper.cast(CamelMongoDbException.class, exc);
        if (message != null) {
            assertTrue("CamelMongoDbException doesn't contain desired message string", camelExc.getMessage().contains(message));
        }
        return camelExc;
    }

}