package com.kiroule.example.restwebapp.dao;

import com.google.common.base.Optional;
import com.kiroule.example.restwebapp.domain.Book;
import com.kiroule.example.restwebapp.domain.builder.BookBuilder;
import com.kiroule.example.restwebapp.util.Utils;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Igor Baiborodine
 */
public class BookDao {

    private static final Logger logger = LoggerFactory.getLogger(BookDao.class);

    private MongoClient mongoClient;
    private String dbName;
    private String collectionName;
    private DBCollection booksCollection;

    public BookDao() {}

    public void init() throws UnknownHostException {
        DB bookshelfDatabase = mongoClient.getDB(dbName);
        booksCollection = bookshelfDatabase.getCollection(collectionName);
    }

    @Nonnull
    public Optional<Book> create(@Nonnull final Book bookToCreate) {
        checkNotNull(bookToCreate, "Argument[bookToCreate] must not be null");

        Book book = new BookBuilder(bookToCreate.getIsbn()).build(bookToCreate);
        book.set_id(book.getIsbn());

        try {
            booksCollection.insert(Utils.convertPOJOtoDBObject(book));
            logger.info("Added new book{}", bookToCreate);
            return Optional.of(bookToCreate);
        } catch (MongoException.DuplicateKey e) {
            logger.info("Book with isbn[{}] already exists", bookToCreate.getIsbn());
            return Optional.absent(); // book already exists
        }
    }

    @Nonnull
    public List<Book> readAll() {
        final List<Book> books = new ArrayList<Book>();
        DBCursor cursor = booksCollection.find();

        try {
            while (cursor.hasNext()) {
                DBObject dbObject = cursor.next();
                books.add((Book) Utils.convertDBObjectToPOJO(dbObject, Book.class));
            }
        } finally {
            cursor.close();
        }
        logger.info("Retrieved [{}] books", books.size());
        return books;
    }

    @Nonnull
    public Optional<Book> readByIsbn(@Nonnull final String isbn) {
        checkNotNull(isbn, "Argument[isbn] must not be null");

        DBObject query = new BasicDBObject("isbn", isbn);
        DBObject dbObject = booksCollection.findOne(query);

        if (dbObject != null) {
            Book book = (Book) Utils.convertDBObjectToPOJO(dbObject, Book.class);
            logger.info("Retrieved book for isbn[{}]:{}", isbn, book);
            return Optional.of(book);
        }
        logger.info("Book with isbn[{}] does not exist", isbn);
        return Optional.absent();
    }

    @Nonnull
    public Optional<Book> update(@Nonnull final Book bookToUpdate) {
        checkNotNull(bookToUpdate, "Argument[bookToUpdate] must not be null");

        DBObject query = new BasicDBObject("_id", bookToUpdate.getIsbn());
        WriteResult result = booksCollection.update(
                query, Utils.convertPOJOtoDBObject(bookToUpdate));

        if (result.getN() == 1) {
            logger.info("Updated book with isbn[{}]", bookToUpdate.getIsbn());
            return Optional.of(bookToUpdate);
        }
        logger.info("Book with isbn[{}] does not exist");
        return Optional.absent(); // book does not exist
    }

    public boolean delete(@Nonnull final Book bookToDelete) {
        checkNotNull(bookToDelete, "Argument[bookToDelete] must not be null");

        DBObject query = new BasicDBObject("_id", bookToDelete.getIsbn());
        WriteResult result = booksCollection.remove(query);

        if (result.getN() == 1) {
            logger.info("Deleted book with isbn[{}]", bookToDelete.getIsbn());
            return true;
        }
        logger.info("Book with isbn[{}] does not exist", bookToDelete.getIsbn());
        return false;
    }

    public void setMongoClient(final MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public void setDbName(final String dbName) {
        this.dbName = dbName;
    }

    public void setCollectionName(final String collectionName) {
        this.collectionName = collectionName;
    }
}
