package org.bennedum.SQLite;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;
import android.os.Environment;
import android.widget.Toast;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;


@DesignerComponent(
    version = SQLite.VERSION,
    description = "SQLite database interface.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/small-icon.png"
)

@SimpleObject(external = true)
public class SQLite extends AndroidNonvisibleComponent implements Component {

    public static final int VERSION = 1;
    
    private static final String NAME = "SQLite";
    
    // Extension properties
    private boolean debugToast = false;
    private boolean debugAlert = false;
    private String dbName = "db.sqlite";
    private int dbVersion = 1;
    private boolean dbReadOnly = false;
    private boolean dbReturnColumnNames = false;
    
    private ComponentContainer container;
    private Context context;
    private boolean isRepl;
    private Exception exception = null;
    
    private DBHelper dbHelper = null;
    private SQLiteDatabase db = null;
    
    private class DBHelper extends SQLiteOpenHelper {
    
        public DBHelper(Context context) {
            super(context, dbName, null, dbVersion);
        }
        
        @Override
        public void onOpen(SQLiteDatabase db) {
            debug("Database opened");
            DatabaseOpened();
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            debug("Database created");
            DatabaseClosed();
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            debug("Database upgraded");
            DatabaseUpgrade(oldVersion, newVersion);
        }
    
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            debug("Database downgraded");
            DatabaseDowngrade(oldVersion, newVersion);
        }
    
    }
    
    
    public SQLite(ComponentContainer container) {
        super(container.$form());
        isRepl = form instanceof ReplForm;  // Note: form is defined in our superclass
        this.container = container;
        context = (Context)container.$context();
    }

    private void debug(final String message) {
        if (debugToast || debugAlert) {
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (debugToast)
                        Toast.makeText(context, NAME + ": " + message, Toast.LENGTH_SHORT).show();
                    if (debugAlert)
                        new Notifier(form).ShowAlert(NAME + ": " + message);                    
                }
            });
        }
    }
    
    private void debugException(final Exception e) {
        exception = e;
        if (e != null) {
            debug(e.getMessage());
            if (e instanceof SQLException)
                SQLError(e.getMessage());
        }
    }
    
    
    /**
    * Display debugging messages as toast messages.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies whether debug messages should be displayed as Toast messages."
                    )
    public boolean DebugToast() {
        return debugToast;
    }
  
    /**
    * Display debugging messages as toast messages.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false"
                      )
    @SimpleProperty
    public void DebugToast(boolean debugToast) {
        this.debugToast = debugToast;
    }
    
    /**
    * Display debugging messages as alerts.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies whether debug messages should be displayed as alerts."
                    )
    public boolean DebugAlert() {
        return debugAlert;
    }
  
    /**
    * Display debugging messages as alerts.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false"
                      )
    @SimpleProperty
    public void DebugAlert(boolean debugAlert) {
        this.debugAlert = debugAlert;
    }
    
    /**
    * Name of the database.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies the name of the database."
                    )
    public String DBName() {
        return dbName;
    }
  
    /**
    * Name of the database.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
                      defaultValue = "db.sqlite"
                      )
    @SimpleProperty
    public void DBName(String dbName) {
        this.dbName = dbName;
    }
    
    /**
    * Version of the database.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specified the version of the database."
                    )
    public int DBVersion() {
        return dbVersion;
    }
  
    /**
    * Version of the database.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER,
                      defaultValue = "1"
                      )
    @SimpleProperty
    public void DBVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }
    
    /**
    * Mode of the database.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies whether the database will be read only or read/write."
                    )
    public boolean DBReadOnly() {
        return dbReadOnly;
    }
  
    /**
    * Mode of the database.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false"
                      )
    @SimpleProperty
    public void DBReadOnly(boolean dbReadOnly) {
        this.dbReadOnly = dbReadOnly;
    }

    /**
    * Should result lists contain column names.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies whether lists of results will contain column names. "
                                + "See the query blocks for more information."
                    )
    public boolean DBReturnColumnNames() {
        return dbReturnColumnNames;
    }
  
    /**
    * Should result lists contain column names.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false")
    @SimpleProperty
    public void DBReturnColumnNames(boolean dbReturnColumnNames) {
        this.dbReturnColumnNames = dbReturnColumnNames;
    }

    
    /**
    * Delete the database.
    */
    @SimpleFunction(description = "Deletes a closed database. "
                                + "This deletes the database file permanently."
                    )
    public void DeleteDatabase() {
        if (db != null)
            throw new YailRuntimeError("Unable to delete an open database.", NAME);
        context.deleteDatabase(dbName);
        debug("Database deleted");
    }
    
    
    /**
    * Opens the database.
    */
    @SimpleFunction(description = "Opens the database. "
                                + "If the database is already open, nothing happens."
                    )
    public void OpenDatabase() {
        if (db == null) {
            dbHelper = new DBHelper(context);
            if (dbReadOnly)
                db = dbHelper.getReadableDatabase();
            else
                db = dbHelper.getWritableDatabase();
        }
    }
            
    /**
    * Closes the database.
    */
    @SimpleFunction(description = "Closes the database. If the database is already closed, nothing happens. "
                                + "Any uncommited transactions will be rolled back."
                    )
    public void CloseDatabase() {
        if (db != null) {
            db.close();
            db = null;
            dbHelper = null;
        }
    }
    
    /**
    * Indicates if the database has been opened or not.
    */
    @SimpleFunction(description = "Returns true if the database is open, false otherwise.")
    public boolean IsDatabaseOpen() {
        return db != null;
    }
    
    /**
    * Returns the number of tables in the database.
    */
    @SimpleFunction(description = "Returns the number of tables in the database, or -1 if an error occurs or the database is not open.")
    public int TableCount() {
        if (db == null) return -1;
        try {
            String sql = "SELECT count(1) FROM sqlite_master WHERE type='table'";
            Cursor cursor = db.rawQuery(sql, null);
            cursor.moveToNext();
            int count = cursor.getInt(0);
            cursor.close();
            return count;
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
    
    /**
    * Returns a list of names of the tables in the the database.
    */
    @SimpleFunction(description = "Returns a list of names of the tables in the database, or an empty list if an error occurs or the database is not open.")
    public YailList TableNames() {
        if (db == null) return YailList.makeEmptyList();
        try {
            String sql = "SELECT name FROM sqlite_master WHERE type='table'";
            Cursor cursor = db.rawQuery(sql, null);
            ArrayList tables = new ArrayList();
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }
            cursor.close();
            return YailList.makeList(tables);
        } catch (SQLException e) {
            debugException(e);
            return YailList.makeEmptyList();
        }
    }
    
    /**
    * Returns true if the table exists in the database, false otherwise.
    */
    @SimpleFunction(description = "Returns true if the table exists in the database, or false if the table does not exist or an error occurs or the database is not open.")
    public boolean TableExists(String table) {
        if (db == null) return false;
        try {
            String sql = "SELECT count(1) FROM sqlite_master WHERE type='table' AND name='?'";
            Cursor cursor = db.rawQuery(sql, new String[] {table});
            int count = cursor.getInt(0);
            cursor.close();
            return count == 1;
        } catch (SQLException e) {
            debugException(e);
            return false;
        }
    }
    
    /**
    * Begin a transaction.
    */
    @SimpleFunction(description = "Begins a transaction on an open database. "
                                + "Nested transactions are supported."
                    )
    public void BeginTransaction() {
        if (db == null) return;
        db.beginTransaction();
        debug("Transaction started");
    }
    
    /**
    * Commit a transaction.
    */
    @SimpleFunction(description = "Commits the last open transaction.")
    public void CommitTransaction() {
        if (db == null) return;
        try {
            db.setTransactionSuccessful();
            db.endTransaction();
            debug("Transaction committed");
        } catch (SQLException e) {
            debugException(e);
        }
    }
    
    /**
    * Rollback a transaction.
    */
    @SimpleFunction(description = "Rolls back the last open transaction.")
    public void RollbackTransaction() {
        if (db == null) return;
        try {
            db.endTransaction();
            debug("Transaction rolled back");
        } catch (SQLException e) {
            debugException(e);
        }
    }
        
    /**
    * Returns the path to the database.
    */
    @SimpleFunction(description = "Returns the path to an open database.")
    public String DatabasePath() {
        if (db == null) return "";
        return db.getPath();
    }
    
    /**
    * Execute a single, raw SQL statement that is NOT a SELECT.
    * @param sql: The SQL statement
    * @return true if the statement was executed successfully, false otherwise
    */
    @SimpleFunction(description = "Execute a single, raw SQL statement that is NOT a SELECT and returns whether or not it succeeded. "
                                + "If the database is not open, false is returned."
                    )
    public boolean ExecRawSQL(final String sql) {
        if (db == null) return false;
        try {
            db.execSQL(sql);
            debug("Executed SQL: " + sql);
            return true;
        } catch (SQLException e) {
            debugException(e);
            return false;
        }
    }
    
    /**
    * Execute a single, raw SQL statement that is NOT a SELECT, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    */
    @SimpleFunction(description = "Execute a single, raw SQL statement that is NOT a SELECT asynchronously. "
                                + "The result of the operation is returned in the AfterExec event. "
                                + "See ExecRawSQL for more information." 
                    )
    public void ExecRawSQLAsync(final String tag, final String sql) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final boolean res = ExecRawSQL(sql);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExec(tag, res);
                    }
                });            
            }
        });
    }
    
    /**
    * Execute a single, parameterized SQL statement that is NOT a SELECT.
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    * @return true if the statements was executed successfully, false otherwise
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL statement that is NOT a SELECT and returns whether or not it succeeded. "
                                + "Each bind parameter replaces the corresponding '?' in WHERE clause in the query. "
                                + "If the database is not open, false is returned."
                    )
    public boolean ExecParamSQL(final String sql, final YailList bindParams) {
        if (db == null) return false;
        try {
            String[] params = bindParams.toStringArray();
            db.execSQL(sql, params);
            debug("Executed SQL: " + sql);
            return true;
        } catch (SQLException e) {
            debugException(e);
            return false;
        }
    }
    
    /**
    * Execute a single, parameterized SQL statement that is NOT a SELECT, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL statement that is NOT a SELECT and returns whether or not it succeeded. "
                                + "The result of the operation is returned in the AfterExec event. "
                                + "See ExecParamSQL for more information." 
                    )
    public void ExecParamSQLAsync(final String tag, final String sql, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final boolean res = ExecParamSQL(sql, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExec(tag, res);
                    }
                });            
            }
        });
    }
    
    /**
    * Execute multiple SQL statements from a file.
    * @param fileName The name of the file
    * @return the number of SQL statements executed, -1 if the database is not open
    */
    @SimpleFunction(description = "Executes multiple SQL statements from a file and returns the count of statements successfully executed. "
                                + "Each line of the file should be a complete non-SELECT SQL statement. "
                                + "Execution stops at the first error. "
                                + "If the database is not open, -1 is returned. "
                                + "Prefix the filename with / to read from a specific file on the SD card. "
                                + "To read assets packaged with an application (also works for the Companion) start "
                                +   "the filename with // (two slashes). "
                                + "If a filename does not start with a slash, it will be read from the applications private storage (for packaged "
                                +   "apps) and from /sdcard/AppInventor/data for the Companion."
                    )
    public int ExecFile(final String fileName) {
        if (db == null) return -1;
        BufferedReader file = null;
        int sqlCount = 0;
        try {
            InputStream is;
            if (fileName.startsWith("//")) {
                if (isRepl)
                    is = new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/AppInventor/assets/" + fileName);
                else
                    is = context.getAssets().open(fileName.substring(2));
            } else
                is = new FileInputStream(resolveFileName(fileName));
            file = new BufferedReader(new InputStreamReader(is));
            String line = file.readLine();
            boolean inComment = false;
            while (line != null) {
                if (inComment) {
                    if (line.matches("\\*\\/")) {                   // end of multiline comment
                        line = line.replaceFirst(".*?\\*\\/", "");
                        inComment = false;
                    } else
                        line = file.readLine();
                    continue;
                } else {
                    line = line.replaceAll("\\/\\*.*?\\*\\/", "");  // remove single line comments
                    line = line.replaceFirst("--.*$", "");          // remove single line comment
                    if (line.matches("\\/\\*")) {                   // start of multiline comment
                        line = line.replaceFirst("\\/\\*.*$", "");
                        inComment = true;
                    }
                }
                
                line = line.trim();
                if (line.length() != 0) {
                    db.execSQL(line);
                    sqlCount++;
                }
                line = file.readLine();
            }
            debug("Executed " + sqlCount + " SQL statements from file");
        } catch (Exception e) {
            debugException(e);
        } finally {
            if (file != null)
                try {
                    file.close();
                } catch (IOException e) {}
            return sqlCount;
        }
    }
    
    /**
    * Execute multiple SQL statements from a file, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param fileName The name of the file
    */
    @SimpleFunction(description = "Executes multiple SQL statements from a file, asynchronously. "
                                + "The result of the operation is returned in the AfterExecMulti event. "
                                + "See ExecFile for more information."
                    )
    public void ExecFileAsync(final String tag, final String fileName) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = ExecFile(fileName);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExecMulti(tag, res);
                    }
                });            
            }
        });
    }
    
    
    /**
    * Execute a list of raw SQL statements that are NOT SELECTs.
    * @param sqlList: The list of SQL statements
    * @return number of SQL statements executed, -1 if the database is not open
    */
    @SimpleFunction(description = "Executes a list of raw SQL statements that are NOT SELECTs and returns the count of statements successfully executed. "
                                + "Execution stops at the first error. "
                                + "If the database is not open, -1 is returned."
                    )
    public int ExecRawSQLList(final YailList sqlList) {
        if (db == null) return -1;
        int sqlCount = 0;
        try {
            for (int i = 0; i < sqlList.size(); i++) {
                db.execSQL(sqlList.getString(i));
                sqlCount++;
            }
            debug("Executed " + sqlCount + " SQL statements");
        } catch (SQLException e) {
            debugException(e);
        } finally {
            return sqlCount;
        }
    }

    /**
    * Execute a list of raw SQL statements that are NOT SELECTs, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sqlList: The list of SQL statements
    */
    @SimpleFunction(description = "Executes a list of raw SQL statements that are NOT SELECTs, asynchronously. "
                                + "The result of the operation is returned in the AfterExecMulti event. "
                                + "See ExecRawSQLList for more information."
                    )
    public void ExecRawSQLListAsync(final String tag, final YailList sqlList) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = ExecRawSQLList(sqlList);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExecMulti(tag, res);
                    }
                });            
            }
        });
    }
    
    /**
    * Execute a single, raw SQL SELECT statement and returns a list of records.
    * @param sql: The SQL statement
    * @return the list of result rows
    */
    @SimpleFunction(description = "Execute a single, raw SQL SELECT statement and returns a list of records. "
                                + "If selecting only a single column, each element in the returned list is the value of that column for each result row. "
                                + "If selecting multiple columns, each element of the returned list is itself a list of values for each selected column. "
                                + "If the DBReturnColumnNames option is true, each column value will be a two element list where the first element is the column name and the second element is the column value. "
                                + "If the database is not open, an empty list is returned."
                    )
    public YailList SelectRawSQL(final String sql) {
        if (db == null) return YailList.makeEmptyList();
        try {
            Cursor cursor = db.rawQuery(sql, null);
            ArrayList rows = cursorToList(cursor);
            debug("Query returned " + rows.size() + " rows");
            return YailList.makeList(rows);
        } catch (SQLException e) {
            debugException(e);
            return YailList.makeEmptyList();
        }
    }
    
    /**
    * Execute a single, raw SQL SELECT statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    */
    @SimpleFunction(description = "Execute a single, raw SQL SELECT statement, asynchronously. "
                                + "The result of the operation is returned in the AfterSelect event. "
                                + "See SelectRawSQL for more information."
                    )
    public void SelectRawSQLAsync(final String tag, final String sql) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final YailList res = SelectRawSQL(sql);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterSelect(tag, res);
                    }
                });
            }
        });
    }
    
    /**
    * Execute a single, parameterized SQL SELECT statement and returns a list of records.
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    * @return the list of result rows
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL SELECT statement and returns a list of records. "
                                + "Each bind parameter replaces the corresponding '?' in WHERE clause in the query. "
                                + "If selecting only a single column, each element in the returned list is the value of that column for each result row. "
                                + "If selecting multiple columns, each element of the returned list is itself a list of values for each selected column. "
                                + "If the DBReturnColumnNames option is true, each column value will be a two element list where the first element is the column name and the second element is the column value. "
                                + "If the database is not open, an empty list is returned."
                    )
    public YailList SelectParamSQL(final String sql, final YailList bindParams) {
        if (db == null) return YailList.makeEmptyList();
        try {
            String[] params = bindParams.toStringArray();
            Cursor cursor = db.rawQuery(sql, params);
            ArrayList rows = cursorToList(cursor);
            debug("Query returned " + rows.size() + " rows");
            return YailList.makeList(rows);
        } catch (SQLException e) {
            debugException(e);
            return YailList.makeEmptyList();
        }
    }
    
    /**
    * Execute a single, parameterized SQL SELECT statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL SELECT statement, asynchronously. "
                                + "The result of the operation is returned in the AfterSelect event. "
                                + "See SelectParamSQL for more information."
                    )
    public void SelectParamSQLAsync(final String tag, final String sql, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final YailList res = SelectParamSQL(sql, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterSelect(tag, res);
                    }
                });
            }
        });
    }
    
    /**
    * Executes a SQL INSERT statement.
    * @param table: Table name
    * @param columns: List of column names
    * @param values: List of column values
    * @return the row ID of the newly inserted row, or -1 if an error occurred
    */
    @SimpleFunction(description = "Executes a SQL INSERT statement. "
                                + "columns contains a list of column names. "
                                + "values contains a list of column values. "
                                + "Returns the row ID of the newly inserted row. "
                                + "If the error occurs or the database is not open, -1 is returned."
                    )
    public long Insert(String table, YailList columns, YailList values) {
        if (db == null) return -1;
        String[] insertColumns = columns.toStringArray();
        String[] insertValues = values.toStringArray();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < insertColumns.length; i++) {
            contentValues.put(insertColumns[i], insertValues[i]);
        }
        try {
            return db.insert(table, null, contentValues);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
    
    /**
    * Executes a SQL INSERT statement.
    * @param table: Table name
    * @param columnsAndValues: List of lists
    * @return the row ID of the newly inserted row, or -1 if an error occurred
    */
    @SimpleFunction(description = "Executes a SQL INSERT statement. "
                                + "columnsAndValues contains a list of lists. "
                                + "Each list element is two element list where the first element is a column name and the second element is a column value. "
                                + "Returns the row ID of the newly inserted row. "
                                + "If the error occurs or the database is not open, -1 is returned."
                    )
    public long InsertPairs(String table, YailList columnsAndValues) {
        if (db == null) return -1;
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < columnsAndValues.size(); i++) {
            Object e = columnsAndValues.getObject(i);
            if (e instanceof YailList)
                contentValues.put(((YailList)e).getString(0), ((YailList)e).getString(1));
            else {
                debugException(new IllegalArgumentException("Expected list of lists."));
                return -1;
            }
        }
        try {
            return db.insert(table, null, contentValues);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
        
    /**
    * Executes a SQL REPLACE statement.
    * @param table: Table name
    * @param columns: List of column names
    * @param values: List of column values
    * @return the row ID of the newly inserted or updated row, or -1 if an error occurred
    */
    @SimpleFunction(description = "Executes a SQL REPLACE statement. "
                                + "columns contains a list of column names. "
                                + "values contains a list of column values. "
                                + "Returns the row ID of the newly inserted or updated row. "
                                + "If the error occurs or the database is not open, -1 is returned."
                    )
    public long Replace(String table, YailList columns, YailList values) {
        String[] replaceColumns = columns.toStringArray();
        String[] replaceValues = values.toStringArray();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < replaceColumns.length; i++) {
            contentValues.put(replaceColumns[i], replaceValues[i]);
        }
        try {
            return db.replace(table, null, contentValues);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }

    /**
    * Executes a SQL REPLACE statement.
    * @param table: Table name
    * @param columnsAndValues: List of lists
    * @return the row ID of the newly inserted row, or -1 if an error occurred
    */
    @SimpleFunction(description = "Executes a SQL REPLACE statement. "
                                + "columnsAndValues contains a list of lists. "
                                + "Each list element is two element list where the first element is a column name and the second element is a column value. "
                                + "Returns the row ID of the newly inserted or updated row. "
                                + "If the error occurs or the database is not open, -1 is returned."
                    )
    public long ReplacePairs(String table, YailList columnsAndValues) {
        if (db == null) return -1;
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < columnsAndValues.size(); i++) {
            Object e = columnsAndValues.getObject(i);
            if (e instanceof YailList)
                contentValues.put(((YailList)e).getString(0), ((YailList)e).getString(1));
            else {
                debugException(new IllegalArgumentException("Expected list of lists."));
                return -1;
            }
        }
        try {
            return db.replace(table, null, contentValues);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }

    /**
    * Executes a SQL UPDATE statement.
    * @param table: Table name
    * @param columns: List of column names
    * @param values: List of column values
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will update all rows.
    * @param bindParams: The list of parameter values to bind
    * @return the number of rows affected
    */
    @SimpleFunction(description = "Executes a SQL UPDATE statement. "
                                + "columns contains a list of column names. "
                                + "values contains a list of column values. "
                                + "There whereClause is optional. "
                                + "All rows in the table will be updated if no whereClause is specified. "
                                + "Each bind parameter replaces the corresponding '?' in whereClause. "
                                + "Returns the number of rows affected."
                                + "If an error occurs or the database is not open, -1 is returned."
                    )
    public int Update(String table, YailList columns, YailList values, String whereClause, YailList bindParams) {
        if (db == null) return -1;
        String[] updateColumns = columns.toStringArray();
        String[] updateValues = values.toStringArray();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < updateColumns.length; i++) {
            contentValues.put(updateColumns[i], updateValues[i]);
        }
        whereClause = (whereClause == "" ? null : whereClause);
        String[] params = bindParams.toStringArray();
        try {
            return db.update(table, contentValues, whereClause, params);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
    
    /**
    * Executes a SQL UPDATE statement.
    * @param table: Table name
    * @param columnsAndValues: List of lists
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will update all rows.
    * @param bindParams: The list of parameter values to bind
    * @return the number of rows affected
    */
    @SimpleFunction(description = "Executes a SQL UPDATE statement. "
                                + "columnsAndValues contains a list of lists. "
                                + "Each list element is two element list where the first element is a column name and the second element is a column value. "
                                + "There whereClause is optional. "
                                + "All rows in the table will be updated if no whereClause is specified. "
                                + "Each bind parameter replaces the corresponding '?' in whereClause. "
                                + "Returns the number of rows affected."
                                + "If an error occurs or the database is not open, -1 is returned."
                    )
    public int UpdatePairs(String table, YailList columnsAndValues, String whereClause, YailList bindParams) {
        if (db == null) return -1;
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < columnsAndValues.size(); i++) {
            Object e = columnsAndValues.getObject(i);
            if (e instanceof YailList)
                contentValues.put(((YailList)e).getString(0), ((YailList)e).getString(1));
            else {
                debugException(new IllegalArgumentException("Expected list of lists."));
                return -1;
            }
        }
        whereClause = (whereClause == "" ? null : whereClause);
        String[] params = bindParams.toStringArray();
        try {
            return db.update(table, contentValues, whereClause, params);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
    
    /**
    * Executes a SQL DELETE statement.
    * @param table: Table name
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will delete all rows.
    * @param bindParams: The list of parameter values to bind
    * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a count pass "1" as the whereClause. 
    */
    @SimpleFunction(description = "Executes a SQL DELETE statement. "
                                + "There whereClause is optional. "
                                + "All rows in the table will be deleted if no whereClause is specified. "
                                + "Each bind parameter replaces the corresponding '?' in whereClause. "
                                + "Returns the number of rows affected if a whereClause is passed in, 0 otherwise."
                                + "If an error occurs or the database is not open, -1 is returned."
                    )
    public int Delete(String table, String whereClause, YailList bindParams) {
        if (db == null) return -1;
        whereClause = (whereClause == "" ? null : whereClause);
        String[] params = bindParams.toStringArray();
        try {
            return db.delete(table, whereClause, params);
        } catch (SQLException e) {
            debugException(e);
            return -1;
        }
    }
    
    /**
    * Executes a SQL SELECT statement.
    * @param table: The table
    * @param distinct: Should the query return distinct rows
    * @param columns: List of column names to return
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will return all rows.
    * @param bindParams: The list of parameter values to bind
    * @param groupBy: How to group rows, formatted as a SQL GROUP BY clause. Passing an empty string will cause the row to not be grouped.
    * @param having: Which row groups to include if row grouping is being used. Passing an empty string will cause all row groups to be included.
    * @param orderBy: How to order the rows, formatted as a SQL ORDER BY clause. Passing an empty string will return rows unordered.
    * @param limit: Limit how many rows to return, formatted as SQL LIMIT clause. Passing an empty string returns all matching rows.
    */
    @SimpleFunction(description = "Executes a SQL SELECT statement. "
                                + "There whereClause is optional. "
                                + "All rows in the table will be returned if no whereClause is specified. "
                                + "Each bind parameter replaces the corresponding '?' in whereClause. "
                                + "groupBy is optional. If not specified, no grouping will be performed. "
                                + "having is optional. If not specified, all row groups will be returned. "
                                + "orderBy is optional. If not specified, rows will be returned unordered. "
                                + "limit is optional. If not specified, all matching rows will be returned. "
                                + "If selecting only a single column, each element in the returned list is the value of that column for each result row. "
                                + "If selecting multiple columns, each element of the returned list is itself a list of values for each selected column. "
                                + "If the DBReturnColumnNames option is true, each column value will be a two element list where the first element is the column name and the second element is the column value. "
                                + "See SelectRawSQL for more information on the returned list."
                    )
    public YailList Select(final String table, final boolean distinct, final YailList columns, String whereClause, final YailList bindParams, String groupBy, String having, String orderBy, String limit) {
        if (db == null) return YailList.makeEmptyList();
        
        String[] queryColumns = columns.toStringArray();
        String[] params = bindParams.toStringArray();
        whereClause = (whereClause == "" ? null : whereClause);
        groupBy = (groupBy == "" ? null : groupBy);
        having = (having == "" ? null : having);
        orderBy = (orderBy == "" ? null : orderBy);
        limit = (limit == "" ? null : limit);
        
        try {
            Cursor cursor = db.query(distinct, table, queryColumns, whereClause, params, groupBy, having, orderBy, limit);
            ArrayList rows = cursorToList(cursor);
            debug("Query returned " + rows.size() + " rows");
            return YailList.makeList(rows);
        } catch (SQLException e) {
            debugException(e);
            return YailList.makeEmptyList();
        }
    }

    /**
    * Executes a SQL SELECT statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param table: The table
    * @param distinct: Should the query return distinct rows
    * @param columns: List of column names to return
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will return all rows.
    * @param bindParams: The list of parameter values to bind
    * @param groupBy: How to group rows, formatted as a SQL GROUP BY clause. Passing an empty string will cause the row to not be grouped.
    * @param having: Which row groups to include if row grouping is being used. Passing an empty string will cause all row groups to be included.
    * @param orderBy: How to order the rows, formatted as a SQL ORDER BY clause. Passing an empty string will return rows unordered.
    * @param limit: Limit how many rows to return, formatted as SQL LIMIT clause. Passing an empty string returns all matching rows.
    * @return the list of result rows
    */
    @SimpleFunction(description = "Executes a SQL SELECT statement, asynchronously. "
                                + "The result of the operation is returned in the AfterSelect event. "
                                + "See Select for more information."
                    )
    public void SelectAsync(final String tag,
                                final String table,
                                final boolean distinct,
                                final YailList columns,
                                final String whereClause,
                                final YailList bindParams,
                                final String groupBy,
                                final String having,
                                final String orderBy,
                                final String limit) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final YailList res = Select(table, distinct, columns, whereClause, bindParams, groupBy, having, orderBy, limit);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterSelect(tag, res);
                    }
                });
            }
        });
    }
    
    /**
    * Converts a cursor of returned records to a list.
    * @param c: The cursor
    * @return The list of result rows
    */
    private ArrayList cursorToList(Cursor c) {
        String[] columnNames = c.getColumnNames();
        int columnCount = c.getColumnCount();
        boolean singleColumn = columnCount == 1;
        ArrayList rows = new ArrayList();
        
        while (c.moveToNext()) {
            for (int i = 0; i < columnCount; i++) {
                if (singleColumn)
                    rows.add(columnValue(c, i));
                else {
                    ArrayList row = new ArrayList();
                    if (dbReturnColumnNames) {
                        ArrayList column = new ArrayList();
                        column.add(columnNames[i]);
                        column.add(columnValue(c, i));
                        row.add(column);
                    } else
                        row.add(columnValue(c, i));
                    rows.add(row);
                }
            }
        }
        c.close();
        return rows;
    }
    
    /**
    * Gets a column value from a cursor.
    * @param c: The cursor
    * @param column: The column index
    * @return The value of the column
    */
    private Object columnValue(Cursor c, int column) {
        switch (c.getType(column)) {
            case Cursor.FIELD_TYPE_NULL:
                return null;
            case Cursor.FIELD_TYPE_INTEGER:
                return c.getInt(column);
            case Cursor.FIELD_TYPE_FLOAT:
                return c.getDouble(column);
            case Cursor.FIELD_TYPE_STRING:
                return c.getString(column);
            case Cursor.FIELD_TYPE_BLOB:
                return new String(c.getBlob(column));
            default:
                return null;
        }
    }
    
    /**
    * Returns absolute file path.
    * @param filename the file used to construct the file path
    */
    
    private String resolveFileName(String fileName) {
        if (fileName.startsWith("/"))
            return Environment.getExternalStorageDirectory().getPath() + fileName;
        java.io.File dirPath = context.getFilesDir();
        if (isRepl) {
            String path = Environment.getExternalStorageDirectory().getPath() + "/AppInventor/data/";
            dirPath = new java.io.File(path);
            if (! dirPath.exists()) {
                dirPath.mkdirs();           // Make sure it exists
            }
        }
        return dirPath.getPath() + "/" + fileName;
    }
    


                
    // ==========================================
    // Events
    
    @SimpleEvent(description = "This event fires when the database is opened.")
    public void DatabaseOpened() {
        EventDispatcher.dispatchEvent(this, "DatabaseOpened");
    }
    
    @SimpleEvent(description = "This event fires when the database is closed.")
    public void DatabaseClosed() {
        EventDispatcher.dispatchEvent(this, "DatabaseClosed");
    }
    
    @SimpleEvent(description = "This event fires when the database is upgraded. "
                             + "The previous and new version numbers are provided. "
                             + "Use this event to modifiy the database as required by the version change."
                )
    public void DatabaseUpgrade(int oldVersion, int newVersion) {
        EventDispatcher.dispatchEvent(this, "DatabaseUpgrade", oldVersion, newVersion);
    }
    
    @SimpleEvent(description = "This event fires when the database is downgraded. "
                             + "The previous and new version numbers are provided. "
                             + "Use this event to modifiy the database as required by the version change."
                )
    public void DatabaseDowngrade(int oldVersion, int newVersion) {
        EventDispatcher.dispatchEvent(this, "DatabaseDowngrade", oldVersion, newVersion);
    }

    @SimpleEvent(description = "This event fires after an ExecRawSQLAsync or ExecParamSQLAsync call. "
                             + "The tag specified in the original call and the result of the execution are provided."
                )
    public void AfterExec(String tag, boolean result) {
        EventDispatcher.dispatchEvent(this, "AfterExec", tag, result);
    }

    @SimpleEvent(description = "This event fires after an ExecFileAsync or ExecRawSQLListAsync call. "
                             + "The tag specified in the original call and the result of the execution are provided."
                )
    public void AfterExecMulti(String tag, int execCount) {
        EventDispatcher.dispatchEvent(this, "AfterExecMulti", tag, execCount);
    }

    @SimpleEvent(description = "This event fires after an asynchronous Select call. "
                             + "The tag specified in the original call, the number of returned rows, and the result rows are provided."
                )
    public void AfterSelect(String tag, YailList rows) {
        EventDispatcher.dispatchEvent(this, "AfterSelect", tag, rows.size(), rows);
    }
    
    @SimpleEvent(description = "This event fires when a SQL error occurs. "
                             + "The error message is provided."
                )
    public void SQLError(String message) {
        EventDispatcher.dispatchEvent(this, "SQLError", message);
    }



    
}
