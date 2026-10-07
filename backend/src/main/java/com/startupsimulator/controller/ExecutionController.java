package com.startupsimulator.controller;

import com.startupsimulator.dto.response.ExecutionStateDto;
import com.startupsimulator.service.ExecutionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/startups/{id}/execution")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionEngine executionEngine;

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionStateDto startExecution(@PathVariable Long id) {
        return executionEngine.startExecution(id);
    }

    @PostMapping("/pause")
    public ExecutionStateDto pauseExecution(@PathVariable Long id) {
        return executionEngine.pauseExecution(id);
    }

    @PostMapping("/resume")
    public ExecutionStateDto resumeExecution(@PathVariable Long id) {
        return executionEngine.resumeExecution(id);
    }

    @PostMapping("/step")
    public ExecutionStateDto stepExecution(@PathVariable Long id) {
        return executionEngine.stepExecution(id);
    }

    @PostMapping("/tasks/{taskId}/resolve-blocker")
    public ExecutionStateDto resolveTaskBlocker(@PathVariable Long id, @PathVariable Long taskId) {
        return executionEngine.resolveTaskBlocker(id, taskId);
    }

    @GetMapping("/state")
    public ExecutionStateDto getExecutionState(@PathVariable Long id) {
        return executionEngine.getExecutionState(id);
    }
}
