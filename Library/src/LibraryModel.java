/*
 * LibraryModel.java
 * Author:
 * Created on:
 */



import java.sql.*;

import javax.swing.*;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection connection;
    private Statement statement;
    private ResultSet result;

    public LibraryModel(JFrame parent, String userid, String password) {
    	dialogParent = parent;
    	try {
    		//Set Up the connection to the database
    		String url = "jdbc:postgresql://db.ecs.vuw.ac.nz/"+userid+"_jdbc";
    		connection = DriverManager.getConnection(url,userid,password);
    		statement=connection.createStatement();

    	}catch (Exception e) {
    		//Exit if there is an error connection
    		System.out.println("Error Connecting to Datbase. Ensure correct username and password");
    		System.exit(1);
    	}

    }


    /**
     * Lookup a book by the specified isbn
     * @param isbn
     * @return
     */
    public String bookLookup(int isbn) {
    	String item = "Book Lookup: \n";

    	try {
    		//Find the book with that isbn value
    		result = statement.executeQuery("" + "SELECT isbn, title, edition_no, numofcop, numleft FROM book WHERE isbn = " + isbn);
    		if (!result.isBeforeFirst()) {
    			return item + "\tNo such ISBN: " + isbn;
    		}
    		while (result.next()) {
    			isbn = result.getInt("isbn");
    			String title = result.getString("title");
    			int edition = result.getInt("edition_no");
    			int numCop = result.getInt("numofcop");
    			int copLeft = result.getInt("numleft");
    			item = item + String.format("\t%d: %s\n" + "\tEdition: %d - Number of copies: %d - Copies Left: %d\n", isbn,title,edition,numCop,copLeft);
    		}

    		//Get the authors for the book
    		result = statement.executeQuery("SELECT surname FROM book_author NATURAL JOIN author NATURAL join book WHERE isbn = " + isbn + " ORDER BY authorseqno");

    		if (!result.isBeforeFirst()) {
    			item = item + ("\t(No Authors) \n");
    		} else {
    			String authors = "\tAuthors: ";
    			while (result.next()) {
    				authors = authors + result.getString(1).trim() + ", ";
    			}

    			authors = authors.substring(0, authors.length()-2);
    			authors = authors + "\n";
    			item = item + authors;
    		}

    		if (item.equals("Book Lookup: \n")) {
    			return "No such ISBN:" + isbn;
    		} else {
    			return item;
    		}
    	} catch (Exception e) {
    		System.out.println("Error in Book Lookup");
    	}

    	return "No such ISBN: " + isbn;

    }
    /**
     * Show the catalogue of all the books in the database
     * @return
     */
    public String showCatalogue() {
    	String catalogue = "Show Catalogue: \n\n";

    	try {
    		//Get all the books
    		result = statement.executeQuery("" + "SELECT isbn, title, edition_no, numofcop, numleft FROM book ORDER BY isbn");

    		while (result.next()) {
    			int isbn = result.getInt("isbn");
    			String title = result.getString("title");
    			int edition = result.getInt("edition_no");
    			int numCop = result.getInt("numofcop");
    			int copLeft = result.getInt("numleft");
    			catalogue = catalogue + String.format("%d: %s\n" + "\tEdition: %d - Number of copies: %d - Copies Left: %d\n", isbn,title,edition,numCop,copLeft);

    			//Get the authors for the book currently on
    			PreparedStatement ps = connection.prepareStatement("SELECT surname FROM book_author NATURAL JOIN author NATURAL join book WHERE isbn = " + isbn + "ORDER BY authorseqno");
        		ResultSet r = ps.executeQuery();

        		if (!r.isBeforeFirst()) {
        			catalogue = catalogue + ("\t(No Authors) \n");
        		} else {
        			String authors = "\tAuthors: ";
        			while (r.next()) {
        				authors = authors + r.getString(1).trim() + ", ";
        			}

        			authors = authors.substring(0, authors.length()-2);
        			authors = authors + "\n";
        			catalogue = catalogue + authors;
        		}
    		}

    		if (catalogue != "") {
    			return catalogue;
    		}
    	} catch (Exception e) {
    		System.out.println("Error in Catalogue");

    	}

    	return "Database doesn't have a catalogue";
    }

    /**
     * Show all the books that are currently out for loan
     * @return
     */
    public String showLoanedBooks() {
    	String loanedBooks = "Show Loaned Books: \n\n";

    	try {
    		//Get all the loaned books
    		result = statement.executeQuery("SELECT isbn FROM cust_book");

    		while (result.next()) {
    			int isbn = result.getInt("isbn");
    			//Get the list of the customers that have this book on loan
    			PreparedStatement ps = connection.prepareStatement("SELECT * FROM cust_book NATURAL JOIN customer WHERE isbn = " + isbn);
    			ResultSet r = ps.executeQuery();
    			String bookLookup = bookLookup(isbn);
    			loanedBooks = loanedBooks + bookLookup.substring(15, bookLookup.length());
        		if (!r.isBeforeFirst()) {
        			loanedBooks = loanedBooks + ("(No Borrowers) \n");
        		} else {
        			String b = "\tBorrowers: \n";

        			while (r.next()) {
        				int customerId = r.getInt(1);
        				//Get the customer information for the customer that has this book on loan
        				PreparedStatement pstat = connection.prepareStatement("SELECT * FROM customer WHERE CustomerID = " + customerId);
            			ResultSet res = pstat.executeQuery();

            			while(res.next()) {
            				String lName = res.getString(2).trim();
            				String fName = res.getString(3).trim();
            				String city = res.getString(4);
            				if (city == null) {
            					city = "(no city)";
            				} else {
            					city = city.trim();
            				}
            				b = b + "\t\t" + customerId + ": " + lName + ", " + fName + " - " + city + "\n";
            			}
        			}
        			b = b + " \n";
        			loanedBooks = loanedBooks + b;
        		}

    		}


    		if (loanedBooks.equals("Show Loaned Books: \n\n")) {
    			//If there is no books loaned out
    			return loanedBooks + "(No Loaned Books) \n";
    		} else {
    			return loanedBooks;
    		}
    	} catch (Exception e) {
    		System.out.println("Error in Loaned Books Lookup");
    	}

    	return loanedBooks + "(No Loaned Books) \n";
    }

    /**
     * Show the information for a specified author
     * @param authorID
     * @return
     */
    public String showAuthor(int authorID) {
    	String books = "";
        String author = "Show Author:\n";
        String name = "";
        String surname = "";
        int isbn = -1;
        String title = "";
        try {
            statement = connection.createStatement();
            //Get the author's information
            result = statement.executeQuery("SELECT name, surname, isbn, title FROM author NATURAL JOIN book_author NATURAL JOIN book WHERE authorId = " + authorID + ";");
            if (!result.isBeforeFirst() ) {
            	//Check if there is no result before the author hasn't written a book or because they don't exist
            	ResultSet r = statement.executeQuery("SELECT name, surname FROM author WHERE authorId = " + authorID + ";");
            	if (!r.isBeforeFirst()) {
            		//This means that the author doesn't exist
            		author = author + "No such author ID: " + authorID + "\n";
            	} else {
            		//This means that author just hasn't written any books but does exist
            		while (r.next()) {
                        name = r.getString(1).trim();
                        surname = r.getString(2).trim();
                    }
            		author = "Show Author:\n \t" + authorID + " - " + name + " " + surname + "\n" + "\t(no books written)" + "\n";
            	}

            }else {
            	int count = 0;
                while (result.next()) {
                    name = result.getString(1).trim();
                    surname = result.getString(2).trim();
                    isbn = result.getInt(3);
                    title = result.getString(4);
                    books += "\t\t" + isbn + " - " + title + "\n";
                    count++;
                }
                //Change if there is an s after book if the author has written multiple
                if (count == 1) {
                	books = "\t Book Written:\n" + books;
                } else if (count > 1) {
                	books = "\t Books Written:\n" + books;
                }
                author = "Show Author:\n \t" + authorID + " - " + name + " " + surname + "\n" + books + "\n";
            }
        }catch(Exception e){
            System.out.println("Error in showAuthor");
        }
        return author;
    }

    /**
     * Show a list of all the authors
     * @return
     */
    public String showAllAuthors() {
		String authors = "Show All Authors: \n";
		try {
			statement = connection.createStatement();
			//Get all the authors
			result = statement.executeQuery("SELECT * FROM author ORDER BY authorid");
			while (result.next()) {
				int authorNum = result.getInt(1);
				String name = result.getString(2);
				String surname = result.getString(3).trim();

				authors = authors + "\t" + authorNum + ": " + surname + ", " + name + "\n";
			}
		} catch (Exception e) {
			System.out.println("Error in showAllAuthors");
		}

		return authors;
    }
    /**
     * Get the information for a specified customer
     * @param customerID
     * @return
     */
    public String showCustomer(int customerID) {
    	String customer = "Show Customer:\n";
        String surname = "";
        String name = "";
        String city = "";
        int isbn;
        String title ;
        try {
            statement = connection.createStatement();
            //Get the information about the customer
            result = statement.executeQuery("SELECT l_name, f_name, city " +
                    "FROM customer " +
                    "WHERE customerID = " + customerID + ";");
            if (!result.isBeforeFirst() ) {
                return customer + " \tNo such customer ID: " + customerID;
            }

            while (result.next()) {
                surname = result.getString(1).trim();
                name = result.getString(2).trim();
                city = result.getString(3);
                //If there isn't a city enter, write that.
                if (city == null) {
					city = "(no city)";
				} else {
					city = city.trim();
				}
            }
            customer = "Show Customer:\n" + "\t" + customerID + ": " + surname + ", " + name + " - " + city + "\n";

            // Get information about any books the customer has on loan
            result = statement.executeQuery("SELECT isbn, title " +
                    "FROM cust_book " +
                    "NATURAL JOIN book " +
                    "WHERE customerid = " + customerID + ";");

            if (!result.isBeforeFirst() ) {
            	customer = customer + "\t No books borrowed ";
            }else {
            	int count = 0;
            	String bookInfo = "";
                while (result.next()) {
                    isbn = result.getInt(1);
                    title = result.getString(2);
                    bookInfo = bookInfo +  "\t\t" + isbn + " - " + title + "\n";
                    count++;
                }

                if (count == 1) {
                	customer = customer + "\t Book Borrowed:\n" + bookInfo;
                } else if (count > 1) {
                	customer = customer + "\t Books Borrowed:\n" + bookInfo;
                }

            }
        }catch(Exception e){
            System.out.println("Error in showCustomer");
        }
        return customer;
    }

    /**
     * Show the information for all the customers in the database
     * @return
     */
    public String showAllCustomers() {
		String customers = "Show All Customers:\n";
		int customerId = -1;
		String surname = "";
		String name = "";
		String city =  "";

		try {
			statement = connection.createStatement();
			//Get the customer information
			result = statement.executeQuery("SELECT * FROM customer");

			while (result.next()) {
				customerId = result.getInt(1);
				surname = result.getString(2).trim();
				name = result.getString(3).trim();
				city = result.getString(4);
				if (city == null) {
					city = "(no city)";
				}
				customers = customers + "\t" + customerId + ": " + surname + "," + name + " - " + city.trim() + "\n";
			}

		} catch (Exception e) {
			System.out.println("Error in showAllCustomer");
		}

		return customers;
    }

    /**
     * A customer borrowed a specified book from the databases
     * @param isbn
     * @param customerid
     * @param day
     * @param month
     * @param year
     * @return
     */
    public String borrowBook(int isbn, int customerid, int day, int month, int year) {
    	int numLeft = 0;
    	String borrowed = "";
    	String custLName = "";
    	String custFName = "";
    	String bookTitle = "";
    	try {
    		statement.execute("START TRANSACTION READ WRITE ");

    		statement = connection.createStatement();
    		connection.setAutoCommit(false);

    		//Check that the customer hasn't already borrowed that book
    		result = statement.executeQuery("SELECT * FROM cust_book WHERE isbn = " + isbn + " AND customerID = " + customerid + ";");
    		if (result.isBeforeFirst()) {
    			connection.rollback();
    			return "Customer already has that book borrowed";
    		}

    		//Check that the customer exists
    		result = statement.executeQuery("SELECT l_name, f_name FROM customer WHERE customerid = " + customerid + " FOR UPDATE;");
    		if (!result.isBeforeFirst()) {
    			connection.rollback();
    			return "No such customer";
    		} else {
    			while(result.next()) {
	    			custLName = result.getString(1).trim();
	    			custFName = result.getString(2).trim();
    			}
    		}

    		//Check that the book exists
    		result = statement.executeQuery("SELECT numleft, title FROM book WHERE isbn = " + isbn + " FOR UPDATE;");
    		if (!result.isBeforeFirst()) {
    			connection.rollback();
    			return "No such book";
    		} else {
    			while(result.next()) {
    				numLeft = result.getInt(1);
    				bookTitle = result.getString(2).trim();
    			}
    		}

    		//Check that there are copies available to borrow
    		if(numLeft <= 0) {
    			connection.rollback();
    			return "No copies left sorry";
    		}

    		continueLock();
    		String date = year + "-" + month + "-" + day;
    		String[] months = new String[] {"January", "February", "March", "April", "May", "June", "July", "August", "September", "Ocotber", "November", "December"};
    		borrowed = "Borrow Book: \n";
    		borrowed = borrowed + "\tBook: " + isbn + " (" + bookTitle + ")\n";
    		borrowed = borrowed + "\tLoaned to: " + customerid + " (" + custFName + " " + custLName + ")\n";
    		borrowed = borrowed + "\tDue Date: " + day + " " + months[month] + " " + year + "\n";
    		numLeft--;


    		//Update the customer information with this borrowed book
    		statement.execute("INSERT INTO cust_book VALUES (" + isbn + ", '" + date + "'," + customerid + ");");

    		//Update the book information that a book has been loaned
    		statement.executeUpdate("UPDATE book SET numLeft = " + numLeft + " WHERE isbn = " + isbn + ";");
    		statement.execute("COMMIT");
    		connection.commit();
    		connection.setAutoCommit(true);

    	} catch (SQLException e) {
    		try {
    			connection.rollback();
    		} catch (SQLException e1) {
    			System.out.println("Error rolling back when borrowing");
    		}
    		System.out.println("Error borrowing a book");
    	}
    	return borrowed;
    }

    /**
     * A customer returns a specified book
     * @param isbn
     * @param customerid
     * @return
     */
    public String returnBook(int isbn, int customerid) {
    	int numLeft = 0;
        String returned = "";
        try {
            statement.execute("START TRANSACTION READ WRITE ");

            statement = connection.createStatement();
            connection.setAutoCommit(false);
            System.out.println();

            //Check that customer has previously borrowed the book
            result = statement.executeQuery("SELECT * FROM cust_book WHERE isbn = " + isbn + " AND customerID = " + customerid + " FOR UPDATE;");
            if (!result.isBeforeFirst()) {
                connection.rollback();
                return "No such book borrowed by that customer";
            }

            //Count how many copies of the book are currently available.
            result = statement.executeQuery("SELECT numleft FROM book WHERE isbn = " + isbn + " FOR UPDATE;");
            while(result.next()) {
                numLeft = result.getInt(1);
            }

            numLeft++;
            //Remove the loaned book from the customer information
            statement.execute("DELETE FROM cust_book WHERE isbn = " + isbn + " AND customerID = " + customerid + ";");

            //Return the book and update how many copies are available
            statement.executeUpdate("UPDATE book SET numLeft = " + numLeft + " WHERE isbn = " + isbn + ";");

            statement.execute("COMMIT");
            connection.commit();
            returned = "Return Book: \n";
            returned = returned + "Book " + isbn + " returned for customer " + customerid + "\n";
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                System.out.println("Error rolling back when returning the book");
            }
            System.out.println("Error returning book");
        }
        return returned;
    }

    /**
     * Close the Database Connection
     */
    public void closeDBConnection() {
    	try{
            connection.close();
        }
        catch(Exception e) {
            System.out.println("Error in closeDBConnection");
        }
    }

    /**
     * Delete a specified customer from the database
     * @param customerID
     * @return
     */
    public String deleteCus(int customerID) {
    	String customer = "";
    	try {
    		//Attempt to delete the customer
    		PreparedStatement ps = connection.prepareStatement("DELETE FROM customer WHERE customerId = " + customerID);
    		int ret = ps.executeUpdate();
    		//If the returned value is 0, then nothing was deleted, otherwise it was deleted
    		if (ret ==0) {
    			customer = "No customer to delete with ID: " + customerID + "\n";
    		} else {
    			customer = "Deleted customer: " + customerID + "\n";
    		}
    	} catch (Exception e) {
    		//A customer can't be deleted if they currently have loaned books
    		customer = "Can't delete this customer " + customerID + "as they still have books borrowed on their account \n";
    	}

    	return customer;
    }

    /**
     * Delete a specified author from the database
     * @param authorID
     * @return
     */
    public String deleteAuthor(int authorID) {
    	String author = "";
    	try {
    		//Attempt to delete the author
    		PreparedStatement ps = connection.prepareStatement("DELETE FROM author WHERE authorId = " + authorID);
    		int ret = ps.executeUpdate();
    		//If the returned value is 0, then nothing was deleted, otherwise it was deleted
    		if (ret ==0) {
    			author = "No author to delete with ID: " + authorID + "\n";
    		} else {
    			author = "Deleted customer: " + authorID + "\n";
    		}
    	} catch (Exception e) {
    		author = "Error in deleteAuthor";
    	}

    	return author;
    }
    /**
     * Deleted a specified book from the database
     * @param isbn
     * @return
     */
    public String deleteBook(int isbn) {
    	String book = "";
    	try {
    		//Attempt to delete the book
    		PreparedStatement ps = connection.prepareStatement("DELETE FROM book WHERE isbn = " + isbn);
    		int ret = ps.executeUpdate();
    		//If the returned value is 0, then nothing was deleted, otherwise it was deleted
    		if (ret ==0) {
    			book = "No book to delete with ISBN: " + isbn + "\n";
    		} else {
    			book = "Deleted book: " + isbn + "\n";
    		}
    	} catch (Exception e) {
    		//Can't delete a book that is currently loaned out
    		book = "Can't delete book " + isbn + " as there are copies out on loan";
    	}

    	return book;
    }

    /**
     * Pop-up to inform the user that the tuples have been locked.
     */
    private void continueLock() {
    	JOptionPane.showMessageDialog(dialogParent, "Locked the tuples(s), ready to update. Click OK to continue");
    }
}