/*

MIT License

Copyright (c) 2018 Thomas Bennedum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package org.bennedum.SQLite;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
@UsesPermissions(permissionNames = "android.permission.READ_EXTERNAL_STORAGE, android.permission.WRITE_EXTERNAL_STORAGE")


@SimpleObject(external = true)
public class SQLite extends AndroidNonvisibleComponent implements Component {

    public static final int VERSION = 1;
    
    private static final String NAME = "SQLite";
    
    // Extension properties
    private boolean debugToast = false;
    private boolean debugDialog = false;
    private String dbName = "db.sqlite";
    private int dbVersion = 1;
    private boolean returnColumnNames = false;
    
    private ComponentContainer container;
    private Context context;
    private boolean isRepl;
    
    private DBHelper dbHelper = null;
    private SQLiteDatabase db = null;

    /**
    * Helper class for handling database life cycle events.
    */
    private class DBHelper extends SQLiteOpenHelper {
    
        public DBHelper(Context context) {
            super(context, dbName, null, dbVersion);
        }
        
        @Override
        public void onOpen(SQLiteDatabase db) {
            debug("Database opened");
            SQLite.this.db = db;
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DatabaseOpened();
                }
            });
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            debug("Database created");
            SQLite.this.db = db;
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DatabaseCreated();
                }
            });
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, final int oldVersion, final int newVersion) {
            debug("Database upgraded");
            SQLite.this.db = db;
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DatabaseUpgrade(oldVersion, newVersion);
                }
            });
        }
    
        @Override
        public void onDowngrade(SQLiteDatabase db, final int oldVersion, final int newVersion) {
            debug("Database downgraded");
            SQLite.this.db = db;
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DatabaseDowngrade(oldVersion, newVersion);
                }
            });
        }
    
    }

    /**
    * Helper class to pass into DBAsyncTask.
    * This is necessary because using a Runnable causes the static execute method to run and that's
    * how you get ants.
    */
    private abstract class DBRunnable {
        public abstract void run();
    }
    
    /**
    * Helper class to ensure all DB calls are serialized on a single thread.
    * Also provides a method to wait on the task completing.
    */
    private class DBAsyncTask extends AsyncTask<DBRunnable, Void, Void> {
    
        public ArrayList rows = null;
        public long id = -1;
        public int count = -1;
        public boolean success = false;
        
        @Override
        protected Void doInBackground(DBRunnable... runnables) {
            if (runnables.length != 1)
                throw new RuntimeException("One runnable is required.");
            runnables[0].run();
            return null;
        }
        
        public boolean waitUntilDone() {
            try {
                get();
                return true;
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof Exception)
                    debugException((Exception)t);
                return false;
            } catch (Exception e) {
                return false;
            } finally {
                if (rows == null) rows = new ArrayList();
            }
        }

        public boolean executeAndWait(DBRunnable... runnables) {
            executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, runnables);
            return waitUntilDone();
        }
        
    }


    /**
    * Constructor
    */
    public SQLite(ComponentContainer container) {
        super(container.$form());
        isRepl = form instanceof ReplForm;  // Note: form is defined in our superclass
        this.container = container;
        context = (Context)container.$context();
    }

    private void debug(final String message) {
        if (debugToast || debugDialog) {
            form.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (debugToast)
                        Toast.makeText(context, NAME + ": " + message, Toast.LENGTH_SHORT).show();
                    if (debugDialog)
                        //new Notifier(form).ShowAlert(NAME + ": " + message);                    
                        new Notifier(form).ShowMessageDialog(NAME + ": " + message, NAME, "OK");
                }
            });
        }
    }
    
    private void debugException(final Exception e) {
        if (e != null) {
            debug(e.getMessage());
            if (e instanceof SQLException)
                SQLError(e.getMessage());
        }
    }
    
    private void toast(final String message) {
        Toast.makeText(context, NAME + ": " + message, Toast.LENGTH_SHORT).show();
    }
    
    private boolean checkDB(final String action) {
        if (db == null) {
            debug("Database is not open: " + action);
            return false;
        }
        return true;
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
                    description = "Specifies whether debug messages should be displayed in dialogs."
                    )
    public boolean DebugDialog() {
        return debugDialog;
    }
  
    /**
    * Display debugging messages as alerts.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false"
                      )
    @SimpleProperty
    public void DebugDialog(boolean debugDialog) {
        this.debugDialog = debugDialog;
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
    * Should result lists contain column names.
    */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR,
                    description = "Specifies whether lists of results will contain column names. "
                                + "See the query blocks for more information."
                    )
    public boolean ReturnColumnNames() {
        return returnColumnNames;
    }
  
    /**
    * Should result lists contain column names.
    */
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
                      defaultValue = "false")
    @SimpleProperty
    public void ReturnColumnNames(boolean returnColumnNames) {
        this.returnColumnNames = returnColumnNames;
    }


    //========================================================
    // Utility methods
    //
    
    /**
    * Returns the path to the database.
    */
    @SimpleFunction(description = "Returns the full path to the database, even if it doesn't exist yet.")
    public String DatabasePath() {
        return context.getDatabasePath(dbName).getPath();
    }
    
    /**
    * Returns true if the database file exists, false otherwise.
    */
    @SimpleFunction(description = "Returns true if the database file exists, false otherwise.")
    public boolean DatabaseExists() {
        return context.getDatabasePath(dbName).exists();
    }
    
    /**
    * Delete the database.
    */
    @SimpleFunction(description = "Deletes a closed database. "
                                + "This deletes the database file permanently."
                    )
    public void DeleteDatabase() {
        if (db != null) {
            debugException(new Exception("Unable to delete when the database is open."));
            return;
        }
        context.deleteDatabase(dbName);
        debug("Database deleted");
    }
    
    /**
    * Import a SQLite database file.
    * This is probably not best done on the main thread.
    */
    @SimpleFunction(description = "Imports a SQLite database completely replacing the currently closed database. "
                                + "Returns true if the import was successful, false otherwise."
                    )
    public boolean ImportDatabase(String fileName) {
        if (db != null) {
            debugException(new Exception("Unable to import when the database is open."));
            return false;
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = openInputStream(fileName);
            os = new FileOutputStream(context.getDatabasePath(dbName));
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0)
                os.write(buf, 0, len);
            debug("Database imported");
            return true;
        } catch (IOException e) {
            debugException(e);
            return false;
        } finally {
            try {
                if (is != null) is.close();
                if (os != null) os.close();
            } catch (IOException e) {}
        }
    }
    
    /**
    * Exports the database.
    * This is probably not best done on the main thread.
    */
    @SimpleFunction(description = "Exports the currently closed database to the specified file. "
                                + "The resulting file is a complete SQLite database."
                                + "Returns true if the import was successful, false otherwise."
                    )
    public boolean ExportDatabase(String fileName) {
        if (db != null) {
            debugException(new Exception("Unable to export when the database is open."));
            return false;
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(context.getDatabasePath(dbName));
            os = new FileOutputStream(resolveFileName(fileName));
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0)
                os.write(buf, 0, len);
            debug("Database exported");
            return true;
        } catch (IOException e) {
            debugException(e);
            return false;
        } finally {
            try {
                if (is != null) is.close();
                if (os != null) os.close();
            } catch (IOException e) {}
        }
    }

    //========================================================
    // Database methods
    //
    
    /**
    * Opens the database.
    */
    @SimpleFunction(description = "Opens the database. "
                                + "If the database is already open, nothing happens."
                    )
    public void OpenDatabase() {
        if (db == null) {
            final DBAsyncTask task = new DBAsyncTask();
            task.executeAndWait(new DBRunnable() {
                @Override
                public void run() {
                    if (db != null) {
                        task.success = true;
                        return;
                    }
                    try {
                        dbHelper = new DBHelper(context);
                        db = dbHelper.getWritableDatabase();
                        task.success = true;
                    } catch (SQLException e) {
                        db = null;
                        dbHelper = null;
                        debugException(e);
                    }
                }
            });
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
            final DBAsyncTask task = new DBAsyncTask();
            task.executeAndWait(new DBRunnable() {
                @Override
                public void run() {
                    if (db == null) return;
                    db.close();
                    db = null;
                    dbHelper = null;
                    task.success = true;
                }
            });
            if (task.success) {
                debug("Database closed");
                DatabaseClosed();
            }
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
        if (! checkDB("TableCount")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    String sql = "SELECT count(1) FROM sqlite_master WHERE type='table'";
                    Cursor cursor = db.rawQuery(sql, null);
                    cursor.moveToNext();
                    task.count = cursor.getInt(0);
                    cursor.close();
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        return task.count;
    }
    
    /**
    * Returns a list of names of the tables in the the database.
    */
    @SimpleFunction(description = "Returns a list of names of the tables in the database, or an empty list if an error occurs or the database is not open.")
    public YailList TableNames() {
        if (! checkDB("TableNames")) return YailList.makeEmptyList();
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    String sql = "SELECT name FROM sqlite_master WHERE type='table'";
                    Cursor cursor = db.rawQuery(sql, null);
                    task.rows = new ArrayList();
                    while (cursor.moveToNext())
                        task.rows.add(cursor.getString(0));
                    cursor.close();
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        return YailList.makeList(task.rows);
    }
    
    /**
    * Returns true if the table exists in the database, false otherwise.
    */
    @SimpleFunction(description = "Returns true if the table exists in the database, or false if the table does not exist or an error occurs or the database is not open.")
    public boolean TableExists(final String table) {
        if (! checkDB("TableExists")) return false;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    String sql = "SELECT count(1) FROM sqlite_master WHERE type='table' AND name=?";
                    Cursor cursor = db.rawQuery(sql, new String[] {table});
                    cursor.moveToNext();
                    task.count = cursor.getInt(0);
                    cursor.close();
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        return task.count == 1;
    }
    
    /**
    * Returns the number of records in a table.
    */
    @SimpleFunction(description = "Returns the number of rows in a table, or -1 if an error occurs or the database is not open.")
    public int TableRowCount(final String table) {
        if (! checkDB("TableRowCount")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    String sql = "SELECT count(1) FROM '" + table + "'";
                    Cursor cursor = db.rawQuery(sql, null);
                    cursor.moveToNext();
                    task.count = cursor.getInt(0);
                    cursor.close();
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        return task.count;
    }

    //========================================================
    // Transaction methods
    //
    
    /**
    * Begin a transaction.
    */
    @SimpleFunction(description = "Begins a transaction on an open database. "
                                + "Nested transactions are supported."
                    )
    public void BeginTransaction() {
        if (! checkDB("BeginTransaction")) return;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    db.beginTransaction();
                    task.success = true;
                } catch (Exception e) {
                    debugException(e);
                }
            }
        });
        if (task.success)
            debug("Transaction started");
    }
    
    /**
    * Commit a transaction.
    */
    @SimpleFunction(description = "Commits the last open transaction.")
    public void CommitTransaction() {
        if (! checkDB("CommitTransaction")) return;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    task.success = true;
                } catch (Exception e) {
                    debugException(e);
                }
            }
        });
        if (task.success)
            debug("Transaction committed");
    }
    
    /**
    * Rollback a transaction.
    */
    @SimpleFunction(description = "Rolls back the last open transaction.")
    public void RollbackTransaction() {
        if (! checkDB("RollbackTransaction")) return;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    debugException(e);
                }
            }
        });
        if (task.success)
            debug("Transaction rolled back");
    }
        
    //========================================================
    // Data manipulation methods
    //
        
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
    public boolean Execute(final String sql, final YailList bindParams) {
        if (! checkDB("Execute")) return false;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    db.execSQL(sql, (bindParams == null) ? null : bindParams.toStringArray());
                    task.success = true;
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        if (task.success)
            debug("Execute: " + sql);
        return task.success;
    }
    
    /**
    * Execute a single, parameterized SQL statement that is NOT a SELECT, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL statement that is NOT a SELECT and returns whether or not it succeeded. "
                                + "The tag identifies the result of this call in the AfterExecute event. "
                                + "See ExecParamSQL for more information." 
                    )
    public void ExecuteAsync(final String tag, final String sql, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final boolean res = Execute(sql, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExecute(tag, res);
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
                                + "Each line of the file should be a complete non-SELECT SQL statement, optionally followed by a semicolon. "
                                + "Single line (--) and multiline (/*...*/) comments are ignored. "
                                + "Line continuation using '\\' is supported. "
                                + "'\\n' are replaced with actual newlines. "
                                + "Execution stops at the first error. "
                                + "If the database is not open, -1 is returned. "
                                + "Prefix the filename with / to read from a specific file on the SD card. "
                                + "To read assets packaged with an application (also works for the Companion) start "
                                +   "the filename with // (two slashes). "
                                + "If a filename does not start with a slash, it will be read from the applications private storage (for packaged "
                                +   "apps) and from /sdcard/AppInventor/data for the Companion."
                    )
    public int ExecuteFile(final String fileName) {
        if (! checkDB("ExecuteFile")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                BufferedReader file = null;
                task.count = 0;
                try {
                    InputStream is = openInputStream(fileName);
                    file = new BufferedReader(new InputStreamReader(is));
                    boolean inComment = false;
                    boolean inLine = false;
                    String fullLine = "";
                    
                    for (String line = file.readLine(); line != null; line = file.readLine()) {
                        if (inComment) {
                            if (line.matches("\\*\\/")) {                   // end of multiline comment
                                line = line.replaceFirst(".*?\\*\\/", "");
                                inComment = false;
                            } else
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
                        if (line.endsWith("\\")) {
                            inLine = true;
                            fullLine += line.substring(0, line.length() - 1).trim();
                            continue;
                        }
                        inLine = false;
                        
                        fullLine += line;
                        
                        if (fullLine.endsWith(";"))
                            fullLine = fullLine.substring(0, fullLine.length() - 1).trim();
                        
                        if (fullLine.length() != 0) {
                            fullLine = fullLine.replace("\\n", "\n");     // replace \n with actual newline
                            db.execSQL(fullLine);
                            task.count++;
                            fullLine = "";
                        }
                    }
                } catch (Exception e) {
                    debugException(e);
                } finally {
                    if (file != null)
                        try {
                            file.close();
                        } catch (IOException e) {}
                }
            }
        });
        debug("ExecuteFile: " + task.count + " statements executed");
        return task.count;
    }
    
    /**
    * Execute multiple SQL statements from a file, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param fileName The name of the file
    */
    @SimpleFunction(description = "Executes multiple SQL statements from a file, asynchronously. "
                                + "The tag identifies the result of this call in the AfterExecuteFile event. "
                                + "See ExecFile for more information."
                    )
    public void ExecuteFileAsync(final String tag, final String fileName) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = ExecuteFile(fileName);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterExecuteFile(tag, res);
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
                                + "If the ReturnColumnNames option is true, each column value will be a two element list where the first element is the column name and the second element is the column value. "
                                + "If the database is not open, an empty list is returned."
                    )
    public YailList SelectSQL(final String sql, final YailList bindParams) {
        if (! checkDB("SelectSQL")) return YailList.makeEmptyList();
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = db.rawQuery(sql, (bindParams == null) ? null : bindParams.toStringArray());
                    task.rows = cursorToList(cursor);
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        debug("SelectSQL: " + task.rows.size() + " rows");
        return YailList.makeList(task.rows);
    }
    
    /**
    * Execute a single, parameterized SQL SELECT statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    */
    @SimpleFunction(description = "Execute a single, parameterized SQL SELECT statement, asynchronously. "
                                + "The tag identifies the result of this call in the AfterSelect event. "
                                + "See SelectSQL for more information."
                    )
    public void SelectSQLAsync(final String tag, final String sql, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final YailList res = SelectSQL(sql, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterSelect(tag, res.size(), res);
                    }
                });
            }
        });
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
                                + "If the ReturnColumnNames option is true, each column value will be a two element list where the first element is the column name and the second element is the column value. "
                                + "See SelectRawSQL for more information on the returned list."
                    )
    public YailList Select(final String table,
                           final boolean distinct,
                           final YailList columns,
                           final String whereClause,
                           final YailList bindParams,
                           final String groupBy,
                           final String having,
                           final String orderBy,
                           final String limit) {
        if (! checkDB("Select")) return YailList.makeEmptyList();
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    Cursor cursor = db.query(distinct, table,
                                                columns.toStringArray(),
                                                (whereClause == "") ?  null : whereClause,
                                                bindParams.toStringArray(),
                                                (groupBy == "") ? null : groupBy,
                                                (having == "") ? null : having,
                                                (orderBy == "") ? null : orderBy,
                                                (limit == "") ? null : limit
                                            );
                    task.rows = cursorToList(cursor);
                } catch (SQLException e) {
                    debugException(e);
                    task.rows = new ArrayList();
                }
            }
        });
        debug("Select " + task.rows.size() + " rows from " + table);
        return YailList.makeList(task.rows);
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
                                + "The tag identifies the result of this call in the AfterSelect event. "
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
                        AfterSelect(tag, res.size(), res);
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
    public long Insert(final String table, final YailList columns, final YailList values) {
        if (! checkDB("Insert")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    task.id = db.insert(table, null, makeContentValues(columns, values));
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        debug("Insert: " + table + " id = " + task.id);
        return task.id;
    }

    /**
    * Executes a SQL INSERT statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param sql: The SQL statement
    * @param bindParams: The list of parameter values to bind
    */
    @SimpleFunction(description = "Executes a SQL INSERT statement, asynchronously. "
                                + "The tag identifies the result of this call in the AfterInsert event. "
                                + "See Insert for more information."
                    )
    public void InsertAsync(final String tag, final String table, final YailList columns, final YailList values) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final long res = Insert(table, columns, values);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterInsert(tag, res);
                    }
                });
            }
        });
    }
    
    /**
    * Inserts rows from a file.
    * @param table Table name
    * @param fileName The name of the file
    * @return the number of rows inserted, -1 if the database is not open
    */
    @SimpleFunction(description = "Inserts multiple rows of data from a file into a table. "
                                + "The first line of the file should be a CSV list of the column names. "
                                + "Each of the remaining lines should be a CSV list of values for each new row. "
                                + "Empty lines are ignored. "
                                + "Line continuation using '\\' is supported. "
                                + "'\\n' are replaced with actual newlines. "
                                + "Insertion stops at the first error. "
                                + "If the database is not open, -1 is returned. "
                                + "Prefix the filename with / to read from a specific file on the SD card. "
                                + "To read assets packaged with an application (also works for the Companion) start "
                                +   "the filename with // (two slashes). "
                                + "If a filename does not start with a slash, it will be read from the applications private storage (for packaged "
                                +   "apps) and from /sdcard/AppInventor/data for the Companion."
                    )
    public int InsertFile(final String table, final String fileName) {
        if (! checkDB("InsertFile")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                BufferedReader file = null;
                task.count = 0;
                try {
                    InputStream is = openInputStream(fileName);
                    file = new BufferedReader(new InputStreamReader(is));
                    boolean inLine = false;
                    String fullLine = "";
                    String[] columnNames = null;
                    
                    for (String line = file.readLine(); line != null; line = file.readLine()) {
                        line = line.trim();
                        if (line.endsWith("\\")) {
                            inLine = true;
                            fullLine += line.substring(0, line.length() - 1).trim();
                            continue;
                        }
                        inLine = false;
                        
                        fullLine += line;
                        
                        if (fullLine.length() != 0) {
                            
                            if (columnNames == null)
                                columnNames = fullLine.split("\\s*,\\s*");
                            else {
                                fullLine = fullLine.replace("\\n", "\n");     // replace \n with actual newline
                                String[] values = fullLine.split("\\s*,\\s*");
                                ContentValues contentValues = new ContentValues();
                                for (int i = 0; i < columnNames.length; i++) {
                                    contentValues.put(columnNames[i], values[i]);
                                }
                                db.insert(table, null, contentValues);
                                task.count++;
                            }
                            fullLine = "";
                        }
                    }
                } catch (Exception e) {
                    debugException(e);
                } finally {
                    if (file != null)
                        try {
                            file.close();
                        } catch (IOException e) {}
                }
            }
        });
        debug("InsertFile: " + task.count + " rows inserted");
        return task.count;
    }
    
    /**
    * Inserts rows from a file, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param table Table name
    * @param fileName The name of the file
    */
    @SimpleFunction(description = "Inserts multiple rows of data from a file into a table, asynchronously. "
                                + "The tag identifies the result of this call in the AfterInsertFile event. "
                                + "See InsertFile for more information."
                    )
    public void InsertFileAsync(final String tag, final String table, final String fileName) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = InsertFile(table, fileName);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterInsertFile(tag, res);
                    }
                });
            }
        });
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
    public long Replace(final String table, final YailList columns, final YailList values) {
        if (! checkDB("Replace")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    task.id = db.replace(table, null, makeContentValues(columns, values));
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        debug("Replace: " + table + " id = " + task.id);
        return task.id;
    }

    /**
    * Executes a SQL REPLACE statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param table: Table name
    * @param columns: List of column names
    * @param values: List of column values
    * @return the row ID of the newly inserted or updated row, or -1 if an error occurred
    */
    @SimpleFunction(description = "Executes a SQL REPLACE statement, asynchronously. "
                                + "The tag identifies the result of this call in the AfterReplace event. "
                                + "See Replace for more information."
                    )
    public void ReplaceAsync(final String tag, final String table, final YailList columns, final YailList values) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final long res = Replace(table, columns, values);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterReplace(tag, res);
                    }
                });
            }
        });
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
    public int Update(final String table, final YailList columns, final YailList values, final String whereClause, final YailList bindParams) {
        if (! checkDB("Update")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    task.count = db.update(table, makeContentValues(columns, values), (whereClause == "") ? null : whereClause, bindParams.toStringArray());
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        debug("Update: " + table + " " + task.count + " rows");
        return task.count;
    }
    
    /**
    * Executes a SQL UPDATE statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param table: Table name
    * @param columns: List of column names
    * @param values: List of column values
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will update all rows.
    * @param bindParams: The list of parameter values to bind
    * @return the number of rows affected
    */
    @SimpleFunction(description = "Executes a SQL UPDATE statement, asynchronously. "
                                + "The tag identifies the result of this call in the AfterUpdate event. "
                                + "See Update for more information."
                    )
    public void UpdateAsync(final String tag, final String table, final YailList columns, final YailList values, final String whereClause, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = Update(table, columns, values, whereClause, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterUpdate(tag, res);
                    }
                });
            }
        });
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
    public int Delete(final String table, final String whereClause, final YailList bindParams) {
        if (! checkDB("Delete")) return -1;
        final DBAsyncTask task = new DBAsyncTask();
        task.executeAndWait(new DBRunnable() {
            @Override
            public void run() {
                try {
                    task.count = db.delete(table, (whereClause == "") ? null : whereClause, bindParams.toStringArray());
                } catch (SQLException e) {
                    debugException(e);
                }
            }
        });
        if ((task.count == 0) && ((whereClause == null) || (whereClause == "")))
            debug("Delete: " + table + " all rows");
        else
            debug("Delete: " + table + " " + task.count + " rows");
        return task.count;
    }

    /**
    * Executes a SQL DELETE statement, asynchronously.
    * @param tag: The identifier for the result of this operation
    * @param table: Table name
    * @param whereClause: The optional WHERE clause. Passing null or an empty string will delete all rows.
    * @param bindParams: The list of parameter values to bind
    * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a count pass "1" as the whereClause. 
    */
    @SimpleFunction(description = "Executes a SQL DELETE statement. "
                                + "The tag identifies the result of this call in the AfterDelete event. "
                                + "See Delete for more information."
                    )
    public void DeleteAsync(final String tag, final String table, final String whereClause, final YailList bindParams) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                final int res = Delete(table, whereClause, bindParams);
                form.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AfterDelete(tag, res);
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
        ArrayList row;
        ArrayList column;
        
        while (c.moveToNext()) {
            if (singleColumn) {
                if (returnColumnNames) {
                    column = new ArrayList();
                    column.add(columnNames[0]);
                    column.add(columnValue(c, 0));
                    rows.add(column);
                } else {
                    rows.add(columnValue(c, 0));
                }
            } else {
                row = new ArrayList();
                for (int i = 0; i < columnCount; i++) {
                    if (returnColumnNames) {
                        column = new ArrayList();
                        column.add(columnNames[i]);
                        column.add(columnValue(c, i));
                        row.add(column);
                    } else {
                        row.add(columnValue(c, i));
                    }
                }
                rows.add(row);
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
    * Converts column names and values to ContentValues.
    */
    private ContentValues makeContentValues(final YailList columns, final YailList values) {
        String[] cols = columns.toStringArray();
        String[] vals = values.toStringArray();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < cols.length; i++) {
            contentValues.put(cols[i], vals[i]);
        }
        return contentValues;
    }
    
    /**
    * Returns an input stream for the specified file.
    * @param fileName The path to the file.
                A prefix of "//" specifies a file in the app's assets.
                A prefix of "/" specifies a file on the external SD card.
                No prefix specifies a path relative to the app's private storage.
      @return an InputStream
    */
    private InputStream openInputStream(final String fileName) throws IOException {
        if (fileName.startsWith("//")) {
            if (isRepl)
                return new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/AppInventor/assets/" + fileName);
            else
                return context.getAssets().open(fileName.substring(2));
        } else
            return new FileInputStream(resolveFileName(fileName));
    }
            
    /**
    * Returns absolute file path.
    * @param filename The path to the file.
                A prefix of "/" specifies a file on the external SD card.
                No prefix specifies a path relative to the app's private storage.
    */
    
    private String resolveFileName(final String fileName) {
        if (fileName.startsWith("/"))
            return Environment.getExternalStorageDirectory().getPath() + fileName;
        File dirPath = context.getFilesDir();
        if (isRepl) {
            String path = Environment.getExternalStorageDirectory().getPath() + "/AppInventor/data/";
            dirPath = new File(path);
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
    
    @SimpleEvent(description = "This event fires when the database is created.")
    public void DatabaseCreated() {
        EventDispatcher.dispatchEvent(this, "DatabaseCreated");
    }
    
    @SimpleEvent(description = "This event fires when the database is upgraded. "
                             + "The previous and new version numbers are provided. "
                             + "Use this event to modify the database as required by the version change."
                )
    public void DatabaseUpgrade(int oldVersion, int newVersion) {
        EventDispatcher.dispatchEvent(this, "DatabaseUpgrade", oldVersion, newVersion);
    }
    
    @SimpleEvent(description = "This event fires when the database is downgraded. "
                             + "The previous and new version numbers are provided. "
                             + "Use this event to modify the database as required by the version change."
                )
    public void DatabaseDowngrade(int oldVersion, int newVersion) {
        EventDispatcher.dispatchEvent(this, "DatabaseDowngrade", oldVersion, newVersion);
    }

    @SimpleEvent(description = "This event fires after an ExecuteSQLAsync call. "
                             + "The tag specified in the original call and the result of the execution are provided."
                )
    public void AfterExecute(String tag, boolean result) {
        EventDispatcher.dispatchEvent(this, "AfterExecute", tag, result);
    }

    @SimpleEvent(description = "This event fires after an ExecuteFileAsync. "
                             + "The tag specified in the original call and the result of the execution are provided."
                )
    public void AfterExecuteFile(String tag, int execCount) {
        EventDispatcher.dispatchEvent(this, "AfterExecuteFile", tag, execCount);
    }

    @SimpleEvent(description = "This event fires after an asynchronous Select call. "
                             + "The tag specified in the original call, the number of returned rows, and the result rows are provided."
                )
    public void AfterSelect(String tag, int rowCount, YailList rows) {
        EventDispatcher.dispatchEvent(this, "AfterSelect", tag, rowCount, rows);
    }
    
    @SimpleEvent(description = "This event fires after an asynchronous Insert call. "
                             + "The tag specified in the original call and the row ID of the new row are provided."
                )
    public void AfterInsert(String tag, long rowId) {
        EventDispatcher.dispatchEvent(this, "AfterInsert", tag, rowId);
    }
    
    @SimpleEvent(description = "This event fires after an asynchronous InsertFile call. "
                             + "The tag specified in the original call and the count of inserted rows are provided."
                )
    public void AfterInsertFile(String tag, int rowCount) {
        EventDispatcher.dispatchEvent(this, "AfterInsertFile", tag, rowCount);
    }
    
    @SimpleEvent(description = "This event fires after an asynchronous Replace call. "
                             + "The tag specified in the original call and the row ID of the new or updated row are provided."
                )
    public void AfterReplace(String tag, long rowId) {
        EventDispatcher.dispatchEvent(this, "AfterReplace", tag, rowId);
    }
    
    @SimpleEvent(description = "This event fires after an asynchronous Update call. "
                             + "The tag specified in the original call and the number of changed rows are provided."
                )
    public void AfterUpdate(String tag, int rowCount) {
        EventDispatcher.dispatchEvent(this, "AfterUpdate", tag, rowCount);
    }
    
    @SimpleEvent(description = "This event fires after an asynchronous Delete call. "
                             + "The tag specified in the original call and the number of deleted rows are provided."
                )
    public void AfterDelete(String tag, int rowCount) {
        EventDispatcher.dispatchEvent(this, "AfterDelete", tag, rowCount);
    }
    
    @SimpleEvent(description = "This event fires when a SQL error occurs. "
                             + "The error message is provided."
                )
    public void SQLError(String message) {
        EventDispatcher.dispatchEvent(this, "SQLError", message);
    }



    
}

