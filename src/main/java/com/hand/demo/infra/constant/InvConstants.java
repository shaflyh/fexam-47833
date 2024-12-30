package com.hand.demo.infra.constant;

public final class InvConstants {

    private InvConstants() {
        // Private constructor to prevent instantiation
    }

    /**
     * Code Rules
     */
    public static final class CodeRules {
        public static final String INV_COUNT_NUMBER = "INV.COUNTING33.COUNT_NUMBER";
    }

    /**
     * Status Constants for Counting Orders
     */
    public static final class CountStatus {
        public static final String DRAFT = "DRAFT";
        public static final String IN_COUNTING = "INCOUNTING";
        public static final String PROCESSING = "PROCESSING";
        public static final String WITHDRAWN = "WITHDRAWN";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String CONFIRMED = "CONFIRMED";
    }

    /**
     * Count Dimension Constants
     */
    public static final class CountDimension {
        public static final String SKU = "SKU";
        public static final String LOT = "LOT";
    }

    /**
     * Count Type Constants
     */
    public static final class CountType {
        public static final String MONTH = "MONTH";
        public static final String YEAR = "YEAR";
    }

    /**
     * Count Mode Constants
     */
    public static final class CountMode {
        public static final String VISIBLE_COUNT = "VISIBLE_COUNT";
        public static final String UNVISIBLE_COUNT = "UNVISIBLE_COUNT";
    }

    /**
     * WMS Synchronization
     */
    public static final class WmsSyncStatus {
        public static final String SUCCESS = "SUCCESS";
        public static final String ERROR = "ERROR";
        public static final String SKIP = "SKIP";
    }

    /**
     * Workflow Configuration Keys
     */
    public static final class WorkflowConfig {
        public static final String IS_WORKFLOW = "FEXAM95.INV.COUNTING.ISWORKFLOW";
    }

    /**
     * Error Messages
     */
    public static final class ErrorMessages {
        public static final String INVALID_COUNT_STATUS = "Status '%s' is not valid for the operation. Allowed statuses: %s.";
        public static final String COUNT_STATUS_CANNOT_BE_UPDATED = "Invoice count status cannot be updated";
        public static final String INVALID_SUPERVISOR = "Only supervisors can perform this operation.";
        public static final String INVALID_DOCUMENT_CREATOR = "Document in draft status can only be modified by the document creator.";
        public static final String EMPTY_COUNT_QUANTITY = "There are data rows with empty count quantity. Please check the data.";
        public static final String MISSING_REASON_FOR_DIFFERENCE = "Counting difference detected. Please provide a reason.";
        public static final String INVALID_STATUS_UPDATE = "Only '%s', '%s', '%s', and '%s' statuses can be modified";
        public static final String WMS_SUPERVISOR_ONLY = "The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate";
        public static final String INVALID_USER_ROLE_FOR_OPERATION = "Only the document creator, counter, or supervisor can modify the document for the status of in counting, rejected, or withdrawn.";
        public static final String ORDER_EXECUTION_FAILED = "Counting order execution failed: ";
        public static final String ONLY_DRAFT_STATUS_EXECUTE = "Only draft status can execute";
        public static final String ONLY_DOCUMENT_CREATOR_EXECUTE = "Only the document creator can execute";
        public static final String COMPANY_DOES_NOT_EXIST = "Company does not exist";
        public static final String DEPARTMENT_DOES_NOT_EXIST = "Department does not exist";
        public static final String WAREHOUSE_DOES_NOT_EXIST = "Warehouse does not exist";
        public static final String UNABLE_TO_QUERY_ON_HAND_QUANTITY = "Unable to query on hand quantity data.";
        public static final String INVALID_DATE_FORMAT = "Invalid date format: %s";
    }

    public static final class Messages {
        public static final String ORDER_EXECUTION_SUCCESSFUL = "All validation successful. Orders executed.";
        public static final String ORDER_SAVE_SUCCESSFUL = "All validation successful. Orders saved.";
        public static final String ORDER_REMOVE_SUCCESSFUL = "All validation successful. Orders deleted.";
    }

    /**
     * Validation
     */
    public static final class Validation {
        public static final String COUNT_TIME_FORMAT_MONTH = "yyyy-MM";
        public static final String COUNT_TIME_FORMAT_YEAR = "yyyy";
    }

    /**
     * API Configuration
     */
    public static final class Api {
        public static final String WMS_NAMESPACE = "HZERO";
        public static final String WMS_SERVER_CODE = "FEXAM_WMS";
        public static final String WMS_INTERFACE_CODE = "fexam-wms-api.thirdAddCounting";
    }

    /**
     * Miscellaneous
     */
    public static final class Misc {
        public static final String DEFAULT_DEL_FLAG = "0";
    }

    public static final class ExtraKeys {
        public static final String WMS_SYNC_STATUS = "wms_sync_status";
        public static final String WMS_SYNC_ERROR_MESSAGE = "wms_sync_error_message";
    }
}
