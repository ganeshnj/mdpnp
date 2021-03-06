package org.mdpnp.devices;

import ice.MDSConnectivity;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mdpnp.rtiapi.data.EventLoop;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.publication.Publisher;
import com.rti.dds.subscription.Subscriber;

public class PartitionAssignmentControllerTest {

    ice.DeviceIdentity deviceIdentity = new ice.DeviceIdentity();

    private static ConfigurableApplicationContext createContext() {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] { "RtConfig.xml" }, false);
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        Properties props = new Properties();
        props.setProperty("mdpnp.domain", "0");
        ppc.setProperties(props);
        ppc.setOrder(0);

        ctx.addBeanFactoryPostProcessor(ppc);
        ctx.refresh();
        return ctx;
    }
    
    @Test
    public void testPartitionNames() {

        String[] partitions = { "A", "B", "C"};
        String s = PartitionAssignmentController.toString(partitions);
        Assert.assertEquals("A,B,C", s);
    }

    @Test
    public void testSetPartition() throws Exception{
        ConfigurableApplicationContext ctx = createContext();
        
        final DomainParticipant participant = ctx.getBean("domainParticipant", DomainParticipant.class);
        final Subscriber subscriber = ctx.getBean("subscriber", Subscriber.class);
        final Publisher  publisher  = ctx.getBean("publisher", Publisher.class);
        final EventLoop eventLoop   = ctx.getBean("eventLoop", EventLoop.class);
        
        try {
            final CountDownLatch stopOk = new CountDownLatch(1);

            PartitionAssignmentController controller =
                    new PartitionAssignmentController(deviceIdentity,
                                                      participant,
                                                      eventLoop,
                                                      publisher,
                                                      subscriber);

            MDSHandler mdsHandler = controller.getConnectivityAdapter();
            mdsHandler.addConnectivityListener(new MDSHandler.Connectivity.MDSListener() {
                @Override
                public void handleDataSampleEvent(MDSHandler.Connectivity.MDSEvent evt) {
                    MDSConnectivity v = (MDSConnectivity) evt.getSource();
                    if (deviceIdentity.unique_device_identifier.equals(v.unique_device_identifier))
                        stopOk.countDown();
                }
            });
            controller.start();

            String[] partitions = { "testPartition1", "testPartition2", "testPartition3"};
            controller.setPartition(partitions);

            boolean isOk = stopOk.await(5000, TimeUnit.MILLISECONDS);
            controller.shutdown();
            if (!isOk)
                Assert.fail("Did not get publication method");
        }
        catch (Exception ex) {
            throw ex;
        }
        finally {
            ctx.close();
        }
    }

    @Test
    public void testCheckForNoPartitionFile() {

        PartitionAssignmentController controller = new PartitionAssignmentController(deviceIdentity) {
            public void setPartition(String[] partition) {
                Assert.assertEquals(0, partition.length);
            }
        };

        controller.checkForPartitionFile(null);
    }

    @Test
    public void testCheckForPartitionFile() {

        URL u = getClass().getResource("device.partition.0.txt");
        String f = u.getFile();

        PartitionAssignmentController controller = new PartitionAssignmentController(deviceIdentity) {
            public void setPartition(String[] partition) {
                Assert.assertEquals(2, partition.length);
                Assert.assertEquals("foo", partition[0]);
                Assert.assertEquals("bar", partition[1]);
            }
        };

        controller.checkForPartitionFile(new File(f));
    }
}


