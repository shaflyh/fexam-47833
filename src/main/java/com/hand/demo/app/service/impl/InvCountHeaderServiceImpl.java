package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hand.demo.api.dto.*;
import com.hand.demo.app.service.*;
import com.hand.demo.domain.entity.*;
import com.hand.demo.domain.repository.*;
import com.hand.demo.infra.constant.CodeRuleConst;
import com.hand.demo.infra.util.Utils;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.mybatis.domian.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final IamDepartmentService departmentService;
    private final IamDepartmentRepository departmentRepository;
    // TODO: Make sure it's okay to call the repository directly. p.s: no, it's bad practice
    private final InvMaterialRepository materialRepository;
    private final InvBatchRepository batchRepository;
    private final IamCompanyRepository companyRepository;
    private final InvWarehouseRepository warehouseRepository;
    private final InvStockRepository stockRepository;
    private final InvCountLineRepository lineRepository;
    private final CodeRuleBuilder codeRuleBuilder;
    private final Utils utils;
    private static final Logger logger = LoggerFactory.getLogger(InvCountHeaderServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    public InvCountHeaderServiceImpl(InvCountHeaderRepository invCountHeaderRepository, InvWarehouseService warehouseService,
                                     InvCountLineService lineService, InvCountExtraService extraService, IamDepartmentService departmentService,
                                     InvMaterialRepository materialRepository, InvBatchRepository batchRepository,
                                     IamCompanyRepository companyRepository, IamDepartmentRepository departmentRepository,
                                     InvWarehouseRepository warehouseRepository, InvStockRepository stockRepository,
                                     InvCountLineRepository lineRepository, CodeRuleBuilder codeRuleBuilder, Utils utils) {
        this.invCountHeaderRepository = invCountHeaderRepository;
        this.warehouseService = warehouseService;
        this.lineService = lineService;
        this.extraService = extraService;
        this.departmentService = departmentService;
        this.materialRepository = materialRepository;
        this.batchRepository = batchRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.lineRepository = lineRepository;
        this.codeRuleBuilder = codeRuleBuilder;
        this.utils = utils;
    }

    /**
     * 1. Counting order save (orderSave)
     */
    @Override
    public InvCountInfoDTO orderSave(List<InvCountHeader> invCountHeaders) {
        // 1. Counting order save verification method
        InvCountInfoDTO countInfo = manualSaveCheck(invCountHeaders);

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
        invCountHeaderDTO.setTenantAdminFlag(utils.getUserVO().getTenantAdminFlag() != null);
        // Perform pagination and sorting
        return PageHelper.doPageAndSort(pageRequest, () -> invCountHeaderRepository.selectList(invCountHeaderDTO));
    }

    /**
     * 3.b. Counting order query (detail)
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
    public InvCountInfoDTO orderExecution(List<InvCountHeader> invCountHeaders) {
        // 1. Counting order save verification method
        InvCountInfoDTO countInfo = manualSaveCheck(invCountHeaders);

        // 2. Counting order save method
        // Save and update data if all validation success
        List<InvCountHeaderDTO> saveResult = manualSave(invCountHeaders);
        // Update the header info with latest data after saving
        countInfo.setSuccessList(saveResult);
        countInfo.setTotalErrorMsg("All validation successful. Orders saved.");

        // 3. Counting order execute verification method
        InvCountInfoDTO executeInfoResult = executeCheck(invCountHeaders);

        // 4. Counting order execute method
        List<InvCountHeaderDTO> executeResult = execute(invCountHeaders);
        // Update the header info with latest data after execute
        executeInfoResult.setSuccessList(executeResult);
        countInfo.setTotalErrorMsg("All validation successful. Orders executed.");

        // 5. Counting order synchronization WMS method
        countInfo = countSyncWms(executeResult);

        // Validation error : throw exception and rollback if error list not empty
        if (!executeInfoResult.getErrorList().isEmpty()) {
            throw new CommonException("Counting order execution failed: " + executeInfoResult.getTotalErrorMsg());
        }

        return countInfo;
    }

    /**
     * 5. Submit counting results for approval (orderSubmit)
     */
    @Override
    public InvCountInfoDTO orderSubmit(List<InvCountHeader> invCountHeaders) {
        return null;
    }

    /**
     * API 6: Counting result synchronous (countResultSync)
     * Synchronizes the counting result with the database by validating the input data,
     * checking warehouse type, verifying data consistency, and updating the line data.
     *
     * @param invCountHeaderDTO the counting order header DTO containing input lines
     * @return updated InvCountHeaderDTO with the status and updated line data
     */
    @Override
    public InvCountHeaderDTO countResultSync(InvCountHeaderDTO invCountHeaderDTO) {
        // Validate all inputs
        List<InvCountLine> fetchedInvLines = new ArrayList<>();
        if (!validateInputs(invCountHeaderDTO, fetchedInvLines)) {
            // Validation failure already sets the error message and status in the DTO
            return invCountHeaderDTO;
        }

        // Update the line data
        updateLineData(fetchedInvLines, invCountHeaderDTO.getCountOrderLineList());

        // Batch update the lines in the database
        List<InvCountLine> resultLines = lineRepository.batchUpdateByPrimaryKeySelective(fetchedInvLines);

        // Step 4: Convert updated lines to DTO and update the header DTO
        invCountHeaderDTO.setCountOrderLineList(convertLinesToDTOList(resultLines));
        invCountHeaderDTO.setStatus("S"); // Set status to success

        return invCountHeaderDTO;
    }

    /**
     * 7. Counting order report dataset method (countingOrderReportDs)
     */
    @Override
    public List<InvCountHeaderDTO> countingOrderReportDs(InvCountHeaderDTO invCountHeader) {
        // Fetch the Invoice Header List
        List<InvCountHeaderDTO> invCountHeaderDTOList = invCountHeaderRepository.selectList(invCountHeader);

        // Assumption: The input is only one header id
        InvCountHeaderDTO invCountHeaderDTO = invCountHeaderDTOList.get(0);

        // Populate the header with related data from details
        populateHeaderDetails(invCountHeaderDTO);

        // Add additional data for report
        populateHeaderDetailsReport(invCountHeaderDTO);

        return invCountHeaderDTOList;
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

        // throw exception if error list not empty
        if (!errorList.isEmpty()) {
            throw new CommonException("Counting order save failed: " + checkResult.getTotalErrorMsg());
        }

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
            boolean isWmsWarehouse = warehouseService.isWmsWarehouse(headerDTO.getWarehouseId());

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


    /// //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DETAIL
    private InvCountHeaderDTO fetchHeaderById(Long countHeaderId) {
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(countHeaderId);
        // If the record is not found, throw error
        if (invCountHeader == null) {
            // TODO: Add error const
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

        // Populate snapshot materials and batches
        header.setSnapshotMaterialList(convertToMaterialDTOs(header.getSnapshotMaterialIds()));
        header.setSnapshotBatchList(convertToBatchDTOs(header.getSnapshotBatchIds()));

        // Determine if the warehouse is a WMS warehouse
        header.setIsWMSWarehouse(warehouseService.isWmsWarehouse(header.getWarehouseId()));

        // Retrieve and set invoice count lines
        header.setCountOrderLineList(lineService.selectListByHeaderId(header.getCountHeaderId()));
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

        // TODO: Check Date validation
        // Updates the countTimeStr field of a HeaderDTO object by formatting it based
        // on the countType field. If the countType is "MONTH", the time is formatted
        // as "yyyy-MM". If the countType is "YEAR", it is formatted as "yyyy".
        String limitFormat = headerDTO.getCountType().equals("MONTH") ? "yyyy-MM" : "yyyy";
        String time = headerDTO.getCountTimeStr();
        try {
            // Parse the input time string
            LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // Format the time string based on the limitFormat
            String formattedTime = dateTime.format(DateTimeFormatter.ofPattern(limitFormat));
            headerDTO.setCountTimeStr(formattedTime);
        } catch (DateTimeParseException e) {
            throw new CommonException("Invalid date format: " + time);
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

    /**
     * Executes the counting process for the provided headers.
     * <p>
     * This method performs the following:
     * 1. Updates the counting status to "INCOUNTING".
     * 2. Fetches stock data for each header to generate line data.
     * 3. Summarizes stock data based on the counting dimension and saves lines to the database.
     * 4. Updates the headers with the latest data and returns them as DTOs.
     *
     * @param invCountHeaders List of InvCountHeader entities to process.
     * @return List of InvCountHeaderDTOs containing updated headers and associated lines.
     */
    private List<InvCountHeaderDTO> execute(List<InvCountHeader> invCountHeaders) {

        // Fetch existing headers
        Map<Long, InvCountHeader> existingHeadersMap = fetchExistingHeaders(invCountHeaders);

        // Store all lines that will generate
        List<InvCountLine> totalLines = new ArrayList<>();
        for (InvCountHeader header : existingHeadersMap.values()) {
            // 1. Update the counting order status to "INCOUNTING"
            header.setCountStatus("INCOUNTING");

            // 2. Fetch valid stock data for the header
            InvCountHeaderDTO headerDTO = new InvCountHeaderDTO();
            BeanUtils.copyProperties(header, headerDTO);
            List<InvStock> invStocks = fetchValidStocks(headerDTO);

            // 3. Generate line data based on the fetched stocks
            // Summarize according to the counting dimension and save the data in the counting order line table.
            List<InvCountLine> lines = new ArrayList<>();
            int index = 1; // Natural serial number for line counting
            for (InvStock stock : invStocks) {
                // Create an invoice line from stock data
                // TODO: Make sure if this implementation is correct
                lines.add(generateInvCountLine(header, stock, index));
                index++;
            }

            totalLines.addAll(lines);

        }
        // Save all line data to database
        lineRepository.batchInsertSelective(totalLines);
        // Update the header for status change
        List<InvCountHeader> latestHeaders = invCountHeaderRepository.batchUpdateOptional(
                new ArrayList<>(existingHeadersMap.values()), InvCountHeader.FIELD_COUNT_STATUS);

        // Fetch again the header with line
        // TODO: Remove this
        List<InvCountLine> fetchedExistingLines = lineService.fetchExistingLines(totalLines);
        // Return the Header DTOs result
        List<InvCountHeaderDTO> headerDTOS = convertToDTOList(latestHeaders);
        // Attach lines to their respective headers
        for (InvCountHeaderDTO headerDTO : headerDTOS) {
            // Filter and map lines that belong to the current header
            List<InvCountLineDTO> lineDTOS = fetchedExistingLines.stream()
                    .filter(line -> line.getCountHeaderId().equals(headerDTO.getCountHeaderId()))
                    .map(this::convertLineToDTO) // Convert line entities to DTOs
                    .collect(Collectors.toList());
            // Attach lines to the header DTO
            headerDTO.setCountOrderLineList(lineDTOS);
        }

        return headerDTOS;
    }

    // Create Invoice Line from stock data
    private InvCountLine generateInvCountLine(InvCountHeader header, InvStock stock, int index) {
        InvCountLine line = new InvCountLine();
        line.setTenantId(header.getTenantId());
        line.setCountHeaderId(header.getCountHeaderId());
        line.setLineNumber(index);
        line.setWarehouseId(header.getWarehouseId());
        line.setCounterIds(header.getCounterIds());
        line.setSnapshotUnitQty(stock.getUnitQuantity());

        // If the counting dimension = SKU, summarize by material
        // TODO: Make sure if this implementation is correct (it feels wrong)
        if (header.getCountDimension().equals("SKU")) {
            // TODO: Check material code column is not exist in the table.
            line.setMaterialId(stock.getMaterialId());
            line.setUnitCode(stock.getUnitCode());
        }
        // If the counting dimension = LOT, summarize by material + batch
        if (header.getCountDimension().equals("LOT")) {
            line.setMaterialId(stock.getMaterialId());
            line.setUnitCode(stock.getUnitCode());
            line.setBatchId(stock.getBatchId());
        }
        return line;
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
    public InvCountInfoDTO countSyncWms(List<InvCountHeaderDTO> invCountHeaders) {
        // Iterate over each counting header to process synchronization
        for (InvCountHeaderDTO header : invCountHeaders) {
            Long tenantId = header.getTenantId();
            Long warehouseId = header.getWarehouseId();
            Long countHeaderId = header.getCountHeaderId();

            // Validate warehouse existence by tenantId and warehouseCode
            InvWarehouse warehouse = warehouseService.validateWarehouse(tenantId, warehouseId);

            // Fetch existing extras or initialize if none found
            List<InvCountExtra> fetchedExtras = extraService.fetchExtrasByHeaderId(countHeaderId);
            if (fetchedExtras.isEmpty()) {
                // Initialize synchronization extras
                InvCountExtra syncStatusExtra = extraService.createExtra(tenantId, countHeaderId, "wms_sync_status");
                InvCountExtra syncMsgExtra = extraService.createExtra(tenantId, countHeaderId, "wms_sync_error_message");

                // Check if the warehouse is a WMS warehouse and call the WMS interface to synchronize the counting order
                if (warehouse.getIsWmsWarehouse().equals(1)) {
                    // Set employee number from the current user for interface parameter
                    header.setEmployeeNumber(utils.getUserVO().getLoginName());

                    // Serialize header to JSON
                    String headerJson = serializeHeader(header);

                    // Call WMS API and handle response
                    Map<String, Object> responseBody = utils.callWmsApiPushCountOrder(
                            "HZERO", "FEXAM_WMS", "fexam-wms-api.thirdAddCounting", headerJson);

                    // Check returnStatus and handle logic
                    String returnStatus = (String) responseBody.get("returnStatus");
                    if ("S".equals(returnStatus)) { // Success case
                        syncStatusExtra.setProgramValue("SUCCESS");
                        syncMsgExtra.setProgramValue("");
                        // Record WMS document number
                        header.setRelatedWmsOrderCode((String) responseBody.get("code"));
                    } else { // Error case
                        syncStatusExtra.setProgramValue("ERROR");
                        syncMsgExtra.setProgramValue((String) responseBody.get("returnMsg"));
                    }
                } else {
                    // Not a WMS warehouse
                    syncStatusExtra.setProgramValue("SKIP");
                }
                // Save synchronization extras
                extraService.saveExtras(syncStatusExtra, syncMsgExtra);
            }
        }

        // Update headers (for WMS order code) and return results
        List<InvCountHeader> updatedHeaders = invCountHeaderRepository.batchUpdateOptional(
                new ArrayList<>(invCountHeaders), InvCountHeader.FIELD_RELATED_WMS_ORDER_CODE);

        InvCountInfoDTO result = new InvCountInfoDTO();
        result.setSuccessList(convertToDTOList(updatedHeaders));
        return result;
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
     * Validates the inputs for the counting result synchronization.
     *
     * @param invCountHeaderDTO the counting order header DTO containing input lines
     * @param fetchedInvLines   a list to hold fetched lines if validation passes
     * @return true if all validations pass; false otherwise, with error message set in the DTO
     */
    private boolean validateInputs(InvCountHeaderDTO invCountHeaderDTO, List<InvCountLine> fetchedInvLines) {
        // Step 1: Validate input lines
        for (InvCountLine line : invCountHeaderDTO.getCountOrderLineList()) {
            if (line.getCountLineId() == null || line.getTenantId() == null || line.getUnitQty() == null) {
                invCountHeaderDTO.setErrorMsg("The counting order line validation failed, please check the data");
                invCountHeaderDTO.setStatus("E");
                return false;
            }
        }

        // Step 2: Retrieve the counting order header from the database
        InvCountHeader invCountHeader = invCountHeaderRepository.selectByPrimary(invCountHeaderDTO.getCountHeaderId());
        if (invCountHeader == null) {
            invCountHeaderDTO.setErrorMsg("Counting order header not found");
            invCountHeaderDTO.setStatus("E");
            return false;
        }

        // Step 3: Validate warehouse type
        boolean isWmsWarehouse = warehouseService.isWmsWarehouse(invCountHeader.getWarehouseId());
        if (!isWmsWarehouse) {
            invCountHeaderDTO.setErrorMsg("The current warehouse is not a WMS warehouse, operations are not allowed");
            invCountHeaderDTO.setStatus("E");
            return false;
        }

        // Step 4: Fetch and validate existing line data
        fetchedInvLines.addAll(lineService.fetchExistingLines(
                new ArrayList<>(invCountHeaderDTO.getCountOrderLineList())
        ));
        if (fetchedInvLines.size() != invCountHeaderDTO.getCountOrderLineList().size()) {
            invCountHeaderDTO.setErrorMsg("The counting order line data is inconsistent with the INV system, please check the data");
            invCountHeaderDTO.setStatus("E");
            return false;
        }

        return true; // Validation passed
    }

    /**
     * Updates the fetched lines with input line data, including calculated fields such as unitDiffQty.
     *
     * @param fetchedInvLines the list of lines fetched from the database
     * @param inputInvLines   the list of input lines provided in the DTO
     */
    private void updateLineData(List<InvCountLine> fetchedInvLines, List<InvCountLineDTO> inputInvLines) {
        for (InvCountLine fetchedLine : fetchedInvLines) {
            for (InvCountLineDTO inputLine : inputInvLines) {
                // Match input line with the corresponding fetched line
                if (inputLine.getCountLineId().equals(fetchedLine.getCountLineId())) {
                    // Calculate unit difference (unitDiffQty)
                    BigDecimal unitQty = inputLine.getUnitQty(); // Get unit qty from input
                    BigDecimal snapshotUnitQty = fetchedLine.getSnapshotUnitQty(); // Get snapshot qty from database
                    BigDecimal unitDiffQty = unitQty.subtract(snapshotUnitQty);

                    // Update fields
                    fetchedLine.setUnitQty(unitQty);
                    fetchedLine.setSnapshotUnitQty(snapshotUnitQty);
                    fetchedLine.setUnitDiffQty(unitDiffQty);
                    // Update the remark only if it's provided in the input
                    fetchedLine.setRemark(inputLine.getRemark() != null ? inputLine.getRemark() : fetchedLine.getRemark());
                }
            }
        }
    }

    /**
     * Populates the InvCountHeaderDTO with required report fields.
     *
     * @param header the InvCountHeaderDTO to populate
     */
    private void populateHeaderDetailsReport(InvCountHeaderDTO header) {
        // Add additional field for reporting
        // Creator name and tenant
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(header.getCreatedBy());

        // Department name and warehouse code
        String departmentName = departmentService.getDepartmentName(header.getDepartmentId());
        String warehouseCode = warehouseService.getWarehouseCode(header.getWarehouseId());

        header.setDepartmentName(departmentName);
        header.setWareHouseCode(warehouseCode);
        header.setCreator(userDTO);

        // Set creator for the invoice line
        for (InvCountLineDTO line : header.getCountOrderLineList()) {
            for (UserDTO user : header.getCounterList()) {
                if (line.getCreatedBy().equals(user.getUserId())) {
                    line.setCounter(user);
                }
            }
        }
    }
}

