package liquibase.change;

import liquibase.ChangeSet;
import liquibase.FileOpener;
import liquibase.database.Database;
import liquibase.database.sql.SqlStatement;
import liquibase.database.structure.DatabaseObject;
import liquibase.exception.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Interface all changes (refactorings) implement.
 * <p>
 * <b>How changes are constructed and run when reading changelogs:</b>
 * <ol>
 *      <li>As the changelog handler gets to each element inside a changeSet, it passes the tag name to liquibase.change.ChangeFactory
 *      which looks through all the registered changes until it finds one with matching specified tag name</li>
 *      <li>The ChangeFactory then constructs a new instance of the change</li>
 *      <li>For each attribute in the XML node, reflection is used to call a corresponding set* method on the change class</li>
 *      <li>The correct generateStatements(*) method is called for the current database</li>
 * </ol>
 * <p>
 * <b>To implement a new change:</b>
 * <ol>
 *      <li>Create a new class that implements Change (normally extend AbstractChange)</li>
 *      <li>Implement the abstract generateStatements(*) methods which return the correct SQL calls for each database</li>
 *      <li>Implement the createMessage() method to create a descriptive message for logs and dialogs
 *      <li>Implement the createNode() method to generate an XML element based on the values in this change</li>
 *      <li>Add the new class to the liquibase.change.ChangeFactory</li>
 * </ol>
 * <p><b>Implementing automatic rollback support</b><br><br>
 * The easiest way to allow automatic rollback support is by overriding the createInverses() method.
 * If there are no corresponding inverse changes, you can override the generateRollbackStatements(*) and canRollBack() methods.
 * <p>
 * <b>Notes for generated SQL:</b><br>
 * Because migration and rollback scripts can be generated for execution at a different time, or against a different database,
 * changes you implement cannot directly reference data in the database.  For example, you cannot implement a change that selects
 * all rows from a database and modifies them based on the primary keys you find because when the SQL is actually run, those rows may not longer
 * exist and/or new rows may have been added.
 * <p>
 * We chose the name "change" over "refactoring" because changes will sometimes change functionality whereas true refactoring will not.
 *
 * @see ChangeFactory
 * @see Database
 */
public interface Change {

    /**
     * Returns the name of this change
     *
     * @return the name of the change
     */
    public String getChangeName();

    /**
     * Returns the tag name of this change
     *
     * @return the tag name of the change
     */
    public String getTagName();

    /**
     * Executes the statements in this change against the database passed in the argument
     *
     * @param database the reference to the target {@link Database}
     *                 to which the statements are executed
     * @throws JDBCException if there were problems executing the statements
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     */
    public void executeStatements(Database database) throws JDBCException, UnsupportedChangeException;

    /**
     * Outputs the SQL statements generated by this change
     *
     * @param database the target {@link Database} associated to this change's statements
     * @param writer the target {@link Writer} to which the statements are <i>appended</i>
     * @throws IOException if there were problems appending the statements to the writer
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     */
    public void saveStatements(Database database, Writer writer) throws IOException, UnsupportedChangeException, StatementNotSupportedOnDatabaseException;

    /**
     * Rolls back the statements in this change against the {@link Database} passed as argument
     *
     * @param database the target {@link Database} associated to this change's rollback statements
     * @throws JDBCException if there were problems executing the rollback statements
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     * @throws RollbackImpossibleException if rollback is not supported for this change
     */
    public void executeRollbackStatements(Database database) throws JDBCException, UnsupportedChangeException, RollbackImpossibleException;

    /**
     * Outputs the statements necessary to roll back this change
     *
     * @param database the target {@link Database} associated to this change's rollback statements
     * @param writer writer the target {@link Writer} to which the rollback statements are <i>appended</i>
     * @throws IOException if there were problems appending the rollback statements to the writer
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     * @throws RollbackImpossibleException if rollback is not supported for this change
     */
    public void saveRollbackStatement(Database database, Writer writer) throws IOException, UnsupportedChangeException, RollbackImpossibleException, StatementNotSupportedOnDatabaseException;

    /**
     * Generates the SQL statements required to run the change
     *
     * @param database databasethe target {@link Database} associated to this change's statements
     * @return an array of {@link String}s with the statements
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     */
    public SqlStatement[] generateStatements(Database database) throws UnsupportedChangeException;

    /**
     * Generates the SQL statements required to roll back the change
     *
     * @param database database databasethe target {@link Database} associated to this change's rollback statements
     * @return an array of {@link String}s with the rollback statements
     * @throws UnsupportedChangeException if this change is not supported by the {@link Database} passed as argument
     * @throws RollbackImpossibleException if rollback is not supported for this change
     */
    public SqlStatement[] generateRollbackStatements(Database database) throws UnsupportedChangeException, RollbackImpossibleException;

    /**
     * Can this change be rolled back
     *
     * @return <i>true</i> if rollback is supported, <i>false</i> otherwise
     */
    public boolean canRollBack();

    /**
     * Confirmation message to be displayed after the change is executed
     *
     * @return a {@link String} containing the message after the change is executed
     */
    public String getConfirmationMessage();

    /**
     * Creates an XML element (of type {@link Element}) of the change object, and adds it
     * to the {@link Document} object passed as argument
     *
     * @param currentChangeLogDOM the current {@link Document} where this element is being added
     * @return the {@link Element} object created
     */
    public Element createNode(Document currentChangeLogDOM);

    /**
     * Calculates the MD5 hash for the string representation of the XML element
     * of this change
     *
     * @return the MD5 hash
     */
    public String getMD5Sum();
    
    /**
     * Sets the fileOpener that should be used for any file loading and resource
     * finding for files that are provided by the user.
     */
    public void setFileOpener(FileOpener fileOpener);
    
    /**
     * This method will be called after the no arg constructor and all of the
     * properties have been set to allow the task to do any heavy tasks or
     * more importantly generate any exceptions to report to the user about
     * the settings provided.
     * 
     */
    public void setUp() throws SetupException;

    public Set<DatabaseObject> getAffectedDatabaseObjects();

    public ChangeSet getChangeSet();

    public void setChangeSet(ChangeSet changeSet);
}
