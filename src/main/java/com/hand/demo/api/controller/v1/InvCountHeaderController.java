package com.hand.demo.api.controller.v1;

import com.hand.demo.api.dto.InvCountHeaderDTO;
import com.hand.demo.api.dto.InvCountInfoDTO;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.base.BaseController;
import org.hzero.core.cache.ProcessCacheValue;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hand.demo.app.service.InvCountHeaderService;
import com.hand.demo.domain.entity.InvCountHeader;
import com.hand.demo.domain.repository.InvCountHeaderRepository;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * (InvCountHeader)表控制层
 *
 * @author Shafly - 47833
 * @since 2024-12-17 10:42:53
 */

@RestController("invCountHeaderController.v1")
@RequestMapping("/v1/{organizationId}/inv-count-headers")
public class InvCountHeaderController extends BaseController {

    private final InvCountHeaderRepository invCountHeaderRepository;
    private final InvCountHeaderService invCountHeaderService;

    @Autowired
    public InvCountHeaderController(InvCountHeaderRepository invCountHeaderRepository,
                                    InvCountHeaderService invCountHeaderService) {
        this.invCountHeaderRepository = invCountHeaderRepository;
        this.invCountHeaderService = invCountHeaderService;
    }

    @ApiOperation(value = "List")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @ProcessCacheValue
    @GetMapping
    public ResponseEntity<Page<InvCountHeaderDTO>> list(InvCountHeader invCountHeader,
                                                        @PathVariable Long organizationId, @ApiIgnore
                                                        @SortDefault(value = InvCountHeader.FIELD_CREATION_DATE,
                                                                direction = Sort.Direction.DESC)
                                                        PageRequest pageRequest) {
        Page<InvCountHeaderDTO> list = invCountHeaderService.selectList(pageRequest, invCountHeader);
        return Results.success(list);
    }

    @ApiOperation(value = "Details")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @ProcessLovValue(targetField = BaseConstants.FIELD_BODY)
    @GetMapping("/{countHeaderId}/detail")
    public ResponseEntity<InvCountHeaderDTO> detail(@PathVariable Long organizationId,
                                                    @PathVariable Long countHeaderId) {
        InvCountHeaderDTO invCountHeader = invCountHeaderService.selectDetail(countHeaderId);
        return Results.success(invCountHeader);
    }

    @ApiOperation(value = "Save Counting Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/order-save")
    public ResponseEntity<InvCountInfoDTO> orderSave(@PathVariable Long organizationId,
                                                     @RequestBody List<InvCountHeader> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.Save.class);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        return Results.success(invCountHeaderService.orderSave(invCountHeaders));
    }

    @ApiOperation(value = "Delete Counting Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @DeleteMapping
    public ResponseEntity<?> orderRemove(@PathVariable Long organizationId,
                                         @RequestBody List<InvCountHeader> invCountHeaders) {
        SecurityTokenHelper.validToken(invCountHeaders);
        return Results.success(invCountHeaderService.orderRemove(invCountHeaders));
    }

    @ApiOperation(value = "Execute Counting Order")
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ProcessCacheValue
    @PostMapping("/order-execution")
    public ResponseEntity<InvCountInfoDTO> orderExecution(@PathVariable Long organizationId,
                                                          @RequestBody List<InvCountHeader> invCountHeaders) {
        validList(invCountHeaders, InvCountHeader.Execute.class);
        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
        return Results.success(invCountHeaderService.executeCheck(invCountHeaders));
    }


    //    @ApiOperation(value = "Delete")
    //    @Permission(level = ResourceLevel.ORGANIZATION)
    //    @DeleteMapping
    //    public ResponseEntity<?> remove(@PathVariable Long organizationId,
    //                                    @RequestBody List<InvCountHeader> invCountHeaders) {
    //        SecurityTokenHelper.validToken(invCountHeaders);
    //        invCountHeaderRepository.batchDeleteByPrimaryKey(invCountHeaders);
    //        return Results.success();
    //    }

    //    @ApiOperation(value = "Create or Update")
    //    @Permission(level = ResourceLevel.ORGANIZATION)
    //    @ProcessCacheValue
    //    @PostMapping
    //    public ResponseEntity<List<InvCountHeader>> save(@PathVariable Long organizationId,
    //                                                     @RequestBody List<InvCountHeader> invCountHeaders) {
    //        validList(invCountHeaders);
    //        SecurityTokenHelper.validTokenIgnoreInsert(invCountHeaders);
    //        invCountHeaders.forEach(item -> item.setTenantId(organizationId));
    //        invCountHeaderService.saveData(invCountHeaders);
    //        return Results.success(invCountHeaders);
    //    }
}

