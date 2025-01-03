package com.hand.demo.app.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hand.demo.api.dto.*;
import com.hand.demo.app.service.*;
import com.hand.demo.domain.entity.*;
import com.hand.demo.domain.repository.*;
import com.hand.demo.infra.constant.InvConstants;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.apache.commons.lang.StringUtils;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.boot.platform.profile.ProfileClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * (InvCountHeader)应用服务
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */
@Service
public class InvCountHeaderServiceImpl implements InvCountHeaderService {
    private final InvCountHeaderRepository invCountHeaderRepository;
    private final InvWarehouseService warehouseService;
    private final InvCountLineService lineService;
    private final InvCountExtraService extraService;
    private final InvStockService stockService;
    private final IamDepartmentService departmentService;
    private final IamCompanyService companyService;
    private final InvMaterialService materialService;
    private final InvBatchService batchService;
    private final ProfileClient profileClient;
    private final InvCountLineRepository lineRepository;
    private final CodeRuleBuilder codeRuleBuilder;
    private final WmsApiService wmsApiService;
    private final Utils utils;
    private static final Logger logger = LoggerFactory.getLogger(InvCountHeaderServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowService workflowService;


    @Autowired
    public InvCountHeaderServiceImpl(InvCountHeaderRepository invCountHeaderRepository, InvWarehouseService warehouseService,
                                     InvCountLineService lineService, InvCountExtraService extraService, InvStockService stockService,
                                     IamDepartmentService departmentService, IamCompanyService companyService, ProfileClient profileClient,
                                     InvMaterialService materialService, InvBatchService batchService, InvCountLineRepository lineRepository,
                                     CodeRuleBuilder codeRuleBuilder, WmsApiService wmsApiService,
                                     Utils utils, WorkflowService workflowService) {
        this.invCountHeaderRepository = invCountHeaderRepository;
        this.warehouseService = warehouseService;
        this.lineService = lineService;
        this.extraService = extraService;
        this.stockService = stockService;
        this.departmentService = departmentService;
        this.companyService = companyService;
        this.profileClient = profileClient;
        this.materialService = materialService;
        this.batchService = batchService;
        this.lineRepository = lineRepository;
        this.codeRuleBuilder = codeRuleBuilder;
        this.wmsApiService = wmsApiService;
        this.utils = utils;
        this.workflowService = workflowService;
    }

    /**
     * API 1: Counting order save (orderSave)
     */
    @Override
    @Transactional
    public InvCountInfoDTO orderSave(List<InvCountHeaderDTO> invCountHeaders) {
        // 1. Counting order save verification method
        InvCountInfoDTO countInfo = manualSaveCheck(invCountHeaders);
        // throw exception if error list not empty
        if (!countInfo.getErrorList().isEmpty()) {
            throw new CommonException("Counting order save failed: " + countInfo.getTotalErrorMsg());
        }
        // 2. Counting order save method
        List<InvCountHeaderDTO> saveResult = manualSave(countInfo.getSuccessList()); // Save or update data if all validation success
        // Update the header info with data after saving
        countInfo.setSuccessList(saveResult);

        // return latest data
        return countInfo;
    }

    /**
     * API 2: Counting order remove (orderRemove)
     */
    @Override
    public InvCountInfoDTO orderRemove(List<InvCountHeaderDTO> invCountHeaders) {
        InvCountInfoDTO checkResult = manualRemoveCheck(invCountHeaders);
        // Check if there are errors
        if (checkResult.getErrorList().isEmpty()) {
            // Delete Invoice Headers if all validation succeeds
            invCountHeaderRepository.batchDeleteByPrimaryKey(new ArrayList<>(invCountHeaders));
            checkResult.setTotalErrorMsg(InvConstants.Messages.ORDER_REMOVE_SUCCESSFUL);
        }
        return checkResult;
    }

    /**
     * API 3.a: Counting order query (list)
     */
    @Override
    public Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeaderDTO invCountHeader) {
        // Add parameter for tenant admin (for data permission rule)
        invCountHeader.setTenantAdminFlag(utils.getUserVO().getTenantAdminFlag() != null);
        // Perform pagination and sorting
        return PageHelper.doPageAndSort(pageRequest, () -> invCountHeaderRepository.selectList(invCountHeader));
    }

    /**
     * API 3.b: Counting order query (detail)
     */
    @Override
    public InvCountHeaderDTO selectDetail(Long countHeaderId) {
        // Fetch the Invoice Header
        InvCountHeaderDTO invCountHeaderDTO = fetchHeaderById(countHeaderId);
        // Populate the header with related data
        populateHeaderDetails(invCountHeaderDTO);

        return invCountHeaderDTO;
    }

    /**
     * 4. Counting order execution (orderExecution)
     */
    @Override
    @Transactional
    public InvCountInfoDTO orderExecution(List<InvCountHeaderDTO> invCountHeaders) {
        // 1. Counting order save verification method
        // 2. Counting order save method: save or update data if all validation success
        List<InvCountHeaderDTO> saveResult = orderSave(invCountHeaders).getSuccessList(); // Just reuse orderSave method

        // 3. Counting order execute verification method
        InvCountInfoDTO executeInfoResult = executeCheck(saveResult);
        // Validation error
        if (!executeInfoResult.getErrorList().isEmpty()) {
            throw new CommonException(InvConstants.ErrorMessages.ORDER_EXECUTION_FAILED + executeInfoResult.getTotalErrorMsg());
        }

        // 4. Counting order execute method
        List<InvCountHeaderDTO> executeResult = execute(executeInfoResult.getSuccessList());

        // 5. Counting order synchronization WMS method
        InvCountInfoDTO syncWmsResult = countSyncWms(executeResult);
        // Throw exception and rollback if error list not empty
        if (!syncWmsResult.getErrorList().isEmpty()) {
            throw new CommonException(InvConstants.ErrorMessages.ORDER_EXECUTION_FAILED + syncWmsResult.getTotalErrorMsg());
        }

        return syncWmsResult;
    }

    /**
     * API 5: Submit counting results for approval (orderSubmit).
     * <p>
     * This method processes the submission of inventory counting orders for approval.
     * Steps:
     * 1. Save the orders using the orderSave method to ensure data integrity.
     * 2. Validate the orders with the submitCheck method to ensure they meet the submission requirements.
     * 3. Process valid orders with the submit method, which either starts a workflow or updates the status.
     */
    @Override
    @Transactional
    public InvCountInfoDTO orderSubmit(List<InvCountHeaderDTO> invCountHeaders) {
        // 1. Save the headers to ensure they are valid and up-to-date
        List<InvCountHeaderDTO> saveResult = orderSave(invCountHeaders).getSuccessList();

        // 2. Perform validation checks on the headers
        InvCountInfoDTO checkResult = submitCheck(saveResult);

        // 3. Submit headers that passed validation
        List<InvCountHeaderDTO> submitResults = submit(checkResult.getSuccessList());

        // Update the result with successfully submitted headers
        checkResult.setSuccessList(submitResults);

        return checkResult;
    }

    /**
     * API 6: Counting result synchronous (countResultSync)
     * Synchronizes the counting result with the database by validating the input data,
     * checking warehouse type, verifying data consistency, and updating the line data.
     *
     * @param countHeaderDTO the counting order header DTO containing input lines
     * @return updated InvCountHeaderDTO with the status and updated line data
     */
    @Override
    @Transactional
    public InvCountHeaderDTO countResultSync(InvCountHeaderDTO countHeaderDTO) {
        // Step 1: Validate all inputs
        List<InvCountLine> inputLines = new ArrayList<>(countHeaderDTO.getCountOrderLineList());
        List<InvCountLineDTO> fetchedLines = lineService.selectListByHeader(countHeaderDTO);

        // Perform validation and handle errors
        String validationMessage = validateResultSyncInput(countHeaderDTO, fetchedLines);
        if (validationMessage != null) {
            // If validation fails, set the error messages and return
            countHeaderDTO.setErrorMsg(validationMessage);
            countHeaderDTO.setStatus(InvConstants.SyncStatus.ERROR);
            return countHeaderDTO;
        }

        // Step 2: Update fetched lines with input data
        updateLineData(fetchedLines, inputLines);

        // Step 3: Batch update the lines in the database
        List<InvCountLine> updatedLines = lineService.resultSyncBatchUpdate(new ArrayList<>(fetchedLines));

        // Step 4: Convert updated lines to DTO and update the header DTO
        countHeaderDTO.setCountOrderLineList(convertLinesToDTOList(updatedLines));
        countHeaderDTO.setStatus(InvConstants.SyncStatus.SUCCESS); // Set status to success

        return countHeaderDTO;
    }

    /**
     * API 7: Counting order report dataset method (countingOrderReportDs)
     * Retrieves and processes the Counting Order Report Dataset.
     *
     * @param invCountHeader Input DTO containing filters and parameters for the report.
     * @return List of enriched InvCountHeaderDTO objects for reporting purposes.
     */
    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeader) {
        // Map codes to IDs before querying the repository
        reportMapCodesToIds(invCountHeader);

        // Fetch list of inventory count headers based on the updated input DTO
        List<InvCountHeaderDTO> invCountHeaderDTOList = invCountHeaderRepository.selectList(invCountHeader);

        // Populates each header DTO with additional details required for the report
        invCountHeaderDTOList.forEach(invCountHeaderDTO -> {
            invCountHeaderDTO.setWarehouseCode(invCountHeader.getWarehouseCode()); // Retain original warehouse code
            invCountHeaderDTO.setDepartmentName(invCountHeader.getDepartmentName());
            populateHeaderReport(invCountHeaderDTO); // Add additional report-specific fields
        });

        return invCountHeaderDTOList;
    }

    @Override
    public InvCountHeaderDTO workflowCallback(Long organizationId, WorkflowEventDTO workflowEventDTO) {
        return processCallback(organizationId, workflowEventDTO);
    }

    /**
     *
     *
     */

    /**
     * Performs the manual save check for the provided headers.
     *
     * @param invCountHeaders List of InvCountHeader entities to check.
     * @return InvCountInfoDTO containing validation results.
     */
    @Override
    public InvCountInfoDTO manualSaveCheck(List<InvCountHeaderDTO> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Separate headers into inserts and updates
        List<InvCountHeaderDTO> insertList = filterHeaders(invCountHeaders, true);
        List<InvCountHeaderDTO> updateList = filterHeaders(invCountHeaders, false);

        // Process all insert operations (skip the validation, directly put into the success list)
        successList.addAll(insertList);

        // Process the validation of all update invoice if there are any
        if (!updateList.isEmpty()) {
            // Fetch existing headers from the repository and map them by their ID for quick access
            Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeadersMap(updateList);

            // Iterate over each header that needs to be validated
            for (InvCountHeader inputHeader : updateList) {
                InvCountHeaderDTO inputHeaderDTO = convertToDTO(inputHeader);
                Long headerId = inputHeaderDTO.getCountHeaderId();

                // Retrieve the corresponding existing header using the header ID
                InvCountHeader existingHeader = existingHeadersMap.get(headerId);
                // Check if header to update exist in the database
                if (existingHeader == null) {
                    // If the existing header is not found, add an error message
                    inputHeaderDTO.setErrorMsg("Existing header not found for ID: " + headerId);
                    errorList.add(inputHeaderDTO);
                    continue; // Skip to the next header
                }

                // Validate the update operation against the existing header
                String validationError = validateSave(inputHeaderDTO, existingHeader);
                if (validationError != null) {
                    // If validation fails, set the error message and add to the error list
                    inputHeaderDTO.setErrorMsg(validationError);
                    errorList.add(inputHeaderDTO);
                } else {
                    // If validation passes, add the fetched header to the success list
                    successList.add(inputHeaderDTO);
                }
            }
        }

        // Populate the response DTO
        populateInvCountInfoDTO(checkResult, errorList, successList);

        return checkResult;
    }

    private InvCountInfoDTO manualRemoveCheck(List<InvCountHeaderDTO> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Fetch existing headers
        Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeadersMap(invCountHeaders);

        for (InvCountHeader header : existingHeadersMap.values()) {
            InvCountHeaderDTO headerDTO = convertToDTO(header); // Copy properties from the entity to the DTO

            // Validate the delete operation against the existing header
            String validationError = validateRemove(headerDTO);
            if (validationError != null) {
                // If validation fails, set the error message and add to the error list
                headerDTO.setErrorMsg(validationError);
                errorList.add(headerDTO);
            } else {
                // If validation passes, add the DTO to the success list
                successList.add(headerDTO);
            }
        }

        // Populate the response DTO
        populateInvCountInfoDTO(invCountInfoDTO, errorList, successList);

        return invCountInfoDTO;
    }

    /**
     * Performs the actual save operation after validation.
     *
     * @param invCountHeaders List of InvCountHeader entities to save.
     */
    @Override
    @Transactional
    public List<InvCountHeaderDTO> manualSave(List<InvCountHeaderDTO> invCountHeaders) {
        List<InvCountHeaderDTO> insertList = filterHeaders(invCountHeaders, true);
        List<InvCountHeaderDTO> updateList = filterHeaders(invCountHeaders, false);

        // Generate count numbers for new headers with Code Rule Builder
        for (InvCountHeader countHeader : insertList) {
            Map<String, String> codeBuilderMap = new HashMap<>();
            codeBuilderMap.put("customSegment", countHeader.getTenantId().toString() + "-");
            String invCountNumber = codeRuleBuilder.generateCode(InvConstants.CodeRules.INV_COUNT_NUMBER, codeBuilderMap);
            countHeader.setCountNumber(invCountNumber);
            // Set default value
            if (countHeader.getCountStatus() == null) {
                countHeader.setCountStatus(InvConstants.CountStatus.DRAFT);
            }
            if (countHeader.getDelFlag() == null) {
                countHeader.setDelFlag(0);
            }
        }
        logger.info("Save and update InvCountHeaders");
        invCountHeaderRepository.batchInsertSelective(new ArrayList<>(insertList));
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(new ArrayList<>(updateList));

        // Update the line method
        for (InvCountHeaderDTO header : invCountHeaders) {
            // Update the line if the line is existed
            if (header.getCountOrderLineList() != null && !header.getCountOrderLineList().isEmpty()) {
                lineRepository.batchUpdateOptional(new ArrayList<>(header.getCountOrderLineList()),
                        InvCountLine.FIELD_UNIT_QTY,
                        InvCountLine.FIELD_COUNTER_IDS,
                        InvCountLine.FIELD_REMARK);
            }
        }

        return fetchExistingHeaders(invCountHeaders);
    }

    /**
     * Filters headers based on whether they are new (insert) or existing (update).
     *
     * @param headers  The list of headers to filter.
     * @param isInsert If true, filters for inserts; else for updates.
     * @return Filtered list of InvCountHeaders.
     */
    private List<InvCountHeaderDTO> filterHeaders(List<InvCountHeaderDTO> headers, boolean isInsert) {
        if (isInsert) {
            return headers.stream().filter(header -> header.getCountHeaderId() == null).collect(Collectors.toList());
        } else {
            return headers.stream().filter(header -> header.getCountHeaderId() != null).collect(Collectors.toList());
        }
    }

    /**
     * Populates the InvCountInfoDTO with error and success lists, and combines error messages.
     *
     * @param invCountInfoDTO The DTO to populate.
     * @param errorList       List of headers with errors.
     * @param successList     List of successfully processed headers.
     */
    private void populateInvCountInfoDTO(InvCountInfoDTO invCountInfoDTO, List<InvCountHeaderDTO> errorList,
                                         List<InvCountHeaderDTO> successList) {
        invCountInfoDTO.setErrorList(errorList);
        invCountInfoDTO.setSuccessList(successList);

        String totalErrorMsg = errorList.stream().map(InvCountHeaderDTO::getErrorMsg).filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        invCountInfoDTO.setTotalErrorMsg(totalErrorMsg);
    }

    /**
     * Converts a list of InvCountHeader entities to a list of InvCountHeaderDTOs.
     *
     * @param headerList List of InvCountHeader entities to convert.
     * @return List of InvCountHeaderDTOs.
     */
    private List<InvCountHeaderDTO> convertToDTOList(List<InvCountHeader> headerList) {
        return headerList.stream().map(header -> {
            InvCountHeaderDTO dto = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, dto); // Copy properties from entity to DTO
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Converts a list of InvCountLine entities to a list of InvCountLineDTOs.
     *
     * @param lineList List of InvCountLine entities to convert.
     * @return List of InvCountLineDTOs.
     */
    private List<InvCountLineDTO> convertLinesToDTOList(List<InvCountLine> lineList) {
        return lineList.stream().map(line -> {
            InvCountLineDTO dto = new InvCountLineDTO();
            BeanUtils.copyProperties(line, dto); // Copy properties from entity to DTO
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Converts InvCountHeader entities to InvCountHeaderDTO.
     *
     * @param headers InvCountHeader to convert.
     * @return InvCountHeaderDTO.
     */
    private InvCountHeaderDTO convertToDTO(InvCountHeader headers) {
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        BeanUtils.copyProperties(headers, invCountHeaderDTO);
        return invCountHeaderDTO;
    }

    /**
     * Converts InvCountHeader entities to InvCountHeaderDTO.
     *
     * @param line InvCountLine to convert.
     * @return InvCountLineDTO.
     */
    private InvCountLineDTO convertLineToDTO(InvCountLine line) {
        InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
        BeanUtils.copyProperties(line, invCountLineDTO);
        return invCountLineDTO;
    }

    /**
     * Fetches existing InvCountHeaders from the repository based on the headers to be updated
     * and maps them by their CountHeaderId for efficient lookup.
     *
     * @param invCountHeaders List of InvCountHeader entities that need to be updated.
     * @return Map of CountHeaderId to InvCountHeader.
     */
    private Map<Long, InvCountHeader> fetchExistingHeadersMap(List<InvCountHeaderDTO> invCountHeaders) {
        // Extract the set of IDs from the update list
        Set<Long> headerIds = invCountHeaders.stream().map(InvCountHeader::getCountHeaderId).collect(Collectors.toSet());

        // Convert the set of IDs to a comma-separated string for the repository query
        String idString = headerIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Fetch the existing headers from the repository using the comma-separated IDs
        List<InvCountHeader> fetchedHeaders = invCountHeaderRepository.selectByIds(idString);

        // Create a map from CountHeaderId to InvCountHeader for quick access during validation
        Map<Long, InvCountHeader> headersMap = new HashMap<>();
        for (InvCountHeader header : fetchedHeaders) {
            headersMap.put(header.getCountHeaderId(), header);
        }
        return headersMap;
    }

    /**
     * Fetches existing InvCountHeaders from the repository based on the headers to be updated
     *
     * @param invCountHeaders List of InvCountHeader DTOs that need to be updated.
     * @return List of CountHeaderId to InvCountHeader.
     */
    private List<InvCountHeaderDTO> fetchExistingHeaders(List<InvCountHeaderDTO> invCountHeaders) {
        // Extract the set of IDs from the update list
        Set<Long> headerIds = invCountHeaders.stream().map(InvCountHeader::getCountHeaderId).collect(Collectors.toSet());

        // Convert the set of IDs to a comma-separated string for the repository query
        String idString = headerIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        // Fetch the existing headers from the repository using the comma-separated IDs
        List<InvCountHeader> fetchedHeaders = invCountHeaderRepository.selectByIds(idString);

        return convertToDTOList(fetchedHeaders);
    }

    /**
     * Validates save or update operation on an InvCountHeaderDTO against the existing InvCountHeader entity.
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param inputHeader    The DTO representing the header to be updated.
     * @param existingHeader The existing header entity from the database.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateSave(InvCountHeaderDTO inputHeader, InvCountHeader existingHeader) {
        // a. Ensure the count status is not being changed (status update not allowed)
        String status = existingHeader.getCountStatus();
        if (!inputHeader.getCountStatus().equals(status)) {
            return InvConstants.ErrorMessages.COUNT_STATUS_CANNOT_BE_UPDATED;
        }

        // b. Check if the current status allows modification
        Set<String> allowedStatusesUpdate = new HashSet<>(Arrays.asList(
                InvConstants.CountStatus.DRAFT,
                InvConstants.CountStatus.IN_COUNTING,
                InvConstants.CountStatus.REJECTED,
                InvConstants.CountStatus.WITHDRAWN
        ));
        if (!allowedStatusesUpdate.contains(status)) {
            return String.format(InvConstants.ErrorMessages.INVALID_STATUS_UPDATE,
                    InvConstants.CountStatus.DRAFT,
                    InvConstants.CountStatus.IN_COUNTING,
                    InvConstants.CountStatus.REJECTED,
                    InvConstants.CountStatus.WITHDRAWN
            );
        }

        // c. Validate the current user based on the document's status and user roles
        Long currentOperator = DetailsHelper.getUserDetails().getUserId(); // Get the ID of the current user
        Long creatorId = existingHeader.getCreatedBy(); // Get the ID of the user who created the document

        // c.1. If the status is 'DRAFT', only the creator can modify the document
        if (InvConstants.CountStatus.DRAFT.equals(status) && !creatorId.equals(currentOperator)) {
            return InvConstants.ErrorMessages.INVALID_DOCUMENT_CREATOR;
        }

        // c.2. If the status is one of 'INCOUNTING', 'REJECTED', or 'WITHDRAWN', perform additional validations
        Set<String> statusWithAdditionalValidation = new HashSet<>(Arrays.asList(
                InvConstants.CountStatus.IN_COUNTING,
                InvConstants.CountStatus.REJECTED,
                InvConstants.CountStatus.WITHDRAWN));
        if (statusWithAdditionalValidation.contains(status)) {
            // Parse supervisor and counter IDs from the existing header
            List<Long> supervisorIds = parseIds(existingHeader.getSupervisorIds());
            List<Long> counterIds = parseIds(existingHeader.getCounterIds());

            // Check if the warehouse is a WMS warehouse
            boolean isWmsWarehouse = warehouseService.isWmsWarehouse(inputHeader.getWarehouseId());

            // If it's a WMS warehouse, only supervisors are allowed to operate
            if (isWmsWarehouse && !supervisorIds.contains(currentOperator)) {
                return "its a WMS warehouse, only supervisors are allowed to operate";
            }

            // c.3. Check if the current user is either a counter, supervisor, or the document creator
            boolean isCounter = counterIds.contains(currentOperator);
            boolean isSupervisor = supervisorIds.contains(currentOperator);
            boolean isCreator = creatorId.equals(currentOperator);

            if (!isCounter && !isSupervisor && !isCreator) {
                return InvConstants.ErrorMessages.INVALID_USER_ROLE_FOR_OPERATION;
            }
        }

        // Check for field that only draft status allows to be updated
        // Validate if fields restricted to DRAFT status are being updated in other statuses.
        if (!status.equals(InvConstants.CountStatus.DRAFT)) {
            String mismatchedField = findMismatchedField(inputHeader, existingHeader);
            if (mismatchedField != null) {
                return String.format("Field '%s' can only be updated in draft status", mismatchedField);
            }
        }

        // Remark can only be updated in DRAFT or IN_COUNTING status.
        if (!status.equals(InvConstants.CountStatus.DRAFT) &&
                !status.equals(InvConstants.CountStatus.IN_COUNTING) &&
                !Objects.equals(inputHeader.getRemark(), existingHeader.getRemark())) {
            return "Remark can only be updated in draft and in counting status";
        }

        // Reason can only be updated in IN_COUNTING or REJECTED status.
        if (!status.equals(InvConstants.CountStatus.IN_COUNTING) &&
                !status.equals(InvConstants.CountStatus.REJECTED) &&
                !Objects.equals(inputHeader.getReason(), existingHeader.getReason())) {
            return "Reason can only be updated in in counting and rejected status";
        }

        // Invoice line validation
        if (CollUtil.isNotEmpty(inputHeader.getCountOrderLineList())) {
            // Only in counting status allows updates, and only counter can modify
            if (!inputHeader.getCountStatus().equals(InvConstants.CountStatus.IN_COUNTING)) {
                return "Order line list can only be updated in INCOUNTING status";
            }
        }

        // All validations passed; no error
        return null;
    }

    /**
     * Finds the first mismatched field between the headerDTO and the existingHeader for not DRAFT status validation.
     *
     * @param inputHeader    The incoming data transfer object.
     * @param existingHeader The existing header to compare against.
     * @return The name of the first mismatched field, or null if all fields match.
     */
    private String findMismatchedField(InvCountHeaderDTO inputHeader, InvCountHeader existingHeader) {
        if (!Objects.equals(inputHeader.getCompanyId(), existingHeader.getCompanyId())) {
            return "companyId";
        }
        if (!Objects.equals(inputHeader.getDepartmentId(), existingHeader.getDepartmentId())) {
            return "departmentId";
        }
        if (!Objects.equals(inputHeader.getWarehouseId(), existingHeader.getWarehouseId())) {
            return "warehouseId";
        }
        if (!Objects.equals(inputHeader.getCountDimension(), existingHeader.getCountDimension())) {
            return "countDimension";
        }
        if (!Objects.equals(inputHeader.getCountType(), existingHeader.getCountType())) {
            return "countType";
        }
        if (!Objects.equals(inputHeader.getCountMode(), existingHeader.getCountMode())) {
            return "countMode";
        }
        if (!Objects.equals(inputHeader.getCountTimeStr(), existingHeader.getCountTimeStr())) {
            return "countTimeStr";
        }
        if (!Objects.equals(inputHeader.getCounterIds(), existingHeader.getCounterIds())) {
            return "counterIds";
        }
        if (!Objects.equals(inputHeader.getSupervisorIds(), existingHeader.getSupervisorIds())) {
            return "supervisorIds";
        }
        if (!Objects.equals(inputHeader.getSnapshotMaterialIds(), existingHeader.getSnapshotMaterialIds())) {
            return "snapshotMaterialIds";
        }
        if (!Objects.equals(inputHeader.getSnapshotBatchIds(), existingHeader.getSnapshotBatchIds())) {
            return "snapshotBatchIds";
        }

        return null; // No mismatched fields
    }

    /**
     * Validates a remove operation on an InvCountHeaderDTO
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param headerDTO The DTO representing the header to be removed.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateRemove(InvCountHeaderDTO headerDTO) {
        // Status verification: only allow draft status to be deleted
        if (!headerDTO.getCountStatus().equals(InvConstants.CountStatus.DRAFT)) {
            return "Only document status draft allowed to be deleted";
        }
        // Validate the current user based on the document's status and user roles
        Long currentOperator = DetailsHelper.getUserDetails().getUserId(); // Get the ID of the current user
        Long creatorId = headerDTO.getCreatedBy(); // Get the ID of the user who created the document
        if (!creatorId.equals(currentOperator)) {
            return "Document can only be deleted by the document creator.";
        }
        // All validations passed; no error
        return null;
    }

    /**
     * Parses a comma-separated string of IDs into a list of Longs.
     *
     * @param ids The comma-separated string of IDs.
     * @return List of Longs representing the IDs. Returns an empty list if the input is null or empty.
     */
    private List<Long> parseIds(String ids) {
        // Check if the input string is null or empty
        if (StringUtils.isBlank(ids)) {
            return Collections.emptyList();
        }

        List<Long> idList = new ArrayList<>();
        for (String id : ids.split(",")) {
            try {
                idList.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException e) {
                // Skip invalid numbers
            }
        }
        return idList; // Return the list of parsed IDs
    }


    /// //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DETAIL
    private InvCountHeaderDTO fetchHeaderById(Long countHeaderId) {
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(countHeaderId);
        // If the record is not found, throw error
        if (invCountHeader == null) {
            throw new CommonException("Invoice with id " + countHeaderId + " not found");
        }
        // Convert to DTO
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        BeanUtils.copyProperties(invCountHeader, invCountHeaderDTO);

        return invCountHeaderDTO;
    }

    /**
     * Populates the InvCountHeaderDTO with related user, material, batch, warehouse, and line details.
     *
     * @param header the InvCountHeaderDTO to populate
     */
    private void populateHeaderDetails(InvCountHeaderDTO header) {
        // Parse and convert counter and supervisor IDs to UserDTO lists
        header.setCounterList(convertIdsToUserDTOs(parseIds(header.getCounterIds())));
        header.setSupervisorList(convertIdsToUserDTOs(parseIds(header.getSupervisorIds())));

        // Populate snapshot materials and batches (it might be null because it's not necessary for header creation)
        String materialIds = header.getSnapshotMaterialIds();
        if (materialIds != null && !materialIds.isEmpty()) {
            header.setSnapshotMaterialList(materialService.convertToMaterialDTOs(materialIds));
        }
        String batchIds = header.getSnapshotBatchIds();
        if (batchIds != null && !batchIds.isEmpty()) {
            header.setSnapshotBatchList(batchService.convertToBatchDTOs(batchIds));
        }

        // Determine if the warehouse is a WMS warehouse
        header.setIsWMSWarehouse(warehouseService.isWmsWarehouse(header.getWarehouseId()));

        // Retrieve and set invoice count lines
        header.setCountOrderLineList(lineService.selectListByHeader(header));
    }

    /**
     * Converts a list of user IDs to a list of UserDTO.
     *
     * @param ids A list of user IDs to convert.
     * @return A list of UserDTO.
     */
    private List<UserDTO> convertIdsToUserDTOs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().map(id -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(id);
            return userDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public InvCountInfoDTO executeCheck(List<InvCountHeaderDTO> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Requery the database based on the input document ID
        List<InvCountHeaderDTO> existingHeaders = fetchExistingHeaders(invCountHeaders);

        // TODO: Collect all id that need to be validate (company, depart, warehouse) and do batch validation

        // Iterate over each header that needs to be validated
        for (InvCountHeaderDTO headerDTO : existingHeaders) {
            // Validate the update operation against the existing header
            String validationError = validateExecute(headerDTO);
            if (validationError != null) {
                // If validation fails, set the error message and add to the error list
                headerDTO.setErrorMsg(validationError);
                errorList.add(headerDTO);
            } else {
                // If validation passes, add the DTO to the success list
                successList.add(headerDTO);
            }
        }

        // Populate the response DTO
        populateInvCountInfoDTO(checkResult, errorList, successList);

        return checkResult;
    }

    /**
     * Validates an execute operation on an InvCountHeaderDTO.
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param headerDTO The DTO representing the header to be executed.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateExecute(InvCountHeaderDTO headerDTO) {
        // a. document status validation: Only draft status can execute
        if (!headerDTO.getCountStatus().equals(InvConstants.CountStatus.DRAFT)) {
            return InvConstants.ErrorMessages.ONLY_DRAFT_STATUS_EXECUTE;
        }

        // b. current login user validation: Only the document creator can execute
        if (!headerDTO.getCreatedBy().equals(DetailsHelper.getUserDetails().getUserId())) {
            return InvConstants.ErrorMessages.ONLY_DOCUMENT_CREATOR_EXECUTE;
        }

        // c. value set validation

        // d. company, department, warehouse validation
        if (companyService.getById(headerDTO.getCompanyId()) == null) {
            return InvConstants.ErrorMessages.COMPANY_DOES_NOT_EXIST;
        }
        if (departmentService.getById(headerDTO.getDepartmentId()) == null) {
            return InvConstants.ErrorMessages.DEPARTMENT_DOES_NOT_EXIST;
        }
        if (warehouseService.getById(headerDTO.getWarehouseId()) == null) {
            return InvConstants.ErrorMessages.WAREHOUSE_DOES_NOT_EXIST;
        }

        // e. on hand quantity validation
        List<InvStock> invStocks = stockService.fetchValidStocks(headerDTO);
        if (CollUtil.isEmpty(invStocks)) {
            return InvConstants.ErrorMessages.UNABLE_TO_QUERY_ON_HAND_QUANTITY;
        }

        // Validate the countTimeStr field of a HeaderDTO object by formatting it based on the countType field.
        String expectedFormat = headerDTO.getCountType().equals("MONTH") ?
                InvConstants.Validation.COUNT_TIME_FORMAT_MONTH :
                InvConstants.Validation.COUNT_TIME_FORMAT_YEAR;
        try {
            // Attempt to parse the countTimeStr with the expected format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(expectedFormat);
            formatter.parse(headerDTO.getCountTimeStr());
        } catch (DateTimeParseException e) {
            // Catch and return the error if it different from expected format
            return String.format(InvConstants.ErrorMessages.INVALID_DATE_FORMAT, headerDTO.getCountTimeStr());
        }

        // All validations passed; no error
        return null;
    }

    /**
     * Executes the counting process for the provided headers.
     * <p>
     * Steps:
     * 1. Update the status of headers to "INCOUNTING".
     * 2. Retrieve summarized stock data for each header.
     * 3. Generate line items based on summarized stock data.
     * 4. Persist the generated lines to the database.
     * 5. Update header statuses in the database.
     * 6. Convert updated headers to DTOs with attached lines for the final result.
     *
     * @param invCountHeaders List of InvCountHeader entities to process.
     * @return List of InvCountHeaderDTOs containing updated headers and associated lines.
     */
    public List<InvCountHeaderDTO> execute(List<InvCountHeaderDTO> invCountHeaders) {
        // Step 1: Process each header
        List<InvCountLine> allGeneratedLines = new ArrayList<>();
        for (InvCountHeaderDTO header : invCountHeaders) {
            // Update the status to "INCOUNTING"
            header.setCountStatus(InvConstants.CountStatus.IN_COUNTING);

            // Step 2: Retrieve summarized stock data
            List<InvStockSummaryDTO> summarizedStocks = stockService.selectStockSummary(header);

            // Step 3: Generate line data
            List<InvCountLine> generatedLines = lineService.generateInvLines(summarizedStocks, header);
            allGeneratedLines.addAll(generatedLines);
        }

        // Step 4: Persist generated lines to the database
        List<InvCountLine> savedLines = lineRepository.batchInsertSelective(allGeneratedLines);

        // Step 5: Update header statuses
        List<InvCountHeader> updatedHeaders = invCountHeaderRepository.batchUpdateOptional(
                new ArrayList<>(invCountHeaders), InvCountHeader.FIELD_COUNT_STATUS);

        // Step 6: Attach lines to headers and return
        return attachLinesToHeaders(convertToDTOList(updatedHeaders), savedLines);
    }

    // Attach lines to their respective headers
    private List<InvCountHeaderDTO> attachLinesToHeaders(List<InvCountHeaderDTO> headerDTOS, List<InvCountLine> lines) {
        for (InvCountHeaderDTO headerDTO : headerDTOS) {
            List<InvCountLineDTO> lineDTOS = lines.stream()
                    .filter(line -> line.getCountHeaderId().equals(headerDTO.getCountHeaderId()))
                    .map(this::convertLineToDTO)
                    .collect(Collectors.toList());
            headerDTO.setCountOrderLineList(lineDTOS);
        }

        return headerDTOS;
    }

    /**
     * Synchronizes the counting orders with the WMS system.
     * <p>
     * This method performs the following steps:
     * 1. Validates if the warehouse exists for each counting order.
     * 2. Checks if the warehouse is a WMS warehouse and pushes the order to the WMS system.
     * 3. Updates the headers with the WMS order code and stores synchronization results in the database.
     *
     * @param invCountHeaders List of counting headers to synchronize with WMS.
     * @return InvCountInfoDTO containing the result of the synchronization, including updated headers.
     * @throws CommonException if validation or API calls fail.
     */
    @Override
    public InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Iterate over each counting header to process synchronization
        for (InvCountHeaderDTO headerDTO : invCountHeaders) {
            Long tenantId = headerDTO.getTenantId();
            Long warehouseId = headerDTO.getWarehouseId();
            Long countHeaderId = headerDTO.getCountHeaderId();

            // Validate warehouse existence by tenantId and warehouseCode
            InvWarehouse warehouse = warehouseService.validateWarehouse(tenantId, warehouseId);

            // Initialize synchronization extras
            InvCountExtra syncStatusExtra;
            InvCountExtra syncMsgExtra;
            // Fetch existing extras or initialize if none found
            List<InvCountExtra> fetchedExtras = extraService.fetchExtrasByHeaderId(countHeaderId);
            if (fetchedExtras.isEmpty()) {
                // Initialize synchronization extras
                syncStatusExtra = extraService.createExtra(tenantId, countHeaderId, InvConstants.ExtraKeys.WMS_SYNC_STATUS);
                syncMsgExtra = extraService.createExtra(tenantId, countHeaderId, InvConstants.ExtraKeys.WMS_SYNC_ERROR_MESSAGE);
            } else {
                syncStatusExtra = fetchedExtras.get(0);
                syncMsgExtra = fetchedExtras.get(1);
            }

            // Check if the warehouse is a WMS warehouse and call the WMS interface to synchronize the counting order
            if (warehouse.getIsWmsWarehouse().equals(1)) { // warehouse is WMS
                // Set employee number from the current user for interface parameter
                headerDTO.setEmployeeNumber(utils.getUserVO().getLoginName());

                // Serialize header to JSON
                String headerJson = serializeHeader(headerDTO);

                // Call WMS API and handle response
                Map<String, Object> responseBody = wmsApiService.callWmsApiPushCountOrder(
                        InvConstants.Api.WMS_NAMESPACE,
                        InvConstants.Api.WMS_SERVER_CODE,
                        InvConstants.Api.WMS_INTERFACE_CODE,
                        headerJson);

                String responseError = wmsApiService.validateResponses(responseBody);

                // Check returnStatus and handle logic
                String returnStatus = (String) responseBody.get("returnStatus");
                if ("S".equals(returnStatus)) { // Success case
                    syncStatusExtra.setProgramValue(InvConstants.WmsSyncStatus.SUCCESS);
                    syncMsgExtra.setProgramValue("");
                    // Record WMS document number
                    headerDTO.setRelatedWmsOrderCode((String) responseBody.get("code"));
                } else if ("E".equals(returnStatus)) { // Error case
                    syncStatusExtra.setProgramValue(InvConstants.WmsSyncStatus.ERROR);
                    syncMsgExtra.setProgramValue((String) responseBody.get("returnMsg"));
                } else { // Error when can't get returnMsg (interface server is down)
                    syncStatusExtra.setProgramValue(InvConstants.WmsSyncStatus.ERROR);
                    syncMsgExtra.setProgramValue(responseError);
                }
            } else {
                // Not a WMS warehouse
                syncStatusExtra.setProgramValue(InvConstants.WmsSyncStatus.SKIP);
                syncMsgExtra.setProgramValue("");
            }

            // Check for success validation (success message extra is empty when no error)
            if (!syncMsgExtra.getProgramValue().isEmpty()) {
                headerDTO.setErrorMsg(syncMsgExtra.getProgramValue());
                errorList.add(headerDTO);
            } else {
                successList.add(headerDTO);
            }

            // Save or update synchronization extras
            extraService.saveData(Arrays.asList(syncStatusExtra, syncMsgExtra));
        }

        // Update headers (for WMS order code) and return results
        List<InvCountHeader> updatedHeaders = invCountHeaderRepository.batchUpdateOptional(
                new ArrayList<>(invCountHeaders), InvCountHeader.FIELD_RELATED_WMS_ORDER_CODE);

        populateInvCountInfoDTO(checkResult, errorList, successList);
        return checkResult;
    }


    /**
     * Serializes the counting header to a JSON string.
     *
     * @param header The counting header.
     * @return JSON representation of the header.
     * @throws CommonException If serialization fails.
     */
    private String serializeHeader(InvCountHeaderDTO header) {
        try {
            return objectMapper.writeValueAsString(header);
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to serialize header to JSON.", e);
        }
    }

    /**
     * Validates the inputs for the counting result synchronization process.
     * Checks the existence of the counting order header, warehouse type, and data consistency.
     *
     * @param invCountHeaderDTO the counting order header DTO containing input lines
     * @param fetchedLines      the list of lines fetched from the database
     * @return a string containing the error message if validation fails; null if validation passes
     */
    private String validateResultSyncInput(InvCountHeaderDTO invCountHeaderDTO, List<InvCountLineDTO> fetchedLines) {

        // Retrieve the counting order header from the database
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(invCountHeaderDTO.getCountHeaderId());
        if (invCountHeader == null) {
            return InvConstants.ErrorMessages.COUNT_HEADER_NOT_FOUND;
        }

        // Validate whether the warehouse is a WMS warehouse
        boolean isWmsWarehouse = warehouseService.isWmsWarehouse(invCountHeader.getWarehouseId());
        if (!isWmsWarehouse) {
            return InvConstants.ErrorMessages.INVALID_WAREHOUSE;
        }

        // Validate consistency of line data (input size must match existing size)
        if (fetchedLines.size() != invCountHeaderDTO.getCountOrderLineList().size()) {
            return InvConstants.ErrorMessages.LINE_DATA_INCONSISTENT_SIZE;
        }
        // Validate input lineId is match with database line id
        Set<Long> inputLineIds = invCountHeaderDTO.getCountOrderLineList().stream()
                .map(InvCountLineDTO::getCountLineId)
                .collect(Collectors.toSet());
        Set<Long> fetchedLineIds = fetchedLines.stream()
                .map(InvCountLineDTO::getCountLineId)
                .collect(Collectors.toSet());
        if (!inputLineIds.containsAll(fetchedLineIds)) {
            return InvConstants.ErrorMessages.LINE_DATA_INCONSISTENT;
        }

        return null; // Validation passed
    }

    /**
     * Updates the fetched lines with data from the input lines.
     * Matches lines by countLineId and calculates unitDiffQty (difference between input and snapshot quantities).
     *
     * @param fetchedLines the list of lines fetched from the database
     * @param inputLines   the list of input lines provided in the DTO
     */
    private void updateLineData(List<InvCountLineDTO> fetchedLines, List<InvCountLine> inputLines) {
        // Map input lines by countLineId
        Map<Long, InvCountLine> inputLineMap = inputLines.stream()
                .collect(Collectors.toMap(InvCountLine::getCountLineId, line -> line));

        // Update each fetched line with corresponding input data
        for (InvCountLine fetchedLine : fetchedLines) {
            InvCountLine inputLine = inputLineMap.get(fetchedLine.getCountLineId());
            if (inputLine != null) {
                // Calculate unit difference quantity (unitDiffQty)
                BigDecimal unitDiffQty = inputLine.getUnitQty().subtract(fetchedLine.getSnapshotUnitQty());

                // Update fetched line fields with input data
                fetchedLine.setUnitQty(inputLine.getUnitQty());
                fetchedLine.setUnitDiffQty(unitDiffQty);
                // Update remark if provided
                if (StringUtils.isNotBlank(inputLine.getRemark())) {
                    fetchedLine.setRemark(inputLine.getRemark());
                }
            }
        }
    }

    /**
     * Perform validation checks for inventory counting orders before submission.
     */
    private InvCountInfoDTO submitCheck(List<InvCountHeaderDTO> countHeaderDTOS) {
        // Initialize the response object to track success and error results
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // 2. Validate each fetched header
        for (InvCountHeaderDTO header : countHeaderDTOS) {
            // Validate the order and get the validation error (if any)
            String validationError = validateSubmit(header);
            if (validationError != null) {
                // If validation fails, set the error message and add to the error list
                header.setErrorMsg(validationError);
                errorList.add(header);
            } else {
                // If validation passes, add the DTO to the success list
                successList.add(header);
            }
        }
        // Populate the response DTO
        populateInvCountInfoDTO(checkResult, errorList, successList);
        // If there are validation errors, throw an exception with the details
        if (!errorList.isEmpty()) {
            throw new CommonException("Counting order submit failed: " + checkResult.getTotalErrorMsg());
        }

        return checkResult;
    }

    /**
     * Validates submit operation on an InvCountHeaderDTO.
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param headerDTO The DTO representing the header to be updated.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateSubmit(InvCountHeaderDTO headerDTO) {
        // 1. Check document status: The operation is allowed only when the status in counting, processing, rejected, withdrawn.
        Set<String> allowedStatuses = new HashSet<>(Arrays.asList(
                InvConstants.CountStatus.IN_COUNTING,
                InvConstants.CountStatus.PROCESSING,
                InvConstants.CountStatus.REJECTED,
                InvConstants.CountStatus.WITHDRAWN
        ));
        String currentStatus = headerDTO.getCountStatus();
        if (!allowedStatuses.contains(currentStatus)) {
            return String.format(InvConstants.ErrorMessages.INVALID_COUNT_STATUS, currentStatus, allowedStatuses);
        }

        // 2. Validate current user is a supervisor
        List<Long> supervisorIds = parseIds(headerDTO.getSupervisorIds()); // Parse supervisor id from the header
        Long currentUser = utils.getUserVO().getId(); // Get current user from the remote service
        if (!supervisorIds.contains(currentUser)) {
            return InvConstants.ErrorMessages.INVALID_SUPERVISOR_OPERATION;
        }

        // 3. Data integrity check for the invoice lines
        // Retrieve and set invoice count lines
        headerDTO.setCountOrderLineList(lineService.selectListByHeader(headerDTO));
        List<InvCountLineDTO> invCountLines = headerDTO.getCountOrderLineList();
        // Check if invoice lines is not empty
        if (CollUtil.isEmpty(invCountLines)) {
            return InvConstants.ErrorMessages.NO_LINES_FOR_VALIDATION;
        }
        // Invoice lines validation for unit quantity and unit difference
        for (int i = 0; i < invCountLines.size(); i++) {
            InvCountLineDTO line = invCountLines.get(i);
            // Check for null unit quantity
            // TODO: Just use line number for the index error
            if (line.getUnitQty() == null) {
                return String.format(InvConstants.ErrorMessages.EMPTY_COUNT_QUANTITY, i + 1);
            }
            // Check for unit difference and missing reason field
            BigDecimal unitDiffQty = line.getUnitDiffQty();
            boolean hasUnitDifference = unitDiffQty != null && unitDiffQty.compareTo(BigDecimal.ZERO) != 0;
            boolean isReasonMissing = StringUtils.isBlank(headerDTO.getReason());
            if (hasUnitDifference && isReasonMissing) {
                return String.format(InvConstants.ErrorMessages.MISSING_REASON_FOR_DIFFERENCE, i + 1);
            }
        }

        // All validations passed
        return null;
    }

    /**
     * Submit inventory counting orders that passed validation.
     * <p>
     * This method either starts a workflow or directly updates the order status
     * to "CONFIRMED" based on configuration.
     *
     * @param invCountHeaderDTOS List of InvCountHeaderDTOs to be submitted.
     * @return List of successfully submitted InvCountHeaderDTOs.
     */
    private List<InvCountHeaderDTO> submit(List<InvCountHeaderDTO> invCountHeaderDTOS) {
        List<InvCountHeaderDTO> resultCountHeader = invCountHeaderDTOS;

        // Get tenant ID for the current operation
        Long tenantId = utils.getUserVO().getTenantId();
        // Fetch workflow configuration to determine the submission method
        String workflowFlag = profileClient.getProfileValueByOptions(tenantId, null, null,
                InvConstants.WorkflowConfig.PROFILE_CLIENT_CONFIG);

        for (InvCountHeaderDTO header : invCountHeaderDTOS) {
            if (workflowFlag.equals("1")) {
                // If workflow is enabled, start the workflow process
                logger.info("Starting workflow.");
                workflowService.startWorkflow(tenantId, header);
            } else {
                // Otherwise, directly update the document status to "CONFIRMED"
                header.setCountStatus(InvConstants.CountStatus.CONFIRMED);
            }
        }

        if (!workflowFlag.equals("1")) {
            // Batch update the document status in the database
            List<InvCountHeader> updatedHeader = invCountHeaderRepository.batchUpdateOptional(
                    new ArrayList<>(invCountHeaderDTOS), InvCountHeader.FIELD_COUNT_STATUS);
            resultCountHeader = convertToDTOList(updatedHeader);
        }

        return resultCountHeader;
    }

    /**
     * Processes a workflow callback event for an inventory counting order.
     *
     * @param organizationId   The organization ID associated with the workflow event.
     * @param workflowEventDTO The workflow event data transfer object containing callback details.
     * @return The updated inventory count header as a DTO.
     */
    private InvCountHeaderDTO processCallback(Long organizationId, WorkflowEventDTO workflowEventDTO) {
        logger.info("Workflow callback processing started for BusinessKey: {}", workflowEventDTO.getBusinessKey());

        // Validate input parameters
        if (workflowEventDTO.getBusinessKey() == null) {
            throw new CommonException("Invalid workflow event: BusinessKey cannot be null.");
        }

        // Retrieve the matching header record from the database
        InvCountHeader existingHeader = invCountHeaderRepository.selectOne(new InvCountHeader() {{
            setCountNumber(workflowEventDTO.getBusinessKey());
            setTenantId(organizationId);
        }});

        if (existingHeader == null) {
            // Throw exception if no matching record is found
            throw new CommonException(String.format(
                    "Workflow callback failed: No matching record found for BusinessKey '%s' in OrganizationId '%d'.",
                    workflowEventDTO.getBusinessKey(), organizationId
            ));
        }

        // Update the header based on the workflow event and save changes
        InvCountHeader invCountHeader = updateHeaderWorkflow(workflowEventDTO, existingHeader);

        // Apply the header update in the database
        invCountHeaderRepository.updateByPrimaryKeySelective(invCountHeader);

        return convertToDTO(invCountHeader);
    }

    // Updates the inventory count header with details from the workflow event.
    private InvCountHeader updateHeaderWorkflow(WorkflowEventDTO workflowEventDTO, InvCountHeader header) {
        logger.info("Updating header for workflow event with BusinessKey: {}", workflowEventDTO.getBusinessKey());

        // Validate workflow event fields
        if (workflowEventDTO.getDocStatus() == null) {
            throw new CommonException("Workflow event is missing document status.");
        }

        // Update header fields with workflow event details
        header.setCountStatus(workflowEventDTO.getDocStatus());
        header.setWorkflowId(workflowEventDTO.getWorkflowId());
        // Only when status APPROVED we set the approvedTime
        if (workflowEventDTO.getDocStatus().equals(InvConstants.CountStatus.APPROVED)) {
            header.setApprovedTime(workflowEventDTO.getApprovedTime());
        }

        /// Update supervisor to the current operator when the process starts
        // TODO: Ensure this implementation is correct
        String currentUserId = utils.getUserVO().getId().toString();
        logger.info("Setting supervisor to current user: {}", currentUserId);
        header.setSupervisorIds(currentUserId);

        return header;
    }

    /**
     * Maps codes (company, department, warehouse) to their corresponding IDs for report
     * and sets them in the given DTO.
     *
     * @param invCountHeader The DTO containing the codes to map.
     */
    private void reportMapCodesToIds(InvCountHeaderDTO invCountHeader) {
        // Map company code to company ID
        String companyCode = invCountHeader.getCompanyCode();
        if (companyCode != null && !companyCode.isEmpty()) {
            Long companyId = companyService.getIdByCompanyCode(companyCode);
            invCountHeader.setCompanyId(companyId);
        }

        // Map department code to department ID
        String departmentCode = invCountHeader.getDepartmentCode();
        if (departmentCode != null && !departmentCode.isEmpty()) {
            IamDepartment department = departmentService.getDepartmentByCode(departmentCode);
            invCountHeader.setDepartmentId(department.getDepartmentId());
            invCountHeader.setDepartmentName(department.getDepartmentName());
        }

        // Map warehouse code to warehouse ID
        String warehouseCode = invCountHeader.getWarehouseCode();
        if (warehouseCode != null && !warehouseCode.isEmpty()) {
            Long warehouseId = warehouseService.getIdByWarehouseCode(warehouseCode);
            invCountHeader.setWarehouseId(warehouseId);
        }
    }

    /**
     * Populates a Counting Order Header DTO with additional fields required for reporting.
     *
     * @param header header The InvCountHeaderDTO to be populated with report-specific data.
     */
    private void populateHeaderReport(InvCountHeaderDTO header) {
        // Retrieve and associate count order lines for the given header ID
        header.setCountOrderLineList(lineService.selectListByHeader(header));

        // Parse and convert counter and supervisor IDs to UserDTO lists
        header.setCounterList(convertIdsToUserDTOs(parseIds(header.getCounterIds())));
        header.setSupervisorList(convertIdsToUserDTOs(parseIds(header.getSupervisorIds())));

        // Associate line creators with their corresponding user details
        // 1. Create a map of userId -> UserDTO for quick lookups
        Map<Long, UserDTO> userMap = header.getCounterList().stream()
                .collect(Collectors.toMap(UserDTO::getUserId, user -> user));

        // 2. Associate line creators with their corresponding user details
        for (InvCountLineDTO line : header.getCountOrderLineList()) {
            List<Long> counterIds = parseIds(line.getCounterIds());
            for (Long counterId : counterIds) {
                UserDTO user = userMap.get(counterId);
                if (user != null) {
                    line.setCounter(user);
                    break;
                }
            }
        }

        // Fetch and set the approval history for the current header
        Long tenantId = utils.getUserVO().getTenantId();
        header.setApprovalHistory(workflowService.getApproveHistory(tenantId, header));

        // Convert snapshot material IDs and batch IDs into corresponding DTO lists
        if (header.getSnapshotMaterialIds() != null && !header.getSnapshotMaterialIds().isEmpty()) {
            header.setSnapshotMaterialList(materialService.convertToMaterialDTOs(header.getSnapshotMaterialIds()));
        }
        if (header.getSnapshotBatchIds() != null && !header.getSnapshotBatchIds().isEmpty()) {
            header.setSnapshotBatchList(batchService.convertToBatchDTOs(header.getSnapshotBatchIds()));
        }

        // Set material code list as a comma-separated string
        if (header.getSnapshotMaterialList() != null) {
            String materialCodeList = header.getSnapshotMaterialList().stream()
                    .map(MaterialDTO::getMaterialCode) // Extract material code
                    .collect(Collectors.joining(",")); // Join them with a comma
            header.setMaterialCodeList(materialCodeList);
        }

        // Set batch code list as a comma-separated string
        if (header.getSnapshotBatchList() != null) {
            String batchCodeList = header.getSnapshotBatchList().stream()
                    .map(BatchDTO::getBatchCode) // Extract batch code
                    .collect(Collectors.joining(",")); // Join them with a comma
            header.setBatchCodeList(batchCodeList);
        }
    }
}

