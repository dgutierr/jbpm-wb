/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.workbench.ht.client.editors.taskslist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.jbpm.workbench.common.client.list.AbstractMultiGridView;
import org.jbpm.workbench.common.client.list.ExtendedPagedTable;
import org.jbpm.workbench.common.client.resources.CommonResources;
import org.jbpm.workbench.common.client.util.ButtonActionCell;
import org.jbpm.workbench.common.client.util.DateUtils;
import org.jbpm.workbench.common.client.util.TaskUtils;
import org.jbpm.workbench.df.client.filter.FilterSettings;
import org.jbpm.workbench.df.client.filter.FilterSettingsBuilderHelper;
import org.jbpm.workbench.df.client.list.base.DataSetEditorManager;
import org.jbpm.workbench.ht.client.resources.HumanTaskResources;
import org.jbpm.workbench.ht.client.resources.i18n.Constants;
import org.jbpm.workbench.ht.model.TaskSummary;
import org.uberfire.ext.services.shared.preferences.GridColumnPreference;
import org.uberfire.ext.services.shared.preferences.GridGlobalPreferences;
import org.uberfire.ext.widgets.common.client.tables.popup.NewTabFilterPopup;
import org.uberfire.ext.widgets.table.client.ColumnMeta;
import org.uberfire.mvp.Command;

import static org.dashbuilder.dataset.filter.FilterFactory.*;
import static org.dashbuilder.dataset.sort.SortOrder.*;
import static org.jbpm.workbench.ht.model.TaskDataSetConstants.*;

@Dependent
public class TaskListViewImpl extends AbstractMultiGridView<TaskSummary, TaskListPresenter>
        implements AbstractTaskListPresenter.TaskListView<TaskListPresenter> {

    public static final String DATA_SET_TASK_LIST_PREFIX = "DataSetTaskListGrid";
    public static final String COL_ID_ACTIONS = "actions";
    private static final String TAB_ADMIN = DATA_SET_TASK_LIST_PREFIX + "_4";
    private static final String TAB_ALL = DATA_SET_TASK_LIST_PREFIX + "_3";
    private static final String TAB_GROUP = DATA_SET_TASK_LIST_PREFIX + "_2";
    private static final String TAB_PERSONAL = DATA_SET_TASK_LIST_PREFIX + "_1";
    private static final String TAB_ACTIVE = DATA_SET_TASK_LIST_PREFIX + "_0";

    private final Constants constants = Constants.INSTANCE;

    @Inject
    private DataSetEditorManager dataSetEditorManager;

    @Override
    public void init( final TaskListPresenter presenter ) {
        final List<String> bannedColumns = new ArrayList<String>();
        bannedColumns.add( COLUMN_NAME );
        bannedColumns.add( COL_ID_ACTIONS );
        final List<String> initColumns = new ArrayList<String>();
        initColumns.add( COLUMN_NAME );
        initColumns.add( COLUMN_PROCESS_ID );
        initColumns.add( COLUMN_STATUS );
        initColumns.add( COLUMN_CREATED_ON );
        initColumns.add( COL_ID_ACTIONS );
        final Button button = GWT.create(Button.class);
        button.setIcon( IconType.PLUS );
        button.setSize( ButtonSize.SMALL );
        button.addClickHandler( new ClickHandler() {
            public void onClick( ClickEvent event ) {
                final String key = getValidKeyForAdditionalListGrid( DATA_SET_TASK_LIST_PREFIX + "_" );

                Command addNewGrid = new Command() {
                    @Override
                    public void execute() {

                        final ExtendedPagedTable<TaskSummary> extendedPagedTable = createGridInstance( new GridGlobalPreferences( key, initColumns, bannedColumns ), key );

                        extendedPagedTable.setDataProvider( presenter.getDataProvider() );

                        filterPagedTable.createNewTab( extendedPagedTable, key, button, new Command() {
                            @Override
                            public void execute() {
                                currentListGrid = extendedPagedTable;
                                applyFilterOnPresenter( key );
                            }
                        } );
                        applyFilterOnPresenter( key );

                    }
                };
                FilterSettings tableSettings = createTableSettingsPrototype();
                tableSettings.setKey( key );
                dataSetEditorManager.showTableSettingsEditor( filterPagedTable, Constants.INSTANCE.New_FilteredList(), tableSettings, addNewGrid );

            }
        } );
        super.init( presenter, new GridGlobalPreferences( DATA_SET_TASK_LIST_PREFIX, initColumns, bannedColumns ), button );
    }

    public void initSelectionModel() {
        final ExtendedPagedTable<TaskSummary> extendedPagedTable = getListGrid();
        selectedStyles = new RowStyles<TaskSummary>() {

            @Override
            public String getStyleNames( TaskSummary row,
                                         int rowIndex ) {
                if ( rowIndex == selectedRow ) {
                    return CommonResources.INSTANCE.css().selected();
                } else {
                    if ( row.getStatus().equals( "InProgress" ) || row.getStatus().equals( "Ready" ) ) {
                        switch (row.getPriority()) {
                            case 5:
                                return HumanTaskResources.INSTANCE.css().taskPriorityFive();
                            case 4:
                                return HumanTaskResources.INSTANCE.css().taskPriorityFour();
                            case 3:
                                return HumanTaskResources.INSTANCE.css().taskPriorityThree();
                            case 2:
                                return HumanTaskResources.INSTANCE.css().taskPriorityTwo();
                            case 1:
                                return HumanTaskResources.INSTANCE.css().taskPriorityOne();
                            default:
                                return "";
                        }
                    } else if ( row.getStatus().equals( "Completed" ) ) {
                        return HumanTaskResources.INSTANCE.css().taskCompleted();
                    }

                }
                return null;
            }
        };

        extendedPagedTable.setEmptyTableCaption( constants.No_Tasks_Found() );

        selectionModel = new NoSelectionModel<TaskSummary>();
        selectionModel.addSelectionChangeHandler( new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange( SelectionChangeEvent event ) {
                boolean close = false;
                if ( selectedRow == -1 ) {
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.setRowStyles( selectedStyles );
                    extendedPagedTable.redraw();

                } else if ( extendedPagedTable.getKeyboardSelectedRow() != selectedRow ) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();
                } else {
                    close = true;
                }

                selectedItem = selectionModel.getLastSelectedObject();

                presenter.selectTask(selectedItem, close);
            }
        } );

        noActionColumnManager = DefaultSelectionEventManager
                .createCustomManager( new DefaultSelectionEventManager.EventTranslator<TaskSummary>() {

                    @Override
                    public boolean clearCurrentSelection( CellPreviewEvent<TaskSummary> event ) {
                        return false;
                    }

                    @Override
                    public DefaultSelectionEventManager.SelectAction translateSelectionEvent( CellPreviewEvent<TaskSummary> event ) {
                        NativeEvent nativeEvent = event.getNativeEvent();
                        if ( BrowserEvents.CLICK.equals( nativeEvent.getType() ) &&
                            // Ignore if the event didn't occur in the correct column.
                            extendedPagedTable.getColumnIndex( actionsColumn ) == event.getColumn() ) {
                                return DefaultSelectionEventManager.SelectAction.IGNORE;
                        }
                        return DefaultSelectionEventManager.SelectAction.DEFAULT;
                    }
                } );
        extendedPagedTable.setSelectionModel( selectionModel, noActionColumnManager );
        extendedPagedTable.setRowStyles( selectedStyles );
    }

    @Override
    public void initColumns( ExtendedPagedTable extendedPagedTable ) {
        initCellPreview( extendedPagedTable );

        actionsColumn = initActionsColumn();

        final List<ColumnMeta<TaskSummary>> columnMetas = new ArrayList<ColumnMeta<TaskSummary>>();

        columnMetas.add(new ColumnMeta<>(createNumberColumn(COLUMN_TASK_ID,
                                                            task -> task.getTaskId()),
                                         constants.Id()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_NAME,
                                                          task -> task.getTaskName()),
                                         constants.Task()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_DESCRIPTION,
                                                          task -> task.getDescription()),
                                         constants.Description()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_PROCESS_ID,
                                                          task -> task.getProcessId()),
                                         constants.Process_Name()));
        columnMetas.add(new ColumnMeta<>(createNumberColumn(COLUMN_PROCESS_INSTANCE_ID,
                                                            task -> task.getProcessInstanceId()),
                                         constants.Process_Id()));
        columnMetas.add(new ColumnMeta<>(createNumberColumn(COLUMN_PRIORITY,
                                                            task -> task.getPriority()),
                                         constants.Priority()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_STATUS,
                                                          task -> task.getStatus()),
                                         constants.Status()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_CREATED_ON,
                                                          task -> DateUtils.getDateTimeStr(task.getCreatedOn())),
                                         constants.Created_On()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_DUE_DATE,
                                                          task -> DateUtils.getDateTimeStr(task.getExpirationTime())),
                                         constants.Due_On()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_ACTUAL_OWNER,
                                                          task -> task.getActualOwner()),
                                         constants.Actual_Owner()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_PROCESS_INSTANCE_CORRELATION_KEY,
                                                          task -> task.getProcessInstanceCorrelationKey()),
                                         constants.Process_Instance_Correlation_Key()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_PROCESS_INSTANCE_DESCRIPTION,
                                                          task -> task.getProcessInstanceDescription()),
                                         constants.Process_Instance_Description()));
        columnMetas.add(new ColumnMeta<>(createTextColumn(COLUMN_LAST_MODIFICATION_DATE,
                                                          task -> DateUtils.getDateTimeStr(task.getLastModificationDate())),
                                         constants.Last_Modification_Date()));
        columnMetas.add(new ColumnMeta<>(actionsColumn,
                                         constants.Actions()));

        List<GridColumnPreference> columPreferenceList = extendedPagedTable.getGridPreferencesStore().getColumnPreferences();

        for ( GridColumnPreference colPref : columPreferenceList ) {
            if ( !isColumnAdded( columnMetas, colPref.getName() ) ) {
                Column genericColumn = initGenericColumn( colPref.getName() );
                genericColumn.setSortable( false );
                columnMetas.add( new ColumnMeta<TaskSummary>( genericColumn, colPref.getName(), true, true ) );
            }
        }

        extendedPagedTable.addColumns( columnMetas );
    }

    private void initCellPreview( final ExtendedPagedTable extendedPagedTable ) {
        extendedPagedTable.addCellPreviewHandler( new CellPreviewEvent.Handler<TaskSummary>() {

            @Override
            public void onCellPreview( final CellPreviewEvent<TaskSummary> event ) {

                if ( BrowserEvents.MOUSEOVER.equalsIgnoreCase( event.getNativeEvent().getType() ) ) {
                    onMouseOverGrid( extendedPagedTable, event );
                }

            }
        } );

    }

    private void onMouseOverGrid( ExtendedPagedTable extendedPagedTable,
                                  final CellPreviewEvent<TaskSummary> event ) {
        TaskSummary task = event.getValue();

        if ( task.getDescription() != null ) {
            extendedPagedTable.setTooltip( extendedPagedTable.getKeyboardSelectedRow(), event.getColumn(), task.getDescription() );
        }
    }

    private Column initActionsColumn() {
        List<HasCell<TaskSummary, ?>> cells = new LinkedList<HasCell<TaskSummary, ?>>();
        cells.add( new ClaimActionHasCell( constants.Claim(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( final TaskSummary task ) {
                presenter.claimTask( task );
            }
        } ) );

        cells.add( new ReleaseActionHasCell( constants.Release(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( final TaskSummary task ) {
                presenter.releaseTask( task );
            }
        } ) );

        cells.add( new CompleteActionHasCell( constants.Open(), new ActionCell.Delegate<TaskSummary>() {
            @Override
            public void execute( final TaskSummary task ) {
                selectedRow = -1;
                presenter.selectTask(task, false);
            }
        } ) );

        CompositeCell<TaskSummary> cell = new CompositeCell<TaskSummary>( cells );
        Column<TaskSummary, TaskSummary> actionsColumn = new Column<TaskSummary, TaskSummary>( cell ) {
            @Override
            public TaskSummary getValue( TaskSummary object ) {
                return object;
            }
        };
        actionsColumn.setDataStoreName( COL_ID_ACTIONS );
        return actionsColumn;

    }

    protected class CompleteActionHasCell extends ButtonActionCell<TaskSummary> {

        public CompleteActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getActualOwner() != null && value.getStatus().equals( "InProgress" ) ) {
                super.render( context, value, sb );
            }
        }
    }

    protected class ClaimActionHasCell extends ButtonActionCell<TaskSummary> {

        public ClaimActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getStatus().equals( "Ready" ) ) {
                super.render( context, value, sb );
            }
        }
    }

    protected class ReleaseActionHasCell extends ButtonActionCell<TaskSummary> {

        public ReleaseActionHasCell( final String text, final ActionCell.Delegate<TaskSummary> delegate ) {
            super( text, delegate );
        }

        @Override
        public void render( final Cell.Context context, final TaskSummary value, final SafeHtmlBuilder sb ) {
            if ( value.getActualOwner() != null && value.getActualOwner().equals( identity.getIdentifier() )
                    && ( value.getStatus().equals( "Reserved" ) || value.getStatus().equals( "InProgress" ) ) ) {
                super.render( context, value, sb );
            }
        }
    }

    @Override
    public void initDefaultFilters( GridGlobalPreferences preferences,
                                    Button createTabButton ) {

        presenter.setAddingDefaultFilters( true );

        //Filter status Active
        initOwnTabFilter(preferences,
                         TAB_ACTIVE,
                         Constants.INSTANCE.Active(),
                         Constants.INSTANCE.FilterActive(),
                         TaskUtils.getStatusByType(TaskUtils.TaskType.ACTIVE));

        //Filter status Personal
        initPersonalTabFilter(preferences,
                              TAB_PERSONAL,
                              Constants.INSTANCE.Personal(),
                              Constants.INSTANCE.FilterPersonal(),
                              TaskUtils.getStatusByType(TaskUtils.TaskType.PERSONAL));

        //Filter status Group
        initGroupTabFilter(preferences,
                           TAB_GROUP,
                           Constants.INSTANCE.Group(),
                           Constants.INSTANCE.FilterGroup(),
                           TaskUtils.getStatusByType(TaskUtils.TaskType.GROUP));

        //Filter status All
        initOwnTabFilter(preferences,
                         TAB_ALL,
                         Constants.INSTANCE.All(),
                         Constants.INSTANCE.FilterAll(),
                         TaskUtils.getStatusByType(TaskUtils.TaskType.ALL));

        //Filter status Admin
        initAdminTabFilter(preferences,
                           TAB_ADMIN,
                           Constants.INSTANCE.Task_Admin(),
                           Constants.INSTANCE.FilterTaskAdmin(),
                           TaskUtils.getStatusByType(TaskUtils.TaskType.ADMIN));

        filterPagedTable.addAddTableButton( createTabButton );
        selectFirstTabAndEnableQueries(TAB_ACTIVE);
    }

    private void initGroupTabFilter( GridGlobalPreferences preferences,
                                     final String key,
                                     String tabName,
                                     String tabDesc,
                                     List<String> states ) {
        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset(HUMAN_TASKS_WITH_USER_DATASET);
        List<Comparable> names = new ArrayList<>(states);
        builder.filter( COLUMN_STATUS, equalsTo( COLUMN_STATUS, names ) );

        builder.filter(COLUMN_ACTUAL_OWNER, OR(equalsTo(""), isNull()) );

        builder.group(COLUMN_TASK_ID);

        addCommonColumnSettings(builder);

        initFilterTab(builder , key, tabName, tabDesc, preferences);
    }

    private void initAdminTabFilter( GridGlobalPreferences preferences,
                                     final String key,
                                     String tabName,
                                     String tabDesc,
                                     List<String> states ) {
        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset(HUMAN_TASKS_WITH_ADMIN_DATASET);
        List<Comparable> names = new ArrayList<>(states);
        builder.filter( COLUMN_STATUS, equalsTo( COLUMN_STATUS, names ) );

        builder.group(COLUMN_TASK_ID);

        addCommonColumnSettings(builder);

        initFilterTab(builder, key, tabName, tabDesc, preferences );
    }

    private void initPersonalTabFilter( GridGlobalPreferences preferences,
                                        final String key,
                                        String tabName,
                                        String tabDesc,
                                        List<String> states ) {

        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset( HUMAN_TASKS_DATASET );
        List<Comparable> names = new ArrayList<>(states);
        builder.filter( equalsTo( COLUMN_STATUS, names ) );
        builder.filter( equalsTo(COLUMN_ACTUAL_OWNER, identity.getIdentifier() ) );

        addCommonColumnSettings(builder);

        initFilterTab(builder, key, tabName, tabDesc, preferences );
    }

    private void initOwnTabFilter( GridGlobalPreferences preferences,
                                   final String key,
                                   String tabName,
                                   String tabDesc,
                                   List<String> states ) {
        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset(HUMAN_TASKS_WITH_USER_DATASET);
        List<Comparable> names = new ArrayList<>(states);
        builder.filter( COLUMN_STATUS, equalsTo( COLUMN_STATUS, names ) );

        builder.group(COLUMN_TASK_ID);

        addCommonColumnSettings(builder);

        initFilterTab(builder, key, tabName, tabDesc, preferences );
    }

    private void addCommonColumnSettings(FilterSettingsBuilderHelper builder) {
        builder.setColumn(COLUMN_ACTIVATION_TIME, constants.ActivationTime(), DateUtils.getDateTimeFormatMask());
        builder.setColumn(COLUMN_ACTUAL_OWNER, constants.Actual_Owner());
        builder.setColumn(COLUMN_CREATED_BY, constants.CreatedBy());
        builder.setColumn(COLUMN_CREATED_ON, constants.Created_On(), DateUtils.getDateTimeFormatMask());
        builder.setColumn(COLUMN_DEPLOYMENT_ID, constants.DeploymentId());
        builder.setColumn(COLUMN_DESCRIPTION, constants.Description());
        builder.setColumn(COLUMN_DUE_DATE, constants.DueDate(), DateUtils.getDateTimeFormatMask());
        builder.setColumn(COLUMN_NAME, constants.Task());
        builder.setColumn(COLUMN_PARENT_ID, constants.ParentId());
        builder.setColumn(COLUMN_PRIORITY, constants.Priority());
        builder.setColumn(COLUMN_PROCESS_ID, constants.Process_Id());
        builder.setColumn(COLUMN_PROCESS_INSTANCE_ID, constants.Process_Instance_Id());
        builder.setColumn(COLUMN_PROCESS_SESSION_ID, constants.ProcessSessionId());
        builder.setColumn(COLUMN_STATUS, constants.Status());
        builder.setColumn(COLUMN_TASK_ID, constants.Id());
        builder.setColumn(COLUMN_WORK_ITEM_ID, constants.WorkItemId());
        builder.setColumn(COLUMN_LAST_MODIFICATION_DATE, constants.Last_Modification_Date());
        builder.setColumn(COLUMN_PROCESS_INSTANCE_CORRELATION_KEY, constants.Process_Instance_Correlation_Key());
        builder.setColumn(COLUMN_PROCESS_INSTANCE_DESCRIPTION, constants.Process_Instance_Description());

        builder.filterOn(true, true, true);
        builder.tableOrderEnabled(true);
        builder.tableOrderDefault(COLUMN_CREATED_ON, DESCENDING);
    }

    private void initFilterTab(FilterSettingsBuilderHelper builder, final String key, String tabName, String tabDesc, GridGlobalPreferences preferences) {
        FilterSettings tableSettings = builder.buildSettings();
        tableSettings.setKey(key);
        tableSettings.setTableName(tabName);
        tableSettings.setTableDescription(tabDesc);
        tableSettings.setUUID(tableSettings.getDataSetLookup().getDataSetUUID());

        HashMap<String, Object> tabSettingsValues = new HashMap<String, Object>();

        tabSettingsValues.put(FILTER_TABLE_SETTINGS, dataSetEditorManager.getTableSettingsToStr(tableSettings));
        tabSettingsValues.put(NewTabFilterPopup.FILTER_TAB_NAME_PARAM, tableSettings.getTableName());
        tabSettingsValues.put(NewTabFilterPopup.FILTER_TAB_DESC_PARAM, tableSettings.getTableDescription());

        filterPagedTable.saveNewTabSettings(key, tabSettingsValues);

        final ExtendedPagedTable<TaskSummary> extendedPagedTable = createGridInstance(new GridGlobalPreferences(key, preferences.getInitialColumns(), preferences.getBannedColumns()), key);
        currentListGrid = extendedPagedTable;
        extendedPagedTable.setDataProvider(presenter.getDataProvider());

        filterPagedTable.addTab(extendedPagedTable, key, new Command() {
            @Override
            public void execute() {
                currentListGrid = extendedPagedTable;
                applyFilterOnPresenter(key);
            }
        });
    }

    /*-------------------------------------------------*/
    /*---              DashBuilder                   --*/
    /*-------------------------------------------------*/

    public FilterSettings createTableSettingsPrototype() {
        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset(HUMAN_TASKS_WITH_USER_DATASET);
        builder.group(COLUMN_TASK_ID);

        addCommonColumnSettings(builder);

        final FilterSettings filterSettings = builder.buildSettings();
        filterSettings.setUUID(HUMAN_TASKS_WITH_USER_DATASET);
        return filterSettings;
    }

    private boolean isColumnAdded( List<ColumnMeta<TaskSummary>> columnMetas,
            String caption ) {
        if ( caption != null ) {
            for ( ColumnMeta<TaskSummary> colMet : columnMetas ) {
                if ( caption.equals( colMet.getColumn().getDataStoreName() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addDomainSpecifColumns( ExtendedPagedTable<TaskSummary> extendedPagedTable,
            Set<String> columns ) {

        extendedPagedTable.storeColumnToPreferences();

        HashMap modifiedCaptions = new HashMap<String, String>();
        ArrayList<ColumnMeta> existingExtraColumns = new ArrayList<ColumnMeta>();
        for ( ColumnMeta<TaskSummary> cm : extendedPagedTable.getColumnMetaList() ) {
            if ( cm.isExtraColumn() ) {
                existingExtraColumns.add( cm );
            } else if ( columns.contains( cm.getCaption() ) ) {      //exist a column with the same caption
                for ( String c : columns ) {
                    if ( c.equals( cm.getCaption() ) ) {
                        modifiedCaptions.put( c, "Var_" + c );
                    }
                }
            }
        }
        for ( ColumnMeta colMet : existingExtraColumns ) {
            if ( !columns.contains( colMet.getCaption() ) ) {
                extendedPagedTable.removeColumnMeta( colMet );
            } else {
                columns.remove( colMet.getCaption() );
            }
        }

        List<ColumnMeta<TaskSummary>> columnMetas = new ArrayList<ColumnMeta<TaskSummary>>();
        String caption = "";
        for ( String c : columns ) {
            caption = c;
            if ( modifiedCaptions.get( c ) != null ) {
                caption = (String) modifiedCaptions.get( c );
            }
            Column genericColumn = initGenericColumn( c );
            genericColumn.setSortable( false );

            columnMetas.add( new ColumnMeta<TaskSummary>( genericColumn, caption, true, true ) );
        }

        extendedPagedTable.addColumns( columnMetas );
    }


    @Override
    public FilterSettings getVariablesTableSettings( String taskName ) {
        FilterSettingsBuilderHelper builder = FilterSettingsBuilderHelper.init();
        builder.initBuilder();

        builder.dataset(HUMAN_TASKS_WITH_VARIABLES_DATASET);
        builder.filter(equalsTo(COLUMN_TASK_VARIABLE_TASK_NAME, taskName));

        builder.filterOn(true, true, true);
        builder.tableOrderEnabled(true);
        builder.tableOrderDefault(COLUMN_TASK_ID, ASCENDING);

        FilterSettings varTableSettings =builder.buildSettings();
        varTableSettings.setTablePageSize(-1);
        varTableSettings.setUUID(HUMAN_TASKS_WITH_VARIABLES_DATASET);

        return varTableSettings;
    }

    private Column initGenericColumn( final String key ) {

        Column<TaskSummary, String> genericColumn = new Column<TaskSummary, String>( new TextCell() ) {
            @Override
            public String getValue( TaskSummary object ) {
                return object.getDomainDataValue( key );
            }
        };
        genericColumn.setSortable(true);
        genericColumn.setDataStoreName(key);

        return genericColumn;
    }

    @Override
    public void resetDefaultFilterTitleAndDescription() {
        saveTabSettings(TAB_ACTIVE,
                        constants.Active(),
                        constants.FilterActive());
        saveTabSettings(TAB_PERSONAL,
                        constants.Personal(),
                        constants.FilterPersonal());
        saveTabSettings(TAB_GROUP,
                        constants.Group(),
                        constants.FilterGroup());
        saveTabSettings(TAB_ALL,
                        constants.All(),
                        constants.FilterAll());
        saveTabSettings(TAB_ADMIN,
                        constants.Task_Admin(),
                        constants.FilterTaskAdmin());
    }

    @Override
    public void setSelectedTask(final TaskSummary selectedTask) {
        selectionModel.setSelected( selectedTask, true );
    }

}