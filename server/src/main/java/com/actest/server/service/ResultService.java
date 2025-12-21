package com.actest.server.service;

import com.actest.server.model.Result;
import com.actest.server.repository.ResultRepository;

import java.util.List;

public class ResultService {
    private final ResultRepository resultRepository;

    public ResultService() {
        this.resultRepository = new ResultRepository();
    }

    public boolean submitResult(Result result) {
        return resultRepository.saveResult(result);
    }

    public List<Result> getUserResults(int userId) {
        return resultRepository.getResultsByUserId(userId);
    }
}
