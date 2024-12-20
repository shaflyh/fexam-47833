package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hand.demo.api.dto.*;
import com.hand.demo.app.service.InvCountLineService;
import com.hand.demo.app.service.InvWarehouseService;
import com.hand.demo.domain.entity.*;
import com.hand.demo.domain.repository.*;
import com.hand.demo.infra.constant.CodeRuleConst;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.boot.apaas.common.userinfo.domain.UserVO;
import org.hzero.boot.apaas.common.userinfo.infra.feign.IamRemoteService;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.mybatis.domian.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountHeaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
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
    private final InvWarehouseService invWarehouseService;
    private final InvCountLineService invCountLineService;
    private final IamDepartmentRepository departmentRepository;
    private final IamRemoteService iamRemoteService;
    // TODO: Make sure it's okay to call the repository directly. p.s: no, it's bad practice
    private final InvMaterialRepository materialRepository;
    private final InvBatchRepository batchRepository;
    private final IamCompanyRepository companyRepository;
    private final InvWarehouseRepository warehouseRepository;
    private final InvStockRepository stockRepository;
    private final InvCountLineRepository lineRepository;
    private final CodeRuleBuilder codeRuleBuilder;
    private static final Logger logger = LoggerFactory.getLogger(InvCountHeaderServiceImpl.class);


    @Autowired
    public InvCountHeaderServiceImpl(InvCountHeaderRepository invCountHeaderRepository, InvWarehouseService invWarehouseService,
                                     InvCountLineService invCountLineService, IamRemoteService iamRemoteService,
                                     InvMaterialRepository materialRepository, InvBatchRepository batchRepository,
                                     IamCompanyRepository companyRepository, IamDepartmentRepository departmentRepository,
                                     InvWarehouseRepository warehouseRepository, InvStockRepository stockRepository, InvCountLineRepository lineRepository,
                                     CodeRuleBuilder codeRuleBuilder) {
        this.invCountHeaderRepository = invCountHeaderRepository;
        this.invWarehouseService = invWarehouseService;
        this.invCountLineService = invCountLineService;
        this.iamRemoteService = iamRemoteService;
        this.materialRepository = materialRepository;
        this.batchRepository = batchRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.lineRepository = lineRepository;
        this.codeRuleBuilder = codeRuleBuilder;
    }

    /**
     * 1. Counting order save (orderSave)
     */
    @Override
    public InvCountInfoDTO orderSave(List<InvCountHeader> invCountHeaders) {
        // 1. Counting order save verification method
        InvCountInfoDTO countInfo = manualSaveCheck(invCountHeaders);
        if (!countInfo.getErrorList().isEmpty()) {
            // return if there is any error
            return countInfo;
        }

        // 2. Counting order save method
        // Save and update data if all validation success
        List<InvCountHeaderDTO> saveResult = manualSave(invCountHeaders);
        // Update the header info with data after saving
        countInfo.setSuccessList(saveResult);
        countInfo.setTotalErrorMsg("All validation successful. Orders saved.");

        // return latest data
        return countInfo;
    }

    /**
     * 2. Counting order remove (orderRemove)
     */
    @Override
    public InvCountInfoDTO orderRemove(List<InvCountHeader> invCountHeaders) {
        InvCountInfoDTO checkResult = manualRemoveCheck(invCountHeaders);
        // Check if there are errors
        if (checkResult.getErrorList().isEmpty()) {
            // Delete Invoice Headers if all validation succeeds
            manualRemove(invCountHeaders);
            checkResult.setTotalErrorMsg("All validation successful. Orders deleted.");
        }
        return checkResult;
    }

    /**
     * 3.a. Counting order query (list)
     */
    @Override
    public Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeader invCountHeader) {
        // TODO: Add request DTO only for list query
        // Convert to DTO
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        BeanUtils.copyProperties(invCountHeader, invCountHeaderDTO);
        // Check if the user admin or not
        invCountHeaderDTO.setTenantAdminFlag(getUserVO().getTenantAdminFlag() != null);
        // Perform pagination and sorting
        return PageHelper.doPageAndSort(pageRequest, () -> invCountHeaderRepository.selectList(invCountHeaderDTO));
    }

    /**
     * 3.b. Counting order query (detail)
     */
    @Override
    public InvCountHeaderDTO selectDetail(Long countHeaderId) {
        // Fetch the Invoice Header
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(countHeaderId);
        // If the record is not found, throw error
        if (invCountHeader == null) {
            // TODO: Add error const
            throw new CommonException("Invoice with id " + countHeaderId + " not found");
        }

        // Convert to DTO
        InvCountHeaderDTO invCountHeaderDTO = new InvCountHeaderDTO();
        BeanUtils.copyProperties(invCountHeader, invCountHeaderDTO);

        // Populate the header with related data
        populateHeaderDetails(invCountHeaderDTO);

        return invCountHeaderDTO;
    }

    /**
     * 4. Counting order execution (orderExecution)
     */
    // TODO: Test order execute
    @Override
    public InvCountInfoDTO orderExecution(List<InvCountHeader> invCountHeaders) {
        // 1. Counting order save verification method
        InvCountInfoDTO countInfo = manualSaveCheck(invCountHeaders);
        if (!countInfo.getErrorList().isEmpty()) {
            // return if there is any error
            return countInfo;
        }

        // 2. Counting order save method
        // Save and update data if all validation success
        List<InvCountHeaderDTO> saveResult = manualSave(invCountHeaders);
        // Update the header info with latest data after saving
        countInfo.setSuccessList(saveResult);
        countInfo.setTotalErrorMsg("All validation successful. Orders saved.");

        // 3. Counting order execute verification method
        InvCountInfoDTO executeInfoResult = executeCheck(invCountHeaders);
        if (!executeInfoResult.getErrorList().isEmpty()) {
            return executeInfoResult;
        }

        // 4. Counting order execute method
        List<InvCountHeaderDTO> executeResult = execute(invCountHeaders);
        // Update the header info with latest data after execute
        executeInfoResult.setSuccessList(executeResult);

        return executeInfoResult;
    }

    /**
     * 5. Submit counting results for approval (orderSubmit)
     */
    @Override
    public InvCountInfoDTO orderSubmit(List<InvCountHeader> invCountHeaders) {
        return null;
    }

    /**
     * 6. Counting result synchronous (countResultSync)
     */
    @Override
    public InvCountHeaderDTO countResultSync(InvCountHeader invCountHeader) {
        return null;
    }

    /**
     * 7. Counting order report dataset method (countingOrderReportDs)
     */
    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeader invCountHeader) {
        return Collections.emptyList();
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     */

    /**
     * Performs the manual save check for the provided headers.
     *
     * @param invCountHeaders List of InvCountHeader entities to check.
     * @return InvCountInfoDTO containing validation results.
     */
    private InvCountInfoDTO manualSaveCheck(List<InvCountHeader> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Separate headers into inserts and updates
        List<InvCountHeader> insertList = filterHeaders(invCountHeaders, true);
        List<InvCountHeader> updateList = filterHeaders(invCountHeaders, false);

        // Process all insert operations (skip the validation, directly put into the success list)
        successList.addAll(convertToDTOList(insertList));

        // Process the validation of all update invoice if there are any
        if (!updateList.isEmpty()) {
            // Fetch existing headers from the repository and map them by their ID for quick access
            Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeaders(updateList);

            // Iterate over each header that needs to be validated
            for (InvCountHeader header : updateList) {
                InvCountHeaderDTO headerDTO = new InvCountHeaderDTO();
                BeanUtils.copyProperties(header, headerDTO); // Copy properties from the entity to the DTO
                Long headerId = headerDTO.getCountHeaderId();

                // Retrieve the corresponding existing header using the header ID
                InvCountHeader existingHeader = existingHeadersMap.get(headerId);
                if (existingHeader == null) {
                    // If the existing header is not found, add an error message
                    headerDTO.setErrorMsg("Existing header not found for ID: " + headerId);
                    errorList.add(headerDTO);
                    continue; // Skip to the next header
                }

                // Validate the update operation against the existing header
                String validationError = validateSave(headerDTO, existingHeader);
                if (validationError != null) {
                    // If validation fails, set the error message and add to the error list
                    headerDTO.setErrorMsg(validationError);
                    errorList.add(headerDTO);
                } else {
                    // If validation passes, add the DTO to the success list
                    successList.add(headerDTO);
                }
            }
        }

        // Populate the response DTO
        populateInvCountInfoDTO(checkResult, errorList, successList);

        return checkResult;
    }

    private InvCountInfoDTO manualRemoveCheck(List<InvCountHeader> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Fetch existing headers
        Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeaders(invCountHeaders);

        for (InvCountHeader header : existingHeadersMap.values()) {
            InvCountHeaderDTO headerDTO = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, headerDTO); // Copy properties from the entity to the DTO

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
    private List<InvCountHeaderDTO> manualSave(List<InvCountHeader> invCountHeaders) {
        List<InvCountHeader> insertList = filterHeaders(invCountHeaders, true);
        List<InvCountHeader> updateList = filterHeaders(invCountHeaders, false);

        // Generate count numbers for new headers with Code Rule Builder
        for (InvCountHeader countHeader : insertList) {
            Map<String, String> codeBuilderMap = new HashMap<>();
            codeBuilderMap.put("customSegment", countHeader.getTenantId().toString() + "-");
            String invCountNumber = codeRuleBuilder.generateCode(CodeRuleConst.INV_COUNT_NUMBER, codeBuilderMap);
            countHeader.setCountNumber(invCountNumber);
        }
        logger.info("Save and update InvCountHeaders");
        invCountHeaderRepository.batchInsertSelective(insertList);
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);

        // Fetch again the data and return the value
        Map<Long, InvCountHeader> latestHeadersMap = fetchExistingHeaders(invCountHeaders);
        List<InvCountHeader> latestHeaders = invCountHeaderRepository.selectByIds(
                latestHeadersMap.keySet().stream().map(String::valueOf).collect(Collectors.joining(",")));
        return convertToDTOList(latestHeaders);
    }

    /**
     * Performs the actual remove operation after validation.
     *
     * @param invCountHeaders List of InvCountHeader entities to remove.
     */
    private void manualRemove(List<InvCountHeader> invCountHeaders) {
        invCountHeaderRepository.batchDeleteByPrimaryKey(invCountHeaders);
    }

    /**
     * Filters headers based on whether they are new (insert) or existing (update).
     *
     * @param headers  The list of headers to filter.
     * @param isInsert If true, filters for inserts; else for updates.
     * @return Filtered list of InvCountHeaders.
     */
    private List<InvCountHeader> filterHeaders(List<InvCountHeader> headers, boolean isInsert) {
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
     * @param headers List of InvCountHeader entities to convert.
     * @return List of InvCountHeaderDTOs.
     */
    private List<InvCountHeaderDTO> convertToDTOList(List<InvCountHeader> headers) {
        return headers.stream().map(header -> {
            InvCountHeaderDTO dto = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, dto); // Copy properties from entity to DTO
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Fetches existing InvCountHeaders from the repository based on the headers to be updated
     * and maps them by their CountHeaderId for efficient lookup.
     *
     * @param updateList List of InvCountHeader entities that need to be updated.
     * @return Map of CountHeaderId to InvCountHeader.
     */
    private Map<Long, InvCountHeader> fetchExistingHeaders(List<InvCountHeader> updateList) {
        // TODO: Check if id may not exist.
        // Extract the set of IDs from the update list
        Set<Long> headerIds = updateList.stream().map(InvCountHeader::getCountHeaderId).collect(Collectors.toSet());

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
     * Validates save or update operation on an InvCountHeaderDTO against the existing InvCountHeader entity.
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param headerDTO      The DTO representing the header to be updated.
     * @param existingHeader The existing header entity from the database.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateSave(InvCountHeaderDTO headerDTO, InvCountHeader existingHeader) {
        // a. Ensure the count status is not being changed
        if (!headerDTO.getCountStatus().equals(existingHeader.getCountStatus())) {
            return "Invoice count status cannot be updated";
        }

        // b. Check if the current status allows modification
        Set<String> allowedStatuses = new HashSet<>(Arrays.asList("DRAFT", "INCOUNTING", "REJECTED", "WITHDRAWN"));
        String currentStatus = existingHeader.getCountStatus();
        if (!allowedStatuses.contains(currentStatus)) {
            return "Only 'Draft', 'In Counting', 'Rejected', and 'Withdrawn' statuses can be modified";
        }

        // c. Validate the current user based on the document's status and user roles
        Long currentOperator = DetailsHelper.getUserDetails().getUserId(); // Get the ID of the current user
        Long creatorId = existingHeader.getCreatedBy(); // Get the ID of the user who created the document

        // c.1. If the status is 'DRAFT', only the creator can modify the document
        if ("DRAFT".equals(currentStatus) && !creatorId.equals(currentOperator)) {
            return "Document in draft status can only be modified by the document creator.";
        }

        // c.2. If the status is one of 'INCOUNTING', 'REJECTED', or 'WITHDRAWN', perform additional validations
        Set<String> statusWithAdditionalValidation =
                new HashSet<>(Arrays.asList("INCOUNTING", "REJECTED", "WITHDRAWN"));
        if (statusWithAdditionalValidation.contains(currentStatus)) {
            // Parse supervisor and counter IDs from the existing header
            List<Long> supervisorIds = parseIds(existingHeader.getSupervisorIds());
            List<Long> counterIds = parseIds(existingHeader.getCounterIds());

            // Check if the warehouse is a WMS warehouse
            boolean isWmsWarehouse = invWarehouseService.isWmsWarehouse(headerDTO.getWarehouseId());

            // If it's a WMS warehouse, only supervisors are allowed to operate
            if (isWmsWarehouse && !supervisorIds.contains(currentOperator)) {
                return "The current warehouse is a WMS warehouse, and only the supervisor is allowed to operate";
            }

            // c.3. Check if the current user is either a counter, supervisor, or the document creator
            boolean isCounter = counterIds.contains(currentOperator);
            boolean isSupervisor = supervisorIds.contains(currentOperator);
            boolean isCreator = creatorId.equals(currentOperator);

            if (!isCounter && !isSupervisor && !isCreator) {
                return "Only the document creator, counter, or supervisor can modify the document for the status of in counting, rejected, or withdrawn.";
            }
        }
        // All validations passed; no error
        return null;
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
        if (!headerDTO.getCountStatus().equals("DRAFT")) {
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
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList(); // Return an empty list if there's nothing to parse
        }

        // Split the string by commas, trim whitespace, parse each segment to Long, and collect into a list
        List<Long> idList = new ArrayList<>();
        for (String idStr : ids.split(",")) {
            String trimmed = idStr.trim();
            if (!trimmed.isEmpty()) { // Ensure the segment is not empty after trimming
                try {
                    Long id = Long.parseLong(trimmed); // Parse the string to a Long
                    idList.add(id); // Add the parsed ID to the list
                } catch (NumberFormatException e) {
                    // Log or handle the parsing error as needed
                    // For now, we'll skip invalid ID segments
                }
            }
        }
        return idList; // Return the list of parsed IDs
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DETAIL

    /**
     * Populates the InvCountHeaderDTO with related user, material, batch, warehouse, and line details.
     *
     * @param header the InvCountHeaderDTO to populate
     */
    private void populateHeaderDetails(InvCountHeaderDTO header) {
        // Parse and convert counter and supervisor IDs to UserDTO lists
        header.setCounterList(convertIdsToUserDTOs(parseIds(header.getCounterIds())));
        header.setSupervisorList(convertIdsToUserDTOs(parseIds(header.getSupervisorIds())));

        // Populate snapshot materials and batches
        header.setSnapshotMaterialList(convertToMaterialDTOs(header.getSnapshotMaterialIds()));
        header.setSnapshotBatchList(convertToBatchDTOs(header.getSnapshotBatchIds()));

        // Determine if the warehouse is a WMS warehouse
        header.setIsWMSwarehouse(invWarehouseService.isWmsWarehouse(header.getWarehouseId()));

        // Retrieve and set invoice count lines
        header.setInvCountLineDTOList(invCountLineService.selectListByHeaderId(header.getCountHeaderId()));
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

    /**
     * Converts a comma-separated string of material IDs to a list of MaterialDTOs.
     *
     * @param materialIds the comma-separated material IDs
     * @return a list of MaterialDTOs
     */
    private List<MaterialDTO> convertToMaterialDTOs(String materialIds) {
        List<InvMaterial> materials = materialRepository.selectByIds(materialIds);
        return materials.stream().map(this::mapToMaterialDTO).collect(Collectors.toList());
    }

    /**
     * Converts a comma-separated string of batch IDs to a list of BatchDTOs.
     *
     * @param batchIds the comma-separated batch IDs
     * @return a list of BatchDTOs
     */
    private List<BatchDTO> convertToBatchDTOs(String batchIds) {
        List<InvBatch> batches = batchRepository.selectByIds(batchIds);
        return batches.stream().map(this::mapToBatchDTO).collect(Collectors.toList());
    }

    /**
     * Maps an InvMaterial entity to a MaterialDTO.
     *
     * @param material the InvMaterial entity
     * @return the corresponding MaterialDTO
     */
    private MaterialDTO mapToMaterialDTO(InvMaterial material) {
        MaterialDTO dto = new MaterialDTO();
        dto.setMaterialId(material.getMaterialId());
        dto.setMaterialCode(material.getMaterialCode());
        return dto;
    }

    /**
     * Maps an InvBatch entity to a BatchDTO.
     *
     * @param batch the InvBatch entity
     * @return the corresponding BatchDTO
     */
    private BatchDTO mapToBatchDTO(InvBatch batch) {
        BatchDTO dto = new BatchDTO();
        dto.setBatchId(batch.getBatchId());
        dto.setBatchCode(batch.getBatchCode());
        return dto;
    }

    ///  //////////////////////////////////////////////////////////////////////////////////////////////////////
    /// User VO for data permission rule

    private UserVO getUserVO() {
        ResponseEntity<String> stringResponse = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();
        // Fix object mapper error
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Set a custom date format that matches the API response
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        UserVO userVO;
        try {
            logger.info(stringResponse.getBody());
            userVO = objectMapper.readValue(stringResponse.getBody(), UserVO.class);
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to parse response body to UserVO", e);
        } catch (Exception e) {
            throw new CommonException("Unexpected error occurred", e);
        }
        return userVO;
    }

    private InvCountInfoDTO executeCheck(List<InvCountHeader> invCountHeaders) {
        // Initialize the response DTO
        InvCountInfoDTO checkResult = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Requery the database based on the input document ID
        Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeaders(invCountHeaders);

        // Iterate over each header that needs to be validated
        for (InvCountHeader header : existingHeadersMap.values()) {
            InvCountHeaderDTO headerDTO = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, headerDTO); // Copy properties from the entity to the DTO

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
        if (!"DRAFT".equals(headerDTO.getCountStatus())) {
            return " Only draft status can execute";
        }

        // b. current login user validation: Only the document creator can execute
        if (!headerDTO.getCreatedBy().equals(DetailsHelper.getUserDetails().getUserId())) {
            return " Only the document creator can execute";
        }

        // c. value set validation
        // TODO: Ask mr zeki if we need to validate the value set again.

        // d. company, department, warehouse validation
        if (companyRepository.selectByPrimary(headerDTO.getCompanyId()) == null) {
            return "Company does not exist";
        }
        if (departmentRepository.selectByPrimary(headerDTO.getDepartmentId()) == null) {
            return "Department does not exist";
        }
        if (warehouseRepository.selectByPrimary(headerDTO.getWarehouseId()) == null) {
            return "Warehouse does not exist";
        }

        // e. on hand quantity validation
        List<InvStock> invStocks = fetchValidStocks(headerDTO);
        if (invStocks == null || invStocks.isEmpty()) {
            return "Unable to query on hand quantity data.";
        }

        // All validations passed; no error
        return null;
    }

    private List<InvStock> fetchValidStocks(InvCountHeaderDTO headerDTO) {
        // on hand quantity validation is not 0 according to the tenantId + companyId + departmentId + warehouseId + snapshotMaterialIds + snapshotBatchIds
        Condition condition = new Condition(InvStock.class); // Create a condition for the InvStock entity

        // Add fixed conditions
        condition.createCriteria()
                .andEqualTo("tenantId", headerDTO.getTenantId())
                .andEqualTo("companyId", headerDTO.getCompanyId())
                .andEqualTo("departmentId", headerDTO.getDepartmentId())
                .andEqualTo("warehouseId", headerDTO.getWarehouseId());

        // Add dynamic list-based conditions for snapshotMaterialIds and snapshotBatchIds
        if (headerDTO.getSnapshotMaterialIds() != null && !headerDTO.getSnapshotMaterialIds().isEmpty()) {
            List<String> materialIds = Arrays.asList(headerDTO.getSnapshotMaterialIds().split(","));
            condition.and().andIn("materialId", materialIds);
        }

        if (headerDTO.getSnapshotBatchIds() != null && !headerDTO.getSnapshotBatchIds().isEmpty()) {
            List<String> batchIds = Arrays.asList(headerDTO.getSnapshotBatchIds().split(","));
            condition.and().andIn("batchId", batchIds);
        }

        // Query using the stock repository
        return stockRepository.selectByCondition(condition);
    }

    private List<InvCountHeaderDTO> execute(List<InvCountHeader> invCountHeaders) {
        List<InvCountHeaderDTO> headerResults = new ArrayList<>();

        // Fetch existing headers from the repository and map them by their ID for quick access
        Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeaders(invCountHeaders);

        int index = 1; // Natural serial number for line counting
        List<InvCountLine> totalLines = new ArrayList<>();
        for (InvCountHeader header : existingHeadersMap.values()) {
            // 1. Update the counting order status to "In counting"
            header.setCountStatus("INCOUNTING");

            // 2. Get stock data to generate row data
            InvCountHeaderDTO headerDTO = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, headerDTO);
            List<InvStock> invStocks = fetchValidStocks(headerDTO);

            // 3. Summarize according to the counting dimension and save the data in the counting order line table.
            // Initialize line list
            List<InvCountLine> lines = new ArrayList<>();
            for (InvStock stock : invStocks) {
                // Create Invoice Line from stock data
                // TODO: Make sure if this implementation is correct
                lines.add(generateInvCountLine(header, stock, index));
                index++;
            }

            totalLines.addAll(lines);

            // TODO: Date validation
            // String limitFormat = headerDTO.getCountType().equals("MONTH") ? "yyyy-MM" : "yyyy";
            // String time = headerDTO.getCountTimeStr();
            // headerDTO.setCountTimeStr();

            // TODO: change the input to just line entity to reduce boiler plate ing converting
            List<InvCountLineDTO> invCountLineDTOS = new ArrayList<>();
            for (InvCountLine invCountLine : lines) {
                InvCountLineDTO invCountLineDTO = new InvCountLineDTO();
                BeanUtils.copyProperties(invCountLine, invCountLineDTO);
                invCountLineDTOS.add(invCountLineDTO);
            }
            headerDTO.setInvCountLineDTOList(invCountLineDTOS);
            headerResults.add(headerDTO);
        }
        // Save all line data to database and update the header
        lineRepository.batchInsertSelective(totalLines);
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(new ArrayList<>(existingHeadersMap.values()));

        return headerResults;
    }

    // Create Invoice Line from stock data
    private InvCountLine generateInvCountLine(InvCountHeader header, InvStock stock, int index) {
        InvCountLine line = new InvCountLine();
        line.setTenantId(header.getTenantId());
        line.setCountHeaderId(header.getCountHeaderId());
        line.setLineNumber(index);
        line.setWarehouseId(header.getWarehouseId());
        line.setCounterIds(header.getCounterIds());

        // If the counting dimension = SKU, summarize by material;
        // TODO: Make sure if this implementation is correct (it feels wrong)
        if (header.getCountDimension().equals("SKU")) {
            // TODO: Check material code column is not exist in the table.
            line.setMaterialId(stock.getMaterialId());
            line.setUnitCode(stock.getUnitCode());
        }
        // If the counting dimension = LOT, summarize by material + batch;
        if (header.getCountDimension().equals("LOT")) {
            line.setMaterialId(stock.getMaterialId());
            line.setUnitCode(stock.getUnitCode());
            line.setBatchId(stock.getBatchId());
        }
        return line;
    }


    private void countingOrderExecute(List<InvCountHeader> invCountHeaders) {
    }

    private void countingOrderSyncWMS(List<InvCountHeader> invCountHeaders) {
    }


    //    @Override
    //    public void saveData(List<InvCountHeader> invCountHeaders) {
    //        List<InvCountHeader> insertList =
    //                invCountHeaders.stream().filter(line -> line.getCountHeaderId() == null).collect(Collectors.toList());
    //        List<InvCountHeader> updateList =
    //                invCountHeaders.stream().filter(line -> line.getCountHeaderId() != null).collect(Collectors.toList());
    //        invCountHeaderRepository.batchInsertSelective(insertList);
    //        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);
    //    }


    //    private InvCountInfoDTO saveCheck(List<InvCountHeaderDTO> invCountHeaderDTOS) {
    //        // Initialize the response DTO to store success and error lists
    //        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
    //        // Use a Set to collect invalid invoices (prevents duplicates automatically)
    //        Set<InvCountHeaderDTO> errorSet = new HashSet<>();
    //        // List to hold successful invoices (to be derived later)
    //        List<InvCountHeaderDTO> successList;
    //        List<String> errorMessages = new ArrayList<>();
    //
    //        // Fetch valid statuses for comparison
    //        List<LovValueDTO> invCountStatusList = lovAdapter.queryLovValue(LovConst.INV_COUNT_STATUS, 0L);
    //        Set<String> validStatuses = invCountStatusList.stream().map(LovValueDTO::getValue).collect(Collectors.toSet());
    //
    //        // Iterate through each invoice to validate fields
    //        for (InvCountHeaderDTO invoice : invCountHeaderDTOS) {
    //            // Check if any mandatory field is null or empty
    //            boolean hasInvalidField = Stream.of(invoice.getTenantId(), invoice.getCountStatus(), invoice.getCompanyId(),
    //                    invoice.getWarehouseId(), invoice.getCounterIds(), invoice.getSupervisorIds()).anyMatch(
    //                    field -> field == null || (field instanceof String && ((String) field).trim().isEmpty()));
    //
    //            // If a field is invalid, add the invoice to the error set and add to the error messages
    //            if (hasInvalidField) {
    //                errorSet.add(invoice);
    //                errorMessages.add(ErrorCodeConst.INPUT_BLANK);
    //                continue; // Skip further validation for this invoice
    //            }
    //            // Check if the count status is invalid
    //            if (!validStatuses.contains(invoice.getCountStatus())) {
    //                errorSet.add(invoice);
    //                errorMessages.add(ErrorCodeConst.INPUT_BLANK);
    //            }
    //        }
    //        // Convert the error set to a list for DTO
    //        List<InvCountHeaderDTO> errorList = new ArrayList<>(errorSet);
    //        // Calculate successList by filtering out invoices present in the error set
    //        successList = invCountHeaderDTOS.stream().filter(invoice -> !errorSet.contains(invoice)) // Exclude errors
    //                                        .collect(Collectors.toList());
    //        // Populate the response DTO with error and success lists
    //        invCountInfoDTO.setErrorList(errorList);   // Set list of invalid invoices
    //        invCountInfoDTO.setSuccessList(successList); // Set list of valid invoices
    //        invCountInfoDTO.setTotalErrorMsg(String.join(", ", errorMessages));
    //
    //        return invCountInfoDTO;
    //    }
}

