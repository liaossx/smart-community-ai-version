package com.lsx.property.express.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsx.property.express.dto.ExpressAuthorizeDTO;
import com.lsx.property.express.dto.ExpressCreateDTO;
import com.lsx.property.express.dto.ExpressPickDTO;
import com.lsx.property.express.entity.Express;
import com.lsx.property.express.vo.ExpressVO;

public interface ExpressService extends IService<Express> {

    Long registerExpress(ExpressCreateDTO dto);

    Page<ExpressVO> listMyExpress(Long userId, Integer pageNum, Integer pageSize);

    Boolean pickExpress(Long expressId, ExpressPickDTO dto);

    Boolean authorizeExpress(Long expressId, ExpressAuthorizeDTO dto);
}
