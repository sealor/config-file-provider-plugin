
package org.jenkinsci.plugins.configfiles.maven.security;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class CredentialsHelperTest {

    final static String PWD     = "MY_NEW_PWD";
    final static String PWD_2   = "{COQLCE6DU6GtcS5P=}";
    final static int SERVER_COUNT = 5;

    @Rule
    public JenkinsRule  jenkins = new JenkinsRule();

    @Test
    public void testIfServerAuthIsReplacedWithinSettingsXmlWhenReplaceTrue() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();
        serverId2Credentials.put("my.server", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));
        serverId2Credentials.put("encoded_pwd", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "*pwd", "some desc2", "dan", PWD_2));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));

        List<String> tempFiles = new ArrayList<String>();
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        Assert.assertTrue("replaced settings.xml must contain new password", replacedContent.contains(PWD));

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        // the 'settings_test.xml' actually contains 4 server tags, but we remove all and only inject the ones managed by the plugin
        Assert.assertEquals("there is not the same number of server tags anymore in the settings.xml", 2, nodes.getLength());

        Node server = (Node) xpath.evaluate("/settings/servers/server[id='my.server']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not at the correct location", PWD, xpath.evaluate("password", server));
        Assert.assertEquals("username is not set correct", "peter", xpath.evaluate("username", server));

        Node server2 = (Node) xpath.evaluate("/settings/servers/server[id='encoded_pwd']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not set correct", PWD_2, xpath.evaluate("password", server2));
        Assert.assertEquals("username is not set correct", "dan", xpath.evaluate("username", server2));

    }

    @Test
    public void testIfServerAuthIsReplacedWithinSettingsXmlWhenReplaceFalse() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();
        serverId2Credentials.put("my.server", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));
        serverId2Credentials.put("encoded_pwd", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "*pwd", "some desc2", "dan", PWD_2));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<String>();
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        Assert.assertTrue("replaced settings.xml must contain new password", replacedContent.contains(PWD));

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/servers/server", doc, XPathConstants.NODESET);
        // the 'settings_test.xml' actually contains 4 server tags, but we remove all and only inject the ones managed by the plugin
        Assert.assertEquals("there is not the same number of server tags anymore in the settings.xml", SERVER_COUNT, nodes.getLength());

    }

    @Test
    public void testIfServerAuthIsReplacedWithinProxySettingsXmlWhenReplaceTrue() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();
        serverId2Credentials.put("proxy1", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "UUID-1", "some desc", "peter", PWD));
        serverId2Credentials.put("proxy2", new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "UUID-2", "some desc2", "dan", PWD_2));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));

        final String replacedContent = CredentialsHelper.fillProxyAuthentication(settingsContent, serverId2Credentials);

        Assert.assertTrue("replaced settings.xml must contain new password", replacedContent.contains(PWD));

        // ensure it is still a valid XML document
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/settings/proxies/proxy", doc, XPathConstants.NODESET);
        Assert.assertEquals("no proxy tag should be removed in the settings.xml", 3, nodes.getLength());

        Node proxy1 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy1']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not at the correct location", PWD, xpath.evaluate("password", proxy1));
        Assert.assertEquals("username is not set correct", "peter", xpath.evaluate("username", proxy1));

        Node proxy2 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy2']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not set correct", PWD_2, xpath.evaluate("password", proxy2));
        Assert.assertEquals("username is not set correct", "dan", xpath.evaluate("username", proxy2));

        Node proxy3 = (Node) xpath.evaluate("/settings/proxies/proxy[id='proxy3']", doc, XPathConstants.NODE);
        Assert.assertEquals("proxy3 should still not have a password", "", xpath.evaluate("password", proxy3));
        Assert.assertEquals("proxy3 should still not have a username", "", xpath.evaluate("username", proxy3));

    }

    @Test
    public void testSettingsXmlIsNotChangedWithoutCredentialsWhenReplaceTrue() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<String>();
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.TRUE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);

    }

    @Test
    public void testSettingsXmlIsNotChangedWithoutCredentialsWhenReplaceFalse() throws Exception {

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<String>();
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, Boolean.FALSE, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        Assert.assertEquals("no changes should have been made to the settings", settingsContent, replacedContent);

    }

    @Issue("JENKINS-39991")
    @Test
    public void testIfServerElementAreKeeptWhenMatchCredentialsWhenReplaceFalse() throws Exception {
        testIfServerElementAreKeeptWhenMatchCredentials(false);

    }

    @Issue("JENKINS-39991")
    @Test
    public void testIfServerElementAreKeeptWhenMatchCredentialsWhenReplaceTrue() throws Exception {
        testIfServerElementAreKeeptWhenMatchCredentials(true);
    }

    private void testIfServerElementAreKeeptWhenMatchCredentials(boolean replaceAll) throws Exception {
        final String serverId = "jenkins-39991";

        Map<String, StandardUsernameCredentials> serverId2Credentials = new HashMap<String, StandardUsernameCredentials>();
        serverId2Credentials.put(serverId, new UsernamePasswordCredentialsImpl(CredentialsScope.SYSTEM, "my*", "some desc", "peter", PWD));

        final String settingsContent = IOUtils.toString(CredentialsHelperTest.class.getResourceAsStream("/settings_test.xml"));
        List<String> tempFiles = new ArrayList<String>();
        final String replacedContent = CredentialsHelper.fillAuthentication(settingsContent, replaceAll, serverId2Credentials, jenkins.jenkins.createPath("tmp"), tempFiles);

        // read original server settings
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(settingsContent)));
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node server = (Node) xpath.evaluate("/settings/servers/server[id='" + serverId + "']", doc, XPathConstants.NODE);
        String filePermissions = xpath.evaluate("filePermissions", server);
        String directoryPermissions = xpath.evaluate("directoryPermissions", server);
        String configuration = xpath.evaluate("configuration", server);

        // ensure it is still a valid XML document
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(replacedContent)));
        // locate the node(s)
        xpath = XPathFactory.newInstance().newXPath();

        server = (Node) xpath.evaluate("/settings/servers/server[id='" + serverId + "']", doc, XPathConstants.NODE);
        Assert.assertEquals("password is not at the correct location", PWD, xpath.evaluate("password", server));
        Assert.assertEquals("username is not set correct", "peter", xpath.evaluate("username", server));
        Assert.assertEquals("filePermissions is not set correct", filePermissions, xpath.evaluate("filePermissions", server));
        Assert.assertEquals("directoryPermissions is not set correct", directoryPermissions, xpath.evaluate("directoryPermissions", server));
        Assert.assertEquals("configuration is not set correct", configuration, xpath.evaluate("configuration", server));
    }

}
