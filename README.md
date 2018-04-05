# SQLite

[App Inventor](http://appinventor.mit.edu/), [Thunkable](https://thunkable.com/), [AppyBuilder](http://appybuilder.com/), [Makeroid](https://www.makeroid.io/), [Blockly Studio](https://www.blocklystudio.xyz) etc. extension for [SQLite](https://www.sqlite.org/)

[SQLite](https://www.sqlite.org/) is a small, fast, self-contained SQL (Structured Query Language) database engine
built into Android.
[SQL](https://www.w3schools.com/sql/) statements are used to create, select, update, and delete data in one or
more tables. SQL allows for complex relationships between tables and provides an expressive means to
find data stored in a database.

## Contents

* [Features](#features)
* [Download](#download)
* [Donate](#donate)
* [Background](#background)
* [Properties](#properties)
* [Events](#events)
* [Methods](#methods)
    * [General](#general)
    * [Transactions](#transactions)
    * [Data Manipulation](#data-manipulation)
        * [Bind Parameters](#bind-parameters)
* [Samples](#samples)

## Features

* Control over database name
* Import/export entire database
* Convenience methods for common CRUD operations
* In-line and asynchronous versions of data manipulation methods
* Can use parameterized SQL statements (prevents SQL injection)
* Query results returned as proper lists
* Database life cycle events
* Debug messages as Toast messages and/or dialogs
* Begin, commit, and rollback transactions (nested too!)
* Database versioning
* and more!

If you don't see a feature you'd like or need, tell me about it. I can't promise I'll add it but you never know!

## Download

[Download AIX](https://github.com/frdfsnlght/aix-SQLite/raw/master/org.bennedum.SQLite.aix)

## Installation

Installation may depend on which App Inventor flavor you're using, but I think they're all pretty similar.

1. Open the "Designer" tab of your application.
2. At the bottom of the Palette on the left side, open the "Extensions" category.
3. Click the "Import extension" link.
4. Select the aix file you downloaded above and click the "Import" button.
5. Wait a few seconds and you should see a message saying the extension was imported. You should also see a SQLite
   extension appear in the "Extensions" category.
6. Drag the SQLite extension into your app and carry on!

## Donate

If you find this extension useful, please consider donating by clicking the button below.
If you're using this extension in an app you're making money from, please STRONGLY consider donating even more.
The recommended donation is $10 USD, but I'll accept anything you think the extension is worth to you.

[![PayPal](https://www.paypalobjects.com/webstatic/en_US/i/buttons/pp-acceptance-medium.png)](https://paypal.me/frdfsnlght/10)

## Background

Before writing this extension, I'd never done any Android programming. I came upon a need
for a small app that would be a perfect learning experience. Starting small, I came across
MIT's App Inventor and all the variations of that platform. After doing some research, I came
to the conclusion that would be a nice way to get my feet wet.

Without going into details, my small app would benefit greatly from a proper database rather than
the TinyDB or other key/value stores included with App Inventor and it's fellows. Since I have a
deep programming background, SQL doesn't bother me and I was already familiar with SQLite.
I was pleasantly surprised to find SQLite is part of the Android platform.

During my research into App Inventor I discovered the ability to create extensions and all the
various free and paid extensions available. There are already a handful of SQLite extensions
available for the App Inventor platform but the various options didn't meet my needs.
I took the opportunity to learn how to build an App Inventor extension in addition to building my first Android app.

## Properties

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetDBName.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetDBName.png)

This property defines the name of the database file. It's default value is "db.sqlite". Changing the name after
opening a database has no effect on the database. Use this property to change the name before the database
is opened.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetDBVersion.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetDBVersion.png)

This is the "version" of the database. An app will define what version of the database it's compatible with.
When a database is opened and its version doesn't match the this version, either the "DatabaseUpgrade" or
"DatabaseDowngrade" event will be fired to allow you to modify the database to make it compatible.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetReturnColumnNames.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetReturnColumnNames.png)

This setting effects how result rows are returned by the various Select methods. When this property is false,
each element of the list returned by the Select methods will be a simple list of values which represent the
values of the columns selected for each matched row. When this property is true, each element of the 
list returned by the Select methods will be a list of pairs which represent the
name and values of the columns selected for each matched row.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetDebugToast.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetDebugToast.png)

This property turns on or off simple debug messages. These messages are displayed as Toast messages and disappear
after a couple of seconds. Turn this on while debugging your application to see what the database is doing.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetDebugDialog.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetDebugDialog.png)

This property turns on or off simple debug messages. These messages are displayed as dialogs with an OK button.
The messages don't disappear until the dialog is dismissed. Turn this on while debugging your application to see what
the database is doing.

## Events

The events in this section don't include events related to asynchronous methods, which are 
included as part of the description for each of those methods.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseOpened.png)

This event fires after a database has been opened, and possibly created.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseCreated.png)

This event fires after a database has been created because the file didn't exist when it
was opened. This event fires before the DatabaseOpen event.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseUpgrade.png)

This event fires when the DBVersion property value is greater than the existing database version
when it's opened. After the event finishes, the database version is set to match the DBVersion
property.

Use this method to make whatever changes to your database are necessary to make it match the
version your application expects.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseDowngrade.png)

This event fires when the DBVersion property value is less than the existing database version
when it's opened. After the event finishes, the database version is set to match the DBVersion
property. This event will usually never be used except for the rare times when a user
downgrades to an older version of your app.

Use this method to make whatever changes to your database are necessary to make it match the
version your application expects.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseClosed.png)

This event fires when the database is closed.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SQLError.png)

This event fires whenever there is a SQL error. This usually happens when you do something
wrong like trying to select from a table or column that doesn't exist, or commit a transaction
when you haven't started one.

## Methods

### General

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabasePath.png)

Returns the path to the database file, whether or not it exists.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExportDatabase.png)

Makes a byte-for-byte copy of an unopened database file to the specified file.
A prefix of "/" specifies a file on the external SD card.
No prefix specifies a path relative to the app's private storage.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ImportDatabase.png)

Makes a byte-for-byte copy of a specified SQLite database file to the file named by the
DBName property.
A prefix of "//" specifies a file in the app's assets.
A prefix of "/" specifies a file on the external SD card.
No prefix specifies a path relative to the app's private storage.

This method is useful for initializing a database on an app's first run. Simply upload a fully
formed SQLite database file into your app's assets and this function will copy it into place.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DeleteDatabase.png)

Deletes the unopened database file named by the DBName property. Use this method to completely
destroy the database and start over.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseExists.png)

Returns true if the database file named by the DBName property exists, false otherwise.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/OpenDatabase.png)

Opens the database named by the DBName property. If the file doesn't exist, it will be
created and the DatabaseCreated event will be fired.
If the file has a version different than the DBVersion property, either the
DatabaseUpgrade or DatabaseDowngrade events will be fired.
After any of these events are fired, the DatabaseOpened event fill fire last.
Opening an already open database has no effect.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/CloseDatabase.png)

Closes a previously opened database, rolling back any uncommitted transactions,
and fires the DatabaseClosed event. Closing an already closed database has no effect.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/IsDatabaseOpen.png)

Returns true if the database is open, false otherwise.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableCount.png)

Returns the number of tables in the open database.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableExists.png)

Returns true if the named table exists in the open database, false otherwise.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableNames.png)

Returns a list of table names in the open database.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableRowCount.png)

Returns the number of rows in a table in the open database.

### Transactions

Transactions allow an "all or nothing" approach to manipulating data. After a transaction has
been started, all the operations that change data are "remembered" by the database until
they are either "committed" or "rolled back". Committing a transaction tells the database,
"I really meant to do all those things, so save the results now", while rolling back
a transaction means, "forget all those things I told you to do." This, of course, is a greatly
simplified description. Google around for more information. Use of transactions is optional
but is an important tool for ensuring data integrity in your database.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/BeginTransaction.png)

Begins a transaction. Transactions can be nested (transactions inside transactions).
Make sure to call CommitTransation or RollbackTransaction for each opened transaction.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/CommitTransation.png)

Commits (saves) changes made during the transaction to the database.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/RollbackTransaction.png)

Rolls back (discards) changes made during the transaction to the database.

### Data Manipulation

The methods in this section deal with creating, deleting and otherwise manipulating
data in the database. They all have asynchronous versions and corresponding events. Asynchronous methods
perform their work on a background thread so they don't block the main UI thread. This is
important for a good user experience during operations that might take more than a 100 milliseconds
or so. Blocking the main UI thread will make your application appear to "freeze" and become
unresponsive. Users don't like that. Use the asynchronous methods when you need to perform
database operations that would take a while, like selecting, updating, deleting, inserting hundreds
of rows.

When an asynchronous method is available, it will accept the same arguments as the synchronous
version with the addition of a "tag" parameter. The "tag" is an arbitrary string argument you provide
and will be passed to the "After" event that corresponds to the asynchronous method. You can use the
tag to differentiate between multiple results in the event handler.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Execute.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterExecute.png)

These methods execute any arbitrary, non-SELECT SQL statement, optionally binding parameters.
See the section below about [bind parameters](#bind-parameters) for more information.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteFile.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteFileAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterExecuteFile.png)

These methods execute one or more SQL statements contained in a file. The file can contain both SQL statements,
blank lines, and comments. In-line comments start with "--" and end at the end of the line. Multi-line comments
start with "\\&ast;" and end with "&ast;/". Line continuation is also supported by using "\\" as the last character in a broken
line. Each statement can optionally end in a semicolon.
The literal string "\n" will be replaced with an actual newline character in any SQL statement.
Execution stops at the first error. The methods return the number of statements successfully executed.

A file name prefix of "//" specifies a file in the app's assets.
A file name prefix of "/" specifies a file on the external SD card.
No prefix specifies a path relative to the app's private storage.

It is recommended these methods should be used inside a transaction since they can result in
partial execution in the event of an error. Typically, you want all the statements in the file to
work, or none at all.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectSQL.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectSQLAsync.png)

These methods execute a SQL SELECT statement, with optional bind parameters, that returns a list with
zero or more rows of data.
See the section below about [bind parameters](#bind-parameters) for more information.

Although not shown here, the AfterSelect event is fired from the SelectSQLAsync method when the query is complete.

The list returned by these methods has a row element list for each row matched by the query.
The elements in the row element list depend on the ReturnColumnNames property.
When this property is true, the elements in the row element list are themselves lists, each with two elements;
a column name and a column value. When the property is false, the elements in the row element list
are the column values in the same order as the requested columns in the SELECT query.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Select.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterSelect.png)

These methods execute a SQL SELECT statement, with optional bind parameters, that returns a list with
zero or more rows of data.
See the section below about [bind parameters](#bind-parameters) for more information.

This is a convenience method that avoids the need to construct the entire SQL statement. A list if column names
and various query clauses can be provided to simplify the call.

See the SelectSQL method for a description of the returned list.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Insert.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterInsert.png)

These methods execute a SQL INSERT statement that returns the unique row ID of the inserted row.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertFile.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertFileAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterInsertFile.png)

These methods perform a bulk insert of CSV formatted data from a file.
The first non-empty line in the file should be a comma separated list of column names from the target table to insert
values into. The second and subsequent non-empty lines should each be a comma separated list of values.
Each of these lines will result in a new row inserted into the target table.
Line continuation is also supported by using "\\" as the last character in a broken
line. The literal string "\n" will be replaced with an actual newline character.
Execution stops at the first error. The methods return the number of rows successfully inserted.

A file name prefix of "//" specifies a file in the app's assets.
A file name prefix of "/" specifies a file on the external SD card.
No prefix specifies a path relative to the app's private storage.

It is recommended these methods should be used inside a transaction since they can result in
partial execution in the event of an error. Typically, you want all the statements in the file to
work, or none at all.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Replace.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ReplaceAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterReplace.png)

These methods execute a SQL REPLACE statement that returns the unique row ID of the inserted or updated row.
A SQL REPLACE statement means "insert if it doesn't exist, update it if it does".

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Update.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/UpdateAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterUpdate.png)

These methods execute a SQL UPDATE statement, with optional bind parameters, that returns the number of
rows that are updated.
See the section below about [bind parameters](#bind-parameters) for more information.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Delete.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DeleteAsync.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterDelete.png)

These methods execute a SQL DELETE statement, with optional bind parameters, that returns the number of
rows that are deleted. If no whereClause is provided, which means "delete all the rows", a zero is returned.
See the section below about [bind parameters](#bind-parameters) for more information.

#### Bind Parameters

Many of the methods above include whereClause and bindParams arguments. Together, the arguments provide a
simple way to limit the rows operated on and prevent SQL injection attacks. The idea is simple. The whereClause
is a string in the form of a SQL WHERE clause, without the word "WHERE". Any question marks in the whereClause
are automatically replaced by the corresponding value from the bindParams list before the statement is
executed by the database engine. The database engine deals with escaping the values as necessary. This
allows the developer to skip building a complex whereClause string using concatenation.

As an example, suppose we want to select rows from a table like this:

```sql
SELECT * FROM myTable WHERE name = 'Unknown' AND catCount > 10
```

The WHERE clause in this SQL statement is:

```sql
name = 'Unknown' AND catCount > 10
```

We can accomplish the same thing by passing in a whereClause like this:

```sql
name = ? AND catCount > ?
```

And a bindParams list like this:

    ("Unknown", 10)
    
## Samples

Coming soon...
