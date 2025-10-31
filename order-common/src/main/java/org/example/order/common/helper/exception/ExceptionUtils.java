package org.example.order.common.helper.exception;

import java.sql.SQLException;

public class ExceptionUtils {
    public static SQLException findSQLException(Throwable ex) {
        while (ex != null) {
            if (ex instanceof SQLException) return (SQLException) ex;
            ex = ex.getCause();
        }

        return null;
    }
}
