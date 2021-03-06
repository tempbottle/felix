/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.itest.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.junit.Assert;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ConfigurationDependencyTest extends TestBase {
    final static String PID = "ConfigurationDependencyTest.pid";
    
    public void testComponentWithRequiredConfigurationAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 50000000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testComponentWithRequiredConfigurationAndCallbackInstanceAndServicePropertyPropagation() {
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        ConfigurationConsumerCallbackInstance callbackInstance = new ConfigurationConsumerCallbackInstance(e);
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumerWithCallbackInstance(e))
            .setInterface(Runnable.class.getName(), null)
            .add(m.createConfigurationDependency().setPid(PID).setPropagate(true).setCallback(callbackInstance, "updateConfiguration"));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e))
            .add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e))
            .add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }
    
    public void testFELIX2987() {
        // mimics testComponentWithRequiredConfigurationAndServicePropertyPropagation
        DependencyManager m = getDM();
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        // create a service provider and consumer
        Component s1 = m.createComponent().setImplementation(new ConfigurationConsumer2(e)).setInterface(Runnable.class.getName(), null).add(m.createConfigurationDependency().setPid(PID).setPropagate(true));
        Component s2 = m.createComponent().setImplementation(new ConfigurationCreator(e)).add(m.createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true));
        Component s3 = m.createComponent().setImplementation(new ConfiguredServiceConsumer(e)).add(m.createServiceDependency().setService(Runnable.class, ("(testkey=testvalue)")).setRequired(true));
        m.add(s1);
        m.add(s2);
        m.add(s3);
        e.waitForStep(4, 5000);
        m.remove(s1);
        m.remove(s2);
        m.remove(s3);
        // ensure we executed all steps inside the component instance
        e.step(6);
    }


    class ConfigurationCreator {
        private volatile ConfigurationAdmin m_ca;
        private final Ensure m_ensure;
        Configuration m_conf;
        
        public ConfigurationCreator(Ensure e) {
            m_ensure = e;
        }

        public void init() {
            try {
                warn("ConfigurationCreator.init");
            	Assert.assertNotNull(m_ca);
                m_ensure.step(1);
                m_conf = m_ca.getConfiguration(PID, null);
                Hashtable props = new Properties();
                props.put("testkey", "testvalue");
                m_conf.update(props);
            }
            catch (IOException e) {
                Assert.fail("Could not create configuration: " + e.getMessage());
            }
        }
        
        public void destroy() throws IOException {
            warn("ConfigurationCreator.destroy");
        	m_conf.delete();  
        	m_ensure.step();
        }
    }
    
    static class ConfigurationConsumer2 extends ConfigurationConsumer {
        public ConfigurationConsumer2(Ensure e) {
            super(e);
        }
    }

    static class ConfigurationConsumer implements ManagedService, Runnable {
        private final Ensure m_ensure;

        public ConfigurationConsumer(Ensure e) {
            m_ensure = e;
        }

        public void updated(Dictionary props) throws ConfigurationException {
            if (props != null) {
                m_ensure.step(2);
                if (!"testvalue".equals(props.get("testkey"))) {
                    Assert.fail("Could not find the configured property.");
                }
            }
        }
        
        public void run() {
            m_ensure.step(4);
        }
    }
    
    static class ConfigurationConsumerCallbackInstance {
        private final Ensure m_ensure;

        public ConfigurationConsumerCallbackInstance(Ensure e) {
            m_ensure = e;
        }
        
        public void updateConfiguration(Dictionary props) throws Exception {
            if (props != null) {
                m_ensure.step(2);
                if (!"testvalue".equals(props.get("testkey"))) {
                    Assert.fail("Could not find the configured property.");
                }
            } 
        }
    }
    
    static class ConfigurationConsumerWithCallbackInstance implements Runnable {
        private final Ensure m_ensure;

        public ConfigurationConsumerWithCallbackInstance(Ensure e) {
            m_ensure = e;
        }
        
        public void run() {
            m_ensure.step(4);
        }
    }

    static class ConfiguredServiceConsumer {
        private final Ensure m_ensure;
        private volatile Runnable m_runnable;

        public ConfiguredServiceConsumer(Ensure e) {
            m_ensure = e;
        }
        
        public void init() {
            m_ensure.step(3);
            m_runnable.run();
        }
    }
}
