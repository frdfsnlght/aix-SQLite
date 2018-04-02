# SQLite

[App Inventor](http://appinventor.mit.edu/)/[Thunkable](https://thunkable.com/)/[AppyBuilder](http://appybuilder.com/)/[Makeroid](https://www.makeroid.io/)/etc.
extension for [SQLite](https://www.sqlite.org/)

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

TODO: add link to aix file

## Installation

Installation may depend on which App Inventor flavor you're using, but I think they're all pretty similar.

1. Open the "Designer" tab of your application.
2. At the bottom of the Palette on the left side, open the "Extensions" category.
3. Click the "Import extension" link.
4. Select the aix file you downloaded above and click the "Import" button.
5. Wait a few seconds and you should see a message saying the extension was imported. You should also see a SQLite
   extension appear in the "Extensions" category.
6. Drag the SQLite extension into your app and carry on!

TODO: add information about app size increase


## Donate

If you find this extension useful, please consider donating by clicking the button below.
If you're using this extension in an app you're making money from, please STRONGLY consider donating even more.
The recommended donation is $10 USD, but I'll accept anything you think the extension is worth to you.

TODO: add link here


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

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/GetDBReturnColumnNames.png) ![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SetDBReturnColumnNames.png)

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
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExportDatabase.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ImportDatabase.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DeleteDatabase.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DatabaseExists.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/OpenDatabase.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/CloseDatabase.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/IsDatabaseOpen.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableCount.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableExists.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableNames.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/TableRowCount.png)

### Transactions

Transactions allow an "all or nothing" approach to manipulating data. After a transaction has
been started, all the operations that change data are "remembered" by the database until
they are either "committed" or "rolled back". Committing a transaction tells the database,
"I really meant to do all those things, so save the results now", while rolling back
a transaction means, "forget all those things I told you to do." This, of course, is a greatly
simplified description. Google around for more information. Use of transactions is optional
but is an important tool for ensuring data integrity in your database.

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/BeginTransaction.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/CommitTransation.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/RollbackTransaction.png)

### Data Manipulation

The methods in this section deal with creating, deleting and otherwise manipulating
data in the database. They all have asynchronous versions. Asynchronous methods
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

![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Execute.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterExecute.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteFile.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ExecuteFileAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterExecuteFile.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectSQL.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectSQLAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Select.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/SelectAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterSelect.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Insert.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterInsert.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertFile.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/InsertFileAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterInsertFile.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Replace.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/ReplaceAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterReplace.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Update.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/UpdateAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterUpdate.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/Delete.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/DeleteAsync.png)
![Image](https://github.com/frdfsnlght/aix-SQLite/raw/master/docs/images/AfterDelete.png)









