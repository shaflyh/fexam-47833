package com.hand.demo.app.service.impl;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import com.hand.demo.api.dto.UserDTO;
import com.hand.demo.app.service.InvWarehouseService;
import com.hand.demo.infra.constant.CodeRuleConst;
import com.hand.demo.infra.mapper.InvCountHeaderMapper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.boot.platform.lov.adapter.LovAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.hand.demo.app.service.InvCountHeaderService;
import org.springframework.stereotype.Service;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;

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
    private final InvCountHeaderMapper invCountHeaderMapper;
    private final InvWarehouseService invWarehouseService;
    private final CodeRuleBuilder codeRuleBuilder;

    private static final Logger logger = LoggerFactory.getLogger(InvCountHeaderServiceImpl.class);

    @Autowired
    public InvCountHeaderServiceImpl(InvCountHeaderRepository invCountHeaderRepository,
                                     InvCountHeaderMapper invCountHeaderMapper, InvWarehouseService invWarehouseService,
                                     CodeRuleBuilder codeRuleBuilder) {
        this.invCountHeaderRepository = invCountHeaderRepository;
        this.invCountHeaderMapper = invCountHeaderMapper;
        this.invWarehouseService = invWarehouseService;
        this.codeRuleBuilder = codeRuleBuilder;
    }

    /*
     * Select all Invoice Count Header with countStatusMeaning
     */
    @Override
    public Page<InvCountHeaderDTO> selectList(PageRequest pageRequest, InvCountHeader invCountHeader) {
        return PageHelper.doPageAndSort(pageRequest, () -> invCountHeaderRepository.selectList(invCountHeader));
    }

    /*
     * Select Invoice Count Header detail with countStatusMeaning
     */
    @Override
    public InvCountHeaderDTO selectDetail(Long countHeaderId) {
        // Create a new InvCountHeader object with the given countHeaderId
        InvCountHeader countHeader = new InvCountHeader();
        countHeader.setCountHeaderId(countHeaderId);

        // Retrieve the list of InvCountHeaderDTO based on countHeader
        List<InvCountHeaderDTO> invCountHeaderDTOS = invCountHeaderMapper.selectList(countHeader);

        // If no records are found, return null early
        if (invCountHeaderDTOS.isEmpty()) {
            // Optionally, log a warning here
            return null;
        }
        // Get the first InvCountHeaderDTO from the list
        InvCountHeaderDTO invCountHeader = invCountHeaderDTOS.get(0);

        // Parse counter and supervisor IDs from the DTO
        List<Long> counterIds = parseIds(invCountHeader.getCounterIds());
        List<Long> supervisorIds = parseIds(invCountHeader.getSupervisorIds());

        // Convert counter and supervisor IDs to UserDTO lists
        List<UserDTO> counterList = convertIdsToUserDTOs(counterIds);
        List<UserDTO> supervisorList = convertIdsToUserDTOs(supervisorIds);

        // Set the lists of UserDTOs to the InvCountHeaderDTO
        invCountHeader.setCounterList(counterList);
        invCountHeader.setSupervisorList(supervisorList);

        // Return the populated InvCountHeaderDTO
        return invCountHeader;
    }

    @Override
    public void saveData(List<InvCountHeader> invCountHeaders) {
        List<InvCountHeader> insertList =
                invCountHeaders.stream().filter(line -> line.getCountHeaderId() == null).collect(Collectors.toList());
        List<InvCountHeader> updateList =
                invCountHeaders.stream().filter(line -> line.getCountHeaderId() != null).collect(Collectors.toList());
        invCountHeaderRepository.batchInsertSelective(insertList);
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    @Override
    public InvCountInfoDTO orderSave(List<InvCountHeader> invCountHeaders) {
        InvCountInfoDTO manualSaveCheckResult = manualSaveCheck(invCountHeaders);
        if (manualSaveCheckResult.getErrorList().isEmpty()) {
            manualSave(invCountHeaders);
            manualSaveCheckResult.setTotalErrorMsg("All validation successful. Order saved.");
        }
        return manualSaveCheckResult;
    }

    private InvCountInfoDTO manualSaveCheck(List<InvCountHeader> invCountHeaders) {
        // Initialize the response DTO to store lists of successful and erroneous headers
        InvCountInfoDTO invCountInfoDTO = new InvCountInfoDTO();
        List<InvCountHeaderDTO> errorList = new ArrayList<>();
        List<InvCountHeaderDTO> successList = new ArrayList<>();

        // Separate the headers into inserts (new entries) and updates (existing entries)
        List<InvCountHeader> insertList = invCountHeaders.stream().filter(header -> header.getCountHeaderId() ==
                                                                                    null) // Headers without an ID are inserts
                                                         .collect(Collectors.toList());

        List<InvCountHeader> updateList = invCountHeaders.stream().filter(header -> header.getCountHeaderId() !=
                                                                                    null) // Headers with an ID are updates
                                                         .collect(Collectors.toList());

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
                String validationError = validateUpdate(headerDTO, existingHeader);
                logger.info("testsjfaskdjflsdjf");
                logger.info(validationError);
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

        // Populate the response DTO with the lists of errors and successes
        invCountInfoDTO.setErrorList(errorList); // Set the list of headers with errors
        invCountInfoDTO.setSuccessList(successList); // Set the list of successfully processed headers

        // Combine all error messages into a single string for easier reference
        String totalErrorMsg = errorList.stream().map(InvCountHeaderDTO::getErrorMsg)
                                        .filter(Objects::nonNull) // Ensure no null messages are included
                                        .collect(Collectors.joining(", ")); // Join them with a comma separator

        invCountInfoDTO.setTotalErrorMsg(totalErrorMsg); // Set the combined error messages

        return invCountInfoDTO;
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
     * Validates an update operation on an InvCountHeaderDTO against the existing InvCountHeader entity.
     * Returns an error message if any validation fails, otherwise returns null.
     *
     * @param headerDTO      The DTO representing the header to be updated.
     * @param existingHeader The existing header entity from the database.
     * @return Error message if validation fails; otherwise, null.
     */
    private String validateUpdate(InvCountHeaderDTO headerDTO, InvCountHeader existingHeader) {
        // a. Ensure the count status is not being changed
        if (!headerDTO.getCountStatus().equals(existingHeader.getCountStatus())) {
            return "Invoice count status cannot be updated";
        }

        // b. Check if the current status allows modification
        Set<String> allowedStatuses = new HashSet<>(Arrays.asList("DRAFT", "INCOUNTING", "REJECTED", "WITHDRAWN"));
        String currentStatus = existingHeader.getCountStatus();
        logger.info("currentStatus: " + currentStatus);
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

    /**
     * Converts a list of user IDs to a list of {@link UserDTO} objects.
     *
     * <p>This utility method transforms each ID in the provided list into a {@link UserDTO}
     * with the corresponding {@code userId} set.
     *
     * @param ids A list of user IDs to convert.
     * @return A list of {@link UserDTO} objects corresponding to the provided IDs.
     * Returns an empty list if the input is {@code null} or empty.
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

    private void manualSave(List<InvCountHeader> invCountHeaders) {
        List<InvCountHeader> insertList =
                invCountHeaders.stream().filter(line -> line.getCountHeaderId() == null).collect(Collectors.toList());
        List<InvCountHeader> updateList =
                invCountHeaders.stream().filter(line -> line.getCountHeaderId() != null).collect(Collectors.toList());

        for (InvCountHeader countHeader : insertList) {
            // Invoice Count Header number generation with Code Rule Builder
            Map<String, String> codeBuilderMap = new HashMap<>();
            codeBuilderMap.put("customSegment", countHeader.getTenantId().toString() + "-");
            String invCountNumber = codeRuleBuilder.generateCode(CodeRuleConst.INV_COUNT_NUMBER, codeBuilderMap);
            countHeader.setCountNumber(invCountNumber);
        }
        logger.info("Inserting and updating InvCountHeaders...");
        invCountHeaderRepository.batchInsertSelective(insertList);
        invCountHeaderRepository.batchUpdateByPrimaryKeySelective(updateList);
    }

    private void countingOrderExecuteVerification(List<InvCountHeader> invCountHeaders) {
    }

    private void countingOrderExecute(List<InvCountHeader> invCountHeaders) {
    }

    private void countingOrderSyncWMS(List<InvCountHeader> invCountHeaders) {
    }

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
    //                // TODO: Update the error code
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

