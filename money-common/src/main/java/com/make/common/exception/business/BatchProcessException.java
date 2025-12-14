package com.make.common.exception.business;

import org.springframework.dao.DataAccessException;

public class BatchProcessException extends RuntimeException{
    public BatchProcessException(String errorMsg, DataAccessException dae) {

    }
}
