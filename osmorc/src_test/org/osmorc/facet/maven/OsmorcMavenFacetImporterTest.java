package org.osmorc.facet.maven;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.importing.FacetImporterTestCase;
import org.junit.Test;
import org.osmorc.facet.OsmorcFacet;
import org.osmorc.facet.OsmorcFacetConfiguration;
import org.osmorc.facet.OsmorcFacetType;

import java.io.IOException;

/**
 * Test case for testing the osmorc facet importer.
 */
public class OsmorcMavenFacetImporterTest extends FacetImporterTestCase<OsmorcFacet, OsmorcFacetType> {
  @Override
  protected FacetImporter<OsmorcFacet, ?, OsmorcFacetType> createImporter() {
    return new OsmorcFacetImporter();
  }

  protected String pluginCoordinates() {
    return "<groupId>" + "org.apache.felix" + "</groupId>" +
           "<artifactId>" + "maven-bundle-plugin" + "</artifactId>" +
           "<version>" + "2.3.4" + "</version>";
  }


  @Test
  public void testSimpleImport() throws IOException {
    importProject(pomContents("simple", "", ""));
    assertModules("simple");
    OsmorcFacet simple = getFacet("simple");
    OsmorcFacetConfiguration configuration = simple.getConfiguration();
    assertNotNull(configuration);
    assertEquals("org.osmorc.simple", configuration.getBundleSymbolicName());
    assertTrue(configuration.isOsmorcControlsManifest());
  }

 @Test
  public void testSymbolicNameInference() throws IOException {
    importProject(pomContents("osmorc-simple", "", ""));
    assertModules("osmorc-simple");
    OsmorcFacet simple = getFacet("osmorc-simple");
    OsmorcFacetConfiguration configuration = simple.getConfiguration();
    assertNotNull(configuration);
    assertEquals("org.osmorc.simple", configuration.getBundleSymbolicName());
    assertTrue(configuration.isOsmorcControlsManifest());
  }

  @Test
  public void testUseExistingManifest() throws IOException {
    importProject(pomContents("simple",
                              "<configuration>"+
     "<instructions>"+
    "<_include>${project.basedir}/foo/Manifest.MF</_include>"+
    "</instructions>"+
    "</configuration>", ""));
    assertModules("simple");
    OsmorcFacet simple = getFacet("simple");
    OsmorcFacetConfiguration configuration = simple.getConfiguration();
    assertNotNull(configuration);
    assertEquals("org.osmorc.simple", configuration.getBundleSymbolicName());
    assertFalse(configuration.isOsmorcControlsManifest());
    assertFalse(configuration.isUseProjectDefaultManifestFileLocation());
    assertTrue(configuration.getManifestLocation().contains("foo/Manifest.MF"));
  }

  @Test
    public void testDefaultOutputPath() throws IOException {
      importProject(pomContents("simple","", ""));
      assertModules("simple");
      OsmorcFacet simple = getFacet("simple");
      OsmorcFacetConfiguration configuration = simple.getConfiguration();
      assertNotNull(configuration);
      assertTrue(configuration.getJarFileLocation().endsWith("simple-1.0.1.jar"));
    }

  @Test
    public void testSpecificOutputPath() throws IOException {
      importProject(pomContents("simple","", "<finalName>${artifactId}-special-${version}</finalName>"));
      assertModules("simple");
      OsmorcFacet simple = getFacet("simple");
      OsmorcFacetConfiguration configuration = simple.getConfiguration();
      assertNotNull(configuration);
      assertTrue(configuration.getJarFileLocation().endsWith("simple-special-1.0.1.jar"));
    }







  private String pomContents(@NotNull String artifactId, @NotNull String pluginInstructions, @NotNull String buildInstructions) {
    return "<groupId>org.osmorc</groupId>" +
                  "<artifactId>"+artifactId+"</artifactId>" +
                  "<version>1.0.1</version>" +
                  "<packaging>bundle</packaging>"+

                  "<build>" +
                  buildInstructions+
                  "  <plugins>" +
                  "    <plugin>" +
                  pluginCoordinates() +
                  pluginInstructions+
                  "    </plugin>" +
                  "  </plugins>" +
                  "</build>";
  }
}
