/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osmorc.run.ui;

import com.intellij.execution.ui.AlternativeJREPanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.frameworkintegration.FrameworkIntegrator;
import org.osmorc.frameworkintegration.FrameworkIntegratorRegistry;
import org.osmorc.i18n.OsmorcBundle;
import org.osmorc.run.OsgiRunConfiguration;
import org.osmorc.run.OsgiRunConfigurationChecker;
import org.osmorc.run.OsgiRunConfigurationCheckerProvider;
import org.osmorc.settings.ApplicationSettings;
import org.osmorc.util.OsgiUiUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Editor for a bundle run configuration.
 *
 * @author <a href="mailto:janthomae@janthomae.de">Jan Thom&auml;</a>
 * @author Robert F. Beeger (robert@beeger.net)
 */
public class OsgiRunConfigurationEditor extends SettingsEditor<OsgiRunConfiguration> {
  private JTabbedPane root;
  // framework& bundles tab
  private JComboBox myFrameworkInstances;
  private JSpinner myFrameworkStartLevel;
  private JSpinner myDefaultStartLevel;
  private JPanel myBundlesPanel;
  private JTable myBundlesTable;
  // parameters tab
  private RawCommandLineEditor myVmOptions;
  private RawCommandLineEditor myProgramParameters;
  private JRadioButton myOsmorcControlledDir;
  private JRadioButton myUserDefinedDir;
  private TextFieldWithBrowseButton myWorkingDirField;
  private AlternativeJREPanel myAlternativeJrePanel;
  private JCheckBox myClassPathAllBundles;
  // additional properties tab
  private JPanel myAdditionalPropertiesPanel;

  private final Project myProject;
  private OsgiRunConfiguration myRunConfiguration;
  private FrameworkRunPropertiesEditor myCurrentRunPropertiesEditor;

  public OsgiRunConfigurationEditor(final Project project) {
    myProject = project;

    myFrameworkStartLevel.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)myFrameworkStartLevel.getEditor();
    editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(new JSpinnerCellEditor.MyNumberFormatter("Auto")));

    myDefaultStartLevel.setModel(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));

    DefaultComboBoxModel model = new DefaultComboBoxModel();
    //noinspection unchecked
    model.addElement(null);
    for (FrameworkInstanceDefinition instance : ApplicationSettings.getInstance().getFrameworkInstanceDefinitions()) {
      //noinspection unchecked
      model.addElement(instance);
    }
    //noinspection unchecked
    myFrameworkInstances.setModel(model);
    myFrameworkInstances.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onFrameworkChange();
      }
    });
    //noinspection unchecked
    myFrameworkInstances.setRenderer(new OsgiUiUtil.FrameworkInstanceRenderer("[project default]"));

    myBundlesTable.setModel(new RunConfigurationTableModel());
    myBundlesTable.setRowSelectionAllowed(true);
    myBundlesTable.setColumnSelectionAllowed(false);
    myBundlesTable.setDefaultEditor(Integer.class, new JSpinnerCellEditor());
    myBundlesTable.setDefaultRenderer(Integer.class, new JSpinnerCellEditor());
    myBundlesTable.setAutoCreateRowSorter(true);
    myBundlesPanel.add(
      ToolbarDecorator.createDecorator(myBundlesTable)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onAddClick();
          }
        })
        .setRemoveAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            onRemoveClick();
          }
        })
        .disableUpDownActions()
        .createPanel(), BorderLayout.CENTER
    );
    myBundlesTable.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        int width = myBundlesTable.getWidth();
        if (width > 200) {
          myBundlesTable.getColumnModel().getColumn(0).setPreferredWidth(width - 200);
          myBundlesTable.getColumnModel().getColumn(1).setPreferredWidth(80);
          myBundlesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        }
      }
    });

    myOsmorcControlledDir.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        boolean isUserDefined = !myOsmorcControlledDir.isSelected();
        myWorkingDirField.setEnabled(isUserDefined);
      }
    });

    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    String title = OsmorcBundle.message("run.configuration.working.dir.title");
    String description = OsmorcBundle.message("run.configuration.working.dir.description");
    myWorkingDirField.addBrowseFolderListener(title, description, null, descriptor);
    myWorkingDirField.getTextField().setColumns(30);

    // avoid text fields growing the dialog when much text is entered.
    myVmOptions.getTextField().setPreferredSize(new Dimension(100, 20));
    myProgramParameters.getTextField().setPreferredSize(new Dimension(100, 20));
  }

  /**
   * Called when the framework is changed. This will create a new editor for framework properties and will also remove
   * any framework bundles from the list, as they are no longer in classpath.
   */
  private void onFrameworkChange() {
    if (myFrameworkInstances.getSelectedItem() != null) {
      FrameworkInstanceDefinition instance = (FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem();
      FrameworkIntegrator integrator = FrameworkIntegratorRegistry.getInstance().findIntegratorByInstanceDefinition(instance);
      assert integrator != null : instance;

      // clear the panel
      myAdditionalPropertiesPanel.removeAll();

      // create and install a new editor (if present)
      myCurrentRunPropertiesEditor = integrator.createRunPropertiesEditor();
      if (myCurrentRunPropertiesEditor != null) {
        myAdditionalPropertiesPanel.removeAll();
        myAdditionalPropertiesPanel.add(myCurrentRunPropertiesEditor.getUI(), BorderLayout.CENTER);
        if (myRunConfiguration != null) {
          myCurrentRunPropertiesEditor.resetEditorFrom(myRunConfiguration);
          OsgiRunConfigurationChecker checker = null;
          if (integrator instanceof OsgiRunConfigurationCheckerProvider) {
            checker = ((OsgiRunConfigurationCheckerProvider)integrator).getOsgiRunConfigurationChecker();
          }
          myRunConfiguration.setAdditionalChecker(checker);
        }
      }

      // remove all framework bundles from the list
      RunConfigurationTableModel model = getTableModel();
      model.removeAllOfType(SelectedBundle.BundleType.FrameworkBundle);
    }
  }

  private RunConfigurationTableModel getTableModel() {
    return (RunConfigurationTableModel)myBundlesTable.getModel();
  }

  private void onAddClick() {
    FrameworkInstanceDefinition instance = (FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem();
    BundleSelector selector = new BundleSelector(myProject, instance, getBundlesToRun());
    selector.show();
    if (selector.isOK()) {
      RunConfigurationTableModel model = getTableModel();
      for (SelectedBundle aModule : selector.getSelectedBundles()) {
        model.addBundle(aModule);
      }
    }
  }

  private void onRemoveClick() {
    int[] indices = myBundlesTable.getSelectedRows();
    RunConfigurationTableModel model = getTableModel();
    for (int i = indices.length - 1; i >= 0; i--) {
      model.removeBundleAt(indices[i]);
    }
  }

  @Override
  protected void resetEditorFrom(OsgiRunConfiguration osgiRunConfiguration) {
    myRunConfiguration = osgiRunConfiguration;
    myVmOptions.setText(osgiRunConfiguration.getVmParameters());
    myProgramParameters.setText(osgiRunConfiguration.getProgramParameters());
    myFrameworkInstances.setSelectedItem(osgiRunConfiguration.getInstanceToUse());
    myClassPathAllBundles.setSelected(osgiRunConfiguration.isIncludeAllBundlesInClassPath());

    if (myCurrentRunPropertiesEditor != null) {
      myCurrentRunPropertiesEditor.resetEditorFrom(osgiRunConfiguration);
    }

    // I deliberately set the list of modules as the last step here as
    // the framework specific modules are cleaned out when you change the framework instance
    // so the framework instance should be changed first
    List<SelectedBundle> modules = osgiRunConfiguration.getBundlesToDeploy();
    RunConfigurationTableModel model = getTableModel();
    while (model.getRowCount() > 0) {
      model.removeBundleAt(0);
    }
    for (SelectedBundle module : modules) {
      model.addBundle(module);
    }
    myBundlesTable.getColumnModel().getColumn(1).setPreferredWidth(200);

    myFrameworkStartLevel.setValue(osgiRunConfiguration.getFrameworkStartLevel());
    myDefaultStartLevel.setValue(osgiRunConfiguration.getDefaultStartLevel());

    boolean useUserDefinedFields = !osgiRunConfiguration.isGenerateWorkingDir();
    myWorkingDirField.setText(osgiRunConfiguration.getWorkingDir());
    if (myWorkingDirField.getText().length() == 0) {
      final CompilerProjectExtension extension = CompilerProjectExtension.getInstance(myProject);
      if (extension != null) {
        final VirtualFilePointer outputDirPointer = extension.getCompilerOutputPointer();
        if (outputDirPointer != null) {
          myWorkingDirField.setText(VfsUtilCore.urlToPath(outputDirPointer.getUrl() + "/run.osgi/"));
        }
      }
    }

    myWorkingDirField.setEnabled(useUserDefinedFields);
    myUserDefinedDir.setSelected(useUserDefinedFields);
    myOsmorcControlledDir.setSelected(!useUserDefinedFields);
    myAlternativeJrePanel.init(osgiRunConfiguration.getAlternativeJrePath(), osgiRunConfiguration.isUseAlternativeJre());
  }

  @Override
  protected void applyEditorTo(OsgiRunConfiguration osgiRunConfiguration) throws ConfigurationException {
    osgiRunConfiguration.setBundlesToDeploy(getBundlesToRun());
    osgiRunConfiguration.setVmParameters(myVmOptions.getText());
    osgiRunConfiguration.setProgramParameters(myProgramParameters.getText());
    osgiRunConfiguration.setIncludeAllBundlesInClassPath(myClassPathAllBundles.isSelected());
    osgiRunConfiguration.setWorkingDir(myWorkingDirField.getText().replace('\\', '/'));
    osgiRunConfiguration.setUseAlternativeJre(myAlternativeJrePanel.isPathEnabled());
    osgiRunConfiguration.setAlternativeJrePath(myAlternativeJrePanel.getPath());
    osgiRunConfiguration.setFrameworkStartLevel((Integer)myFrameworkStartLevel.getValue());
    osgiRunConfiguration.setDefaultStartLevel((Integer)myDefaultStartLevel.getValue());
    osgiRunConfiguration.setGenerateWorkingDir(myOsmorcControlledDir.isSelected());
    osgiRunConfiguration.setInstanceToUse((FrameworkInstanceDefinition)myFrameworkInstances.getSelectedItem());

    if (myCurrentRunPropertiesEditor != null) {
      myCurrentRunPropertiesEditor.applyEditorTo(osgiRunConfiguration);
    }
  }

  private List<SelectedBundle> getBundlesToRun() {
    return getTableModel().getBundles();
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return root;
  }

  @Override
  protected void disposeEditor() { }


  private static class RunConfigurationTableModel extends AbstractTableModel {
    private final List<SelectedBundle> mySelectedBundles;

    public RunConfigurationTableModel() {
      mySelectedBundles = new ArrayList<SelectedBundle>();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return String.class;
        case 1:
          return Integer.class;
        case 2:
          return Boolean.class;
        default:
          return Object.class;
      }
    }

    @Override
    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return "Bundle name";
        case 1:
          return "Start level";
        case 2:
          return "Start after install";
        default:
          return "";
      }
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public int getRowCount() {
      return mySelectedBundles.size();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
      return column != 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
      SelectedBundle bundle = getBundleAt(row);
      switch (column) {
        case 0:
          return bundle.toString();
        case 1:
          return bundle.getStartLevel();
        case 2:
          return bundle.isStartAfterInstallation();
        default:
          throw new RuntimeException("Don't know column " + column);
      }
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      SelectedBundle bundle = getBundleAt(row);
      switch (column) {
        case 1:
          bundle.setStartLevel((Integer)o);
          break;
        case 2:
          bundle.setStartAfterInstallation((Boolean)o);
          break;
        default:
          throw new RuntimeException("Cannot edit column " + column);
      }
    }

    public SelectedBundle getBundleAt(int index) {
      return mySelectedBundles.get(index);
    }

    public List<SelectedBundle> getBundles() {
      return mySelectedBundles;
    }

    public void addBundle(SelectedBundle bundle) {
      mySelectedBundles.add(bundle);
      fireTableRowsInserted(mySelectedBundles.size() - 1, mySelectedBundles.size() - 1);
    }

    public void removeBundleAt(int index) {
      mySelectedBundles.remove(index);
      fireTableRowsDeleted(index, index);
    }

    public void removeAllOfType(SelectedBundle.BundleType type) {
      for (Iterator<SelectedBundle> selectedBundleIterator = mySelectedBundles.iterator(); selectedBundleIterator.hasNext(); ) {
        SelectedBundle selectedBundle = selectedBundleIterator.next();
        if (selectedBundle.getBundleType() == type) {
          selectedBundleIterator.remove();
        }
      }
      fireTableDataChanged();
    }
  }
}
