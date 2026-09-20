package com.example.lld.warehouse_fulfilment_domain.exception;

public final class TaskAlreadyClaimedException extends WarehouseDomainException {
    private static final long serialVersionUID = 1L;

    public TaskAlreadyClaimedException(String taskId, String workerId) {
        super("Task " + taskId + " is already claimed by " + workerId);
    }
}
