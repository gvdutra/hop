/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.salesforceupsert;

import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.SourceToTargetMapping;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaNone;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnection;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceConnectionUtils;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformDialog;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.EnterMappingDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.LabelTextVar;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.apache.hop.ui.pipeline.transform.ComponentSelectionListener;
import org.apache.hop.ui.pipeline.transform.ITableItemInsertListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SalesforceUpsertDialog extends SalesforceTransformDialog {

  private static final Class<?> PKG = SalesforceUpsertMeta.class; // For Translator

  private Map<String, Integer> inputFields;

  private ColumnInfo[] ciReturn;

  private TableView wReturn;

  private SalesforceUpsertMeta input;

  private LabelTextVar wUserName;
  private LabelTextVar wURL;
  private LabelTextVar wPassword;

  private TextVar wBatchSize;

  private ComboVar wModule;

  private CCombo wUpsertField;

  private TextVar wSalesforceIDFieldName;

  private boolean gotModule = false;

  private boolean getModulesListError = false; /* True if error getting modules list */

  private Button wUseCompression;

  private TextVar wTimeOut;

  private Button wRollbackAllChangesOnError;

  /** List of ColumnInfo that should have the field names of the selected database table */
  private static List<ColumnInfo> tableFieldColumns = new ArrayList<>();

  private String[] moduleFields;

  public SalesforceUpsertDialog(
      Shell parent, IVariables variables, Object in, PipelineMeta pipelineMeta, String sname) {
    super(parent, variables, in, pipelineMeta, sname);
    input = (SalesforceUpsertMeta) in;
    inputFields = new HashMap<>();
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    ModifyListener lsTableMod =
        arg0 -> {
          input.setChanged();
          moduleFields = null;
        };
    SelectionAdapter lsSelection =
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            input.setChanged();
            moduleFields = null;
          }
        };
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.DialogTitle"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.Label.TransformName"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.top = new FormAttachment(0, margin);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    CTabFolder wTabFolder = new CTabFolder(shell, SWT.BORDER);
    PropsUi.setLook(wTabFolder, Props.WIDGET_STYLE_TAB);

    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////
    CTabItem wGeneralTab = new CTabItem(wTabFolder, SWT.NONE);
    wGeneralTab.setFont(GuiResource.getInstance().getFontDefault());
    wGeneralTab.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.General.Tab"));

    Composite wGeneralComp = new Composite(wTabFolder, SWT.NONE);
    PropsUi.setLook(wGeneralComp);

    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wGeneralComp.setLayout(generalLayout);

    // ///////////////////////////////
    // START OF Connection GROUP //
    // ///////////////////////////////

    Group wConnectionGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wConnectionGroup);
    wConnectionGroup.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.ConnectionGroup.Label"));

    FormLayout connectionGroupLayout = new FormLayout();
    connectionGroupLayout.marginWidth = 10;
    connectionGroupLayout.marginHeight = 10;
    wConnectionGroup.setLayout(connectionGroupLayout);

    // Webservice URL
    wURL =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.URL.Label"),
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.URL.Tooltip"));
    PropsUi.setLook(wURL);
    wURL.addModifyListener(lsMod);
    FormData fdURL = new FormData();
    fdURL.left = new FormAttachment(0, 0);
    fdURL.top = new FormAttachment(wTransformName, margin);
    fdURL.right = new FormAttachment(100, 0);
    wURL.setLayoutData(fdURL);

    // UserName line
    wUserName =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.User.Label"),
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.User.Tooltip"));
    PropsUi.setLook(wUserName);
    wUserName.addModifyListener(lsMod);
    FormData fdUserName = new FormData();
    fdUserName.left = new FormAttachment(0, 0);
    fdUserName.top = new FormAttachment(wURL, margin);
    fdUserName.right = new FormAttachment(100, 0);
    wUserName.setLayoutData(fdUserName);

    // Password line
    wPassword =
        new LabelTextVar(
            variables,
            wConnectionGroup,
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.Password.Label"),
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.Password.Tooltip"),
            true);
    PropsUi.setLook(wPassword);
    wPassword.addModifyListener(lsMod);
    FormData fdPassword = new FormData();
    fdPassword.left = new FormAttachment(0, 0);
    fdPassword.top = new FormAttachment(wUserName, margin);
    fdPassword.right = new FormAttachment(100, 0);
    wPassword.setLayoutData(fdPassword);

    // Test Salesforce connection button
    Button wTest = new Button(wConnectionGroup, SWT.PUSH);
    wTest.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.TestConnection.Label"));
    PropsUi.setLook(wTest);
    FormData fdTest = new FormData();
    wTest.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.TestConnection.Tooltip"));
    fdTest.top = new FormAttachment(wPassword, margin);
    fdTest.right = new FormAttachment(100, 0);
    wTest.setLayoutData(fdTest);

    FormData fdConnectionGroup = new FormData();
    fdConnectionGroup.left = new FormAttachment(0, margin);
    fdConnectionGroup.top = new FormAttachment(wTransformName, margin);
    fdConnectionGroup.right = new FormAttachment(100, -margin);
    wConnectionGroup.setLayoutData(fdConnectionGroup);

    // ///////////////////////////////
    // END OF Connection GROUP //
    // ///////////////////////////////

    // ///////////////////////////////
    // START OF Settings GROUP //
    // ///////////////////////////////

    Group wSettingsGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wSettingsGroup);
    wSettingsGroup.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.SettingsGroup.Label"));

    FormLayout settingGroupLayout = new FormLayout();
    settingGroupLayout.marginWidth = 10;
    settingGroupLayout.marginHeight = 10;
    wSettingsGroup.setLayout(settingGroupLayout);

    // Timeout
    Label wlTimeOut = new Label(wSettingsGroup, SWT.RIGHT);
    wlTimeOut.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.TimeOut.Label"));
    PropsUi.setLook(wlTimeOut);
    FormData fdlTimeOut = new FormData();
    fdlTimeOut.left = new FormAttachment(0, 0);
    fdlTimeOut.top = new FormAttachment(wSettingsGroup, margin);
    fdlTimeOut.right = new FormAttachment(middle, -margin);
    wlTimeOut.setLayoutData(fdlTimeOut);
    wTimeOut = new TextVar(variables, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wTimeOut);
    wTimeOut.addModifyListener(lsMod);
    FormData fdTimeOut = new FormData();
    fdTimeOut.left = new FormAttachment(middle, 0);
    fdTimeOut.top = new FormAttachment(wSettingsGroup, margin);
    fdTimeOut.right = new FormAttachment(100, 0);
    wTimeOut.setLayoutData(fdTimeOut);

    // Use compression?
    Label wlUseCompression = new Label(wSettingsGroup, SWT.RIGHT);
    wlUseCompression.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.UseCompression.Label"));
    PropsUi.setLook(wlUseCompression);
    FormData fdlUseCompression = new FormData();
    fdlUseCompression.left = new FormAttachment(0, 0);
    fdlUseCompression.top = new FormAttachment(wTimeOut, margin);
    fdlUseCompression.right = new FormAttachment(middle, -margin);
    wlUseCompression.setLayoutData(fdlUseCompression);
    wUseCompression = new Button(wSettingsGroup, SWT.CHECK);
    PropsUi.setLook(wUseCompression);
    wUseCompression.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.UseCompression.Tooltip"));
    FormData fdUseCompression = new FormData();
    fdUseCompression.left = new FormAttachment(middle, 0);
    fdUseCompression.top = new FormAttachment(wlUseCompression, 0, SWT.CENTER);
    wUseCompression.setLayoutData(fdUseCompression);
    wUseCompression.addSelectionListener(new ComponentSelectionListener(input));

    // Rollback all changes on error?
    Label wlRollbackAllChangesOnError = new Label(wSettingsGroup, SWT.RIGHT);
    wlRollbackAllChangesOnError.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.RollbackAllChangesOnError.Label"));
    PropsUi.setLook(wlRollbackAllChangesOnError);
    FormData fdlRollbackAllChangesOnError = new FormData();
    fdlRollbackAllChangesOnError.left = new FormAttachment(0, 0);
    fdlRollbackAllChangesOnError.top = new FormAttachment(wUseCompression, margin);
    fdlRollbackAllChangesOnError.right = new FormAttachment(middle, -margin);
    wlRollbackAllChangesOnError.setLayoutData(fdlRollbackAllChangesOnError);
    wRollbackAllChangesOnError = new Button(wSettingsGroup, SWT.CHECK);
    wRollbackAllChangesOnError.addSelectionListener(new ComponentSelectionListener(input));
    PropsUi.setLook(wRollbackAllChangesOnError);
    wRollbackAllChangesOnError.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.RollbackAllChangesOnError.Tooltip"));
    FormData fdRollbackAllChangesOnError = new FormData();
    fdRollbackAllChangesOnError.left = new FormAttachment(middle, 0);
    fdRollbackAllChangesOnError.top =
        new FormAttachment(wlRollbackAllChangesOnError, 0, SWT.CENTER);
    wRollbackAllChangesOnError.setLayoutData(fdRollbackAllChangesOnError);

    // BatchSize value
    Label wlBatchSize = new Label(wSettingsGroup, SWT.RIGHT);
    wlBatchSize.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.Limit.Label"));
    PropsUi.setLook(wlBatchSize);
    FormData fdlBatchSize = new FormData();
    fdlBatchSize.left = new FormAttachment(0, 0);
    fdlBatchSize.top = new FormAttachment(wRollbackAllChangesOnError, margin);
    fdlBatchSize.right = new FormAttachment(middle, -margin);
    wlBatchSize.setLayoutData(fdlBatchSize);
    wBatchSize = new TextVar(variables, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wBatchSize);
    wBatchSize.addModifyListener(lsMod);
    FormData fdBatchSize = new FormData();
    fdBatchSize.left = new FormAttachment(middle, 0);
    fdBatchSize.top = new FormAttachment(wRollbackAllChangesOnError, margin);
    fdBatchSize.right = new FormAttachment(100, 0);
    wBatchSize.setLayoutData(fdBatchSize);

    // Module
    Label wlModule = new Label(wSettingsGroup, SWT.RIGHT);
    wlModule.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.Module.Label"));
    PropsUi.setLook(wlModule);
    FormData fdlModule = new FormData();
    fdlModule.left = new FormAttachment(0, 0);
    fdlModule.top = new FormAttachment(wBatchSize, margin);
    fdlModule.right = new FormAttachment(middle, -margin);
    wlModule.setLayoutData(fdlModule);
    wModule = new ComboVar(variables, wSettingsGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
    wModule.setEditable(true);
    PropsUi.setLook(wModule);
    wModule.addModifyListener(lsTableMod);
    wModule.addSelectionListener(lsSelection);
    FormData fdModule = new FormData();
    fdModule.left = new FormAttachment(middle, 0);
    fdModule.top = new FormAttachment(wBatchSize, margin);
    fdModule.right = new FormAttachment(100, -margin);
    wModule.setLayoutData(fdModule);
    wModule.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {}

          @Override
          public void focusGained(FocusEvent e) {
            // check if the URL and login credentials passed and not just had error
            if (skipFetchModules()) {
              getModulesListError = false;
              return;
            }

            Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
            shell.setCursor(busy);
            getModulesList();
            shell.setCursor(null);
            busy.dispose();
          }
        });

    // Upsert Field
    Label wlUpsertField = new Label(wSettingsGroup, SWT.RIGHT);
    wlUpsertField.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.Upsert.Label"));
    PropsUi.setLook(wlUpsertField);
    FormData fdlUpsertField = new FormData();
    fdlUpsertField.left = new FormAttachment(0, 0);
    fdlUpsertField.top = new FormAttachment(wModule, margin);
    fdlUpsertField.right = new FormAttachment(middle, -margin);
    wlUpsertField.setLayoutData(fdlUpsertField);
    wUpsertField = new CCombo(wSettingsGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
    wUpsertField.setEditable(true);
    PropsUi.setLook(wUpsertField);
    wUpsertField.addModifyListener(lsMod);
    FormData fdUpsertField = new FormData();
    fdUpsertField.left = new FormAttachment(middle, 0);
    fdUpsertField.top = new FormAttachment(wModule, margin);
    fdUpsertField.right = new FormAttachment(100, -margin);
    wUpsertField.setLayoutData(fdUpsertField);
    wUpsertField.addFocusListener(
        new FocusListener() {
          @Override
          public void focusLost(FocusEvent e) {}

          @Override
          public void focusGained(FocusEvent e) {
            getFieldsList();
          }
        });

    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(wConnectionGroup, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    wSettingsGroup.setLayoutData(fdSettingsGroup);

    // ///////////////////////////////
    // END OF Settings GROUP //
    // ///////////////////////////////

    // ///////////////////////////////
    // START OF OutFields GROUP //
    // ///////////////////////////////

    Group wOutFieldsGroup = new Group(wGeneralComp, SWT.SHADOW_NONE);
    PropsUi.setLook(wOutFieldsGroup);
    wOutFieldsGroup.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.OutFieldsGroup.Label"));

    FormLayout outFieldsGroupLayout = new FormLayout();
    outFieldsGroupLayout.marginWidth = 10;
    outFieldsGroupLayout.marginHeight = 10;
    wOutFieldsGroup.setLayout(outFieldsGroupLayout);

    // SalesforceIDFieldName
    Label wlSalesforceIDFieldName = new Label(wOutFieldsGroup, SWT.RIGHT);
    wlSalesforceIDFieldName.setText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.SalesforceIDFieldName.Label"));
    PropsUi.setLook(wlSalesforceIDFieldName);
    FormData fdlSalesforceIDFieldName = new FormData();
    fdlSalesforceIDFieldName.left = new FormAttachment(0, 0);
    fdlSalesforceIDFieldName.top = new FormAttachment(wSettingsGroup, margin);
    fdlSalesforceIDFieldName.right = new FormAttachment(middle, -margin);
    wlSalesforceIDFieldName.setLayoutData(fdlSalesforceIDFieldName);
    wSalesforceIDFieldName =
        new TextVar(variables, wOutFieldsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    PropsUi.setLook(wSalesforceIDFieldName);
    wSalesforceIDFieldName.setToolTipText(
        BaseMessages.getString(PKG, "SalesforceUpsertDialog.SalesforceIDFieldName.Tooltip"));
    wSalesforceIDFieldName.addModifyListener(lsMod);
    FormData fdSalesforceIDFieldName = new FormData();
    fdSalesforceIDFieldName.left = new FormAttachment(middle, 0);
    fdSalesforceIDFieldName.top = new FormAttachment(wSettingsGroup, margin);
    fdSalesforceIDFieldName.right = new FormAttachment(100, 0);
    wSalesforceIDFieldName.setLayoutData(fdSalesforceIDFieldName);

    FormData fdOutFieldsGroup = new FormData();
    fdOutFieldsGroup.left = new FormAttachment(0, margin);
    fdOutFieldsGroup.top = new FormAttachment(wSettingsGroup, margin);
    fdOutFieldsGroup.right = new FormAttachment(100, -margin);
    wOutFieldsGroup.setLayoutData(fdOutFieldsGroup);

    // ///////////////////////////////
    // END OF OutFields GROUP //
    // ///////////////////////////////

    // THE UPDATE/INSERT TABLE
    Label wlReturn = new Label(wGeneralComp, SWT.NONE);
    wlReturn.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.UpdateFields.Label"));
    PropsUi.setLook(wlReturn);
    FormData fdlReturn = new FormData();
    fdlReturn.left = new FormAttachment(0, 0);
    fdlReturn.top = new FormAttachment(wOutFieldsGroup, margin);
    wlReturn.setLayoutData(fdlReturn);

    int upInsCols = 3;
    int upInsRows = (input.getUpdateLookup() != null ? input.getUpdateLookup().length : 1);

    ciReturn = new ColumnInfo[upInsCols];
    ciReturn[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.ColumnInfo.TableField"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            new String[] {""},
            false);
    ciReturn[1] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.ColumnInfo.StreamField"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            new String[] {""},
            false);
    ciReturn[2] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.ColumnInfo.UseExternalId"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            new String[] {"Y", "N"});
    ciReturn[2].setToolTip(
        BaseMessages.getString(PKG, "SalesforceUpdateDialog.ColumnInfo.UseExternalId.Tooltip"));
    tableFieldColumns.add(ciReturn[0]);
    wReturn =
        new TableView(
            variables,
            wGeneralComp,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
            ciReturn,
            upInsRows,
            lsMod,
            props);
    wReturn
        .getTable()
        .addFocusListener(
            new FocusListener() {

              @Override
              public void focusGained(FocusEvent e) {
                setModuleFieldCombo();
              }

              @Override
              public void focusLost(FocusEvent e) {}
            });

    Button wDoMapping = new Button(wGeneralComp, SWT.PUSH);
    wDoMapping.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.EditMapping.Label"));
    FormData fdDoMapping = new FormData();
    fdDoMapping.top = new FormAttachment(wlReturn, margin);
    fdDoMapping.right = new FormAttachment(100, 0);
    wDoMapping.setLayoutData(fdDoMapping);
    wDoMapping.addListener(SWT.Selection, arg0 -> generateMappings());

    FormData fdReturn = new FormData();
    fdReturn.left = new FormAttachment(0, 0);
    fdReturn.top = new FormAttachment(wlReturn, margin);
    fdReturn.right = new FormAttachment(wDoMapping, -margin);
    fdReturn.bottom = new FormAttachment(100, -2 * margin);
    wReturn.setLayoutData(fdReturn);

    Button wGetLU = new Button(wGeneralComp, SWT.PUSH);
    wGetLU.setText(BaseMessages.getString(PKG, "SalesforceUpsertDialog.GetAndUpdateFields.Label"));
    FormData fdGetLU = new FormData();
    fdGetLU.top = new FormAttachment(wDoMapping, margin);
    fdGetLU.left = new FormAttachment(wReturn, margin);
    fdGetLU.right = new FormAttachment(100, 0);
    wGetLU.setLayoutData(fdGetLU);

    //
    // Search the fields in the background
    //

    final Runnable runnable =
        () -> {
          TransformMeta transformMeta = pipelineMeta.findTransform(transformName);
          if (transformMeta != null) {
            try {
              IRowMeta row = pipelineMeta.getPrevTransformFields(variables, transformMeta);

              // Remember these fields...
              for (int i = 0; i < row.size(); i++) {
                inputFields.put(row.getValueMeta(i).getName(), Integer.valueOf(i));
              }

              setComboBoxes();
              // Dislay in red missing field names
              HopGui.getInstance().getDisplay()
                  .asyncExec(
                      () -> {
                        if (!wReturn.isDisposed()) {
                          for (int i = 0; i < wReturn.table.getItemCount(); i++) {
                            TableItem it = wReturn.table.getItem(i);
                            if (!Utils.isEmpty(it.getText(2))
                                && !inputFields.containsKey(it.getText(2))) {
                              it.setBackground(GuiResource.getInstance().getColorRed());
                            }
                          }
                        }
                      });
            } catch (HopException e) {
              logError(BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"));
            }
          }
        };
    new Thread(runnable).start();

    FormData fdGeneralComp = new FormData();
    fdGeneralComp.left = new FormAttachment(0, 0);
    fdGeneralComp.top = new FormAttachment(wTransformName, margin);
    fdGeneralComp.right = new FormAttachment(100, 0);
    fdGeneralComp.bottom = new FormAttachment(100, 0);
    wGeneralComp.setLayoutData(fdGeneralComp);

    wGeneralComp.layout();
    wGeneralTab.setControl(wGeneralComp);

    // THE BUTTONS
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0);
    fdTabFolder.top = new FormAttachment(wTransformName, margin);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(wOk, -margin);
    wTabFolder.setLayoutData(fdTabFolder);

    // Add listeners
    wOk.addListener(SWT.Selection, e -> ok());
    wGetLU.addListener(SWT.Selection, e -> getUpdate());
    wTest.addListener(SWT.Selection, e -> test());
    wCancel.addListener(SWT.Selection, e -> cancel());

    wTabFolder.setSelection(0);
    getData(input);
    input.setChanged(changed);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void getUpdate() {
    try {
      IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
      if (r != null) {
        ITableItemInsertListener listener =
            (tableItem, v) -> {
              tableItem.setText(3, "Y");
              return true;
            };
        BaseTransformDialog.getFieldsFromPrevious(
            r, wReturn, 1, new int[] {1, 2}, new int[] {}, -1, -1, listener);
      }
    } catch (HopException ke) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.FailedToGetFields.DialogMessage"),
          ke);
    }
  }

  private void getFieldsList() {
    try {
      String selectedField = wUpsertField.getText();
      wUpsertField.removeAll();
      wUpsertField.setItems(getModuleFields());

      if (!Utils.isEmpty(selectedField)) {
        wUpsertField.setText(selectedField);
      }
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.ErrorRetrieveModules.DialogTitle"),
          BaseMessages.getString(
              PKG, "SalesforceUpsertDialog.ErrorRetrieveData.ErrorRetrieveModules"),
          e);
    }
  }

  /**
   * Read the data from the TextFileInputMeta object and show it in this dialog.
   *
   * @param in The SalesforceUpsertMeta object to obtain the data from.
   */
  public void getData(SalesforceUpsertMeta in) {
    wURL.setText(Const.NVL(in.getTargetUrl(), ""));
    wUserName.setText(Const.NVL(in.getUsername(), ""));
    wPassword.setText(Const.NVL(in.getPassword(), ""));
    wBatchSize.setText(in.getBatchSize());
    wModule.setText(Const.NVL(in.getModule(), "Account"));
    wUpsertField.setText(Const.NVL(in.getUpsertField(), "Id"));
    wBatchSize.setText("" + in.getBatchSize());
    wSalesforceIDFieldName.setText(Const.NVL(in.getSalesforceIDFieldName(), ""));
    if (isDebug()) {
      logDebug(BaseMessages.getString(PKG, "SalesforceUpsertDialog.Log.GettingFieldsInfo"));
    }

    if (input.getUpdateLookup() != null) {
      for (int i = 0; i < input.getUpdateLookup().length; i++) {
        TableItem item = wReturn.table.getItem(i);
        if (input.getUpdateLookup()[i] != null) {
          item.setText(1, input.getUpdateLookup()[i]);
        }
        if (input.getUpdateStream()[i] != null) {
          item.setText(2, input.getUpdateStream()[i]);
        }
        if (input.getUseExternalId()[i] == null || input.getUseExternalId()[i].booleanValue()) {
          item.setText(3, "Y");
        } else {
          item.setText(3, "N");
        }
      }
    }

    wReturn.removeEmptyRows();
    wReturn.setRowNums();
    wReturn.optWidth(true);
    wTimeOut.setText(Const.NVL(in.getTimeout(), SalesforceConnectionUtils.DEFAULT_TIMEOUT));
    wUseCompression.setSelection(in.isCompression());
    wRollbackAllChangesOnError.setSelection(in.isRollbackAllChangesOnError());

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void ok() {
    try {
      getInfo(input);
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.ErrorValidateData.DialogTitle"),
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.ErrorValidateData.DialogMessage"),
          e);
    }
    dispose();
  }

  @Override
  protected void getInfo(SalesforceTransformMeta in) throws HopException {
    SalesforceUpsertMeta meta = (SalesforceUpsertMeta) in;
    transformName = wTransformName.getText(); // return value

    // copy info to SalesforceUpsertMeta class (input)
    meta.setTargetUrl(Const.NVL(wURL.getText(), SalesforceConnectionUtils.TARGET_DEFAULT_URL));
    meta.setUsername(wUserName.getText());
    meta.setPassword(wPassword.getText());
    meta.setModule(Const.NVL(wModule.getText(), "Account"));
    meta.setUpsertField(Const.NVL(wUpsertField.getText(), "Id"));
    meta.setSalesforceIDFieldName(wSalesforceIDFieldName.getText());
    meta.setBatchSize(wBatchSize.getText());

    int nrFields = wReturn.nrNonEmpty();

    meta.allocate(nrFields);

    // CHECKSTYLE:Indentation:OFF
    for (int i = 0; i < nrFields; i++) {
      TableItem item = wReturn.getNonEmpty(i);
      meta.getUpdateLookup()[i] = item.getText(1);
      meta.getUpdateStream()[i] = item.getText(2);
      meta.getUseExternalId()[i] = Boolean.valueOf("Y".equals(item.getText(3)));
    }
    meta.setCompression(wUseCompression.getSelection());
    meta.setTimeout(Const.NVL(wTimeOut.getText(), "0"));
    meta.setRollbackAllChangesOnError(wRollbackAllChangesOnError.getSelection());
  }

  // check if module, username is given
  private boolean checkInput() {
    if (Utils.isEmpty(wModule.getText())) {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setMessage(
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.ModuleMissing.DialogMessage"));
      mb.setText(BaseMessages.getString(PKG, "System.Dialog.Error.Title"));
      mb.open();
      return false;
    }
    return checkUser();
  }

  // check if module, username is given
  private boolean checkUser() {

    if (Utils.isEmpty(wUserName.getText())) {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setMessage(
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.UsernameMissing.DialogMessage"));
      mb.setText(BaseMessages.getString(PKG, "System.Dialog.Error.Title"));
      mb.open();
      return false;
    }

    return true;
  }

  private String[] getModuleFields() throws HopException {
    if (moduleFields != null) {
      return moduleFields;
    } else if (skipFetchModules() || Utils.isEmpty(wModule.getText())) {
      getModulesListError = false;
      return new String[0];
    }

    getModulesListError = true;
    SalesforceUpsertMeta meta = new SalesforceUpsertMeta();
    getInfo(meta);
    String url = variables.resolve(meta.getTargetUrl());
    String selectedModule = variables.resolve(meta.getModule());
    // Define a new Salesforce connection
    SalesforceConnection connection =
        new SalesforceConnection(
            log,
            url,
            variables.resolve(meta.getUsername()),
            Utils.resolvePassword(variables, meta.getPassword()));
    int realTimeOut = Const.toInt(variables.resolve(meta.getTimeout()), 0);
    connection.setTimeOut(realTimeOut);
    Cursor busy = new Cursor(shell.getDisplay(), SWT.CURSOR_WAIT);
    try {
      shell.setCursor(busy);

      // connect to Salesforce
      connection.connect();
      boolean excludeNonUpdatableFields = true;
      moduleFields = connection.getFields(selectedModule, excludeNonUpdatableFields);
      getModulesListError = false;
      return moduleFields;
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (Exception e) {
          /* Ignore */
        }
      }
      shell.setCursor(null);
      busy.dispose();
    }
  }

  private boolean skipFetchModules() {
    return Utils.isEmpty(wURL.getText())
        || Utils.isEmpty(wUserName.getText())
        || Utils.isEmpty(wPassword.getText())
        || getModulesListError;
  }

  /**
   * Reads in the fields from the previous transforms and from the ONE next transform and opens an
   * EnterMappingDialog with this information. After the user did the mapping, those information is
   * put into the Select/Rename table.
   */
  private void generateMappings() {
    if (!checkInput()) {
      return;
    }
    // Determine the source and target fields...
    //
    IRowMeta sourceFields;
    IRowMeta targetFields = new RowMeta();

    try {
      sourceFields = pipelineMeta.getPrevTransformFields(variables, transformMeta);
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(
              PKG, "SalesforceUpsertDialog.DoMapping.UnableToFindSourceFields.Title"),
          BaseMessages.getString(
              PKG, "SalesforceUpsertDialog.DoMapping.UnableToFindSourceFields.Message"),
          e);
      return;
    }

    try {

      String[] fields = getModuleFields();
      for (int i = 0; i < fields.length; i++) {
        targetFields.addValueMeta(new ValueMetaNone(fields[i]));
      }
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(
              PKG, "SalesforceUpsertDialog.DoMapping.UnableToFindTargetFields.Title"),
          BaseMessages.getString(
              PKG, "SalesforceUpsertDialog.DoMapping.UnableToFindTargetFields.Message"),
          e);
      return;
    }

    String[] inputNames = new String[sourceFields.size()];
    for (int i = 0; i < sourceFields.size(); i++) {
      IValueMeta value = sourceFields.getValueMeta(i);
      inputNames[i] = value.getName();
    }

    // Create the existing mapping list...
    //
    List<SourceToTargetMapping> mappings = new ArrayList<>();
    StringBuffer missingSourceFields = new StringBuffer();
    StringBuffer missingTargetFields = new StringBuffer();

    int nrFields = wReturn.nrNonEmpty();
    for (int i = 0; i < nrFields; i++) {
      TableItem item = wReturn.getNonEmpty(i);
      String source = item.getText(2);
      String target = item.getText(1);

      int sourceIndex = sourceFields.indexOfValue(source);
      if (sourceIndex < 0) {
        missingSourceFields.append(Const.CR + "   " + source + " --> " + target);
      }
      int targetIndex = targetFields.indexOfValue(target);
      if (targetIndex < 0) {
        missingTargetFields.append(Const.CR + "   " + source + " --> " + target);
      }
      if (sourceIndex < 0 || targetIndex < 0) {
        continue;
      }

      SourceToTargetMapping mapping = new SourceToTargetMapping(sourceIndex, targetIndex);
      mappings.add(mapping);
    }

    // show a confirm dialog if some missing field was found
    //
    if (missingSourceFields.length() > 0 || missingTargetFields.length() > 0) {

      String message = "";
      if (missingSourceFields.length() > 0) {
        message +=
            BaseMessages.getString(
                    PKG,
                    "SalesforceUpsertDialog.DoMapping.SomeSourceFieldsNotFound",
                    missingSourceFields.toString())
                + Const.CR;
      }
      if (missingTargetFields.length() > 0) {
        message +=
            BaseMessages.getString(
                    PKG,
                    "SalesforceUpsertDialog.DoMapping.SomeTargetFieldsNotFound",
                    missingSourceFields.toString())
                + Const.CR;
      }
      message += Const.CR;
      message +=
          BaseMessages.getString(PKG, "SalesforceUpsertDialog.DoMapping.SomeFieldsNotFoundContinue")
              + Const.CR;
      int answer =
          BaseDialog.openMessageBox(
              shell,
              BaseMessages.getString(
                  PKG, "SalesforceUpsertDialog.DoMapping.SomeFieldsNotFoundTitle"),
              message,
              SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
      boolean goOn = (answer & SWT.OK) != 0;
      if (!goOn) {
        return;
      }
    }
    EnterMappingDialog d =
        new EnterMappingDialog(
            SalesforceUpsertDialog.this.shell,
            sourceFields.getFieldNames(),
            targetFields.getFieldNames(),
            mappings);
    mappings = d.open();

    // mappings == null if the user pressed cancel
    //
    if (mappings != null) {
      // Clear and re-populate!
      //
      wReturn.table.removeAll();
      wReturn.table.setItemCount(mappings.size());
      for (int i = 0; i < mappings.size(); i++) {
        SourceToTargetMapping mapping = mappings.get(i);
        TableItem item = wReturn.table.getItem(i);
        item.setText(2, sourceFields.getValueMeta(mapping.getSourcePosition()).getName());
        item.setText(1, targetFields.getValueMeta(mapping.getTargetPosition()).getName());
      }
      wReturn.setRowNums();
      wReturn.optWidth(true);
    }
  }

  protected void setComboBoxes() {
    // Something was changed in the row.
    //
    final Map<String, Integer> fields = new HashMap<>();

    // Add the currentMeta fields...
    fields.putAll(inputFields);

    Set<String> keySet = fields.keySet();
    List<String> entries = new ArrayList<>(keySet);

    String[] fieldNames = entries.toArray(new String[entries.size()]);
    if ( PropsUi.getInstance().isSortFieldByName() ) {
      Const.sortStrings(fieldNames);
    }
    // return fields
    ciReturn[1].setComboValues(fieldNames);
  }

  private void getModulesList() {
    if (!gotModule) {
      SalesforceConnection connection = null;

      try {
        SalesforceUpsertMeta meta = new SalesforceUpsertMeta();
        getInfo(meta);
        String url = variables.resolve(meta.getTargetUrl());

        String selectedField = wModule.getText();
        wModule.removeAll();

        // Define a new Salesforce connection
        connection =
            new SalesforceConnection(
                log,
                url,
                variables.resolve(meta.getUsername()),
                Utils.resolvePassword(variables, meta.getPassword()));
        // connect to Salesforce
        connection.connect();
        // return
        wModule.setItems(connection.getAllAvailableObjects(false));

        if (!Utils.isEmpty(selectedField)) {
          wModule.setText(selectedField);
        }

        gotModule = true;
        getModulesListError = false;

      } catch (Exception e) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "SalesforceUpsertDialog.ErrorRetrieveModules.DialogTitle"),
            BaseMessages.getString(
                PKG, "SalesforceUpsertDialog.ErrorRetrieveData.ErrorRetrieveModules"),
            e);
        getModulesListError = true;
      } finally {
        if (connection != null) {
          try {
            connection.close();
          } catch (Exception e) {
            /* Ignore */
          }
        }
      }
    }
  }

  public void setModuleFieldCombo() {
    // clear
    for (int i = 0; i < tableFieldColumns.size(); i++) {
      ColumnInfo colInfo = tableFieldColumns.get(i);
      colInfo.setComboValues(new String[] {});
    }
    String selectedModule = variables.resolve(wModule.getText());
    if (!Utils.isEmpty(selectedModule)) {
      try {
        // loop through the objects and find build the list of fields
        String[] fieldsName = getModuleFields();

        if (fieldsName != null) {
          for (int i = 0; i < tableFieldColumns.size(); i++) {
            ColumnInfo colInfo = tableFieldColumns.get(i);
            colInfo.setComboValues(fieldsName);
          }
        }
      } catch (Exception e) {
        for (int i = 0; i < tableFieldColumns.size(); i++) {
          ColumnInfo colInfo = tableFieldColumns.get(i);
          colInfo.setComboValues(new String[] {});
        }
        // ignore any errors here. drop downs will not be
        // filled, but no problem for the user
      }
    }
  }
}
