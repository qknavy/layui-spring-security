package com.laungcisin.security.core.validate.code.service.impl;

import com.laungcisin.security.core.properties.SecurityConstants;
import com.laungcisin.security.core.validate.code.bean.ValidateCode;
import com.laungcisin.security.core.validate.code.bean.ValidateCodeType;
import com.laungcisin.security.core.validate.code.exception.ValidateCodeException;
import com.laungcisin.security.core.validate.code.generator.ValidateCodeGenerator;
import com.laungcisin.security.core.validate.code.repository.ValidateCodeRepository;
import com.laungcisin.security.core.validate.code.service.ValidateCodeProcessor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

/**
 * @author laungcisin
 */
public abstract class AbstractValidateCodeProcessor<C extends ValidateCode> implements ValidateCodeProcessor {

    /**
     * 收集系统中所有的 {@link ValidateCodeGenerator} 接口的实现。
     * spring依赖搜索
     */
    @Autowired
    private Map<String, ValidateCodeGenerator> validateCodeGenerators;

    @Autowired
    private ValidateCodeRepository validateCodeRepository;

    /**
     * 模板方法--主逻辑
     * 生成校验码，图形验证码或者短信验证码都是需要这3个步骤。
     * 生成-->存储-->发送
     */
    @Override
    public final void create(ServletWebRequest request) throws Exception {
        //1.生成校验码
        C validateCode = generate(request);
        //2.存储校验码
        save(request, validateCode);
        //3.发送校验码
        send(request, validateCode);
    }

    /**
     * 生成校验码
     *
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    private C generate(ServletWebRequest request) {
        String type = getValidateCodeType(request).toString().toLowerCase();

        // 校验码Generator上Component注解的value为: [type]ValidateCodeGenerator,
        // 所以拼装的name为[type]ValidateCodeGenerator
        String generatorName = type + ValidateCodeGenerator.class.getSimpleName();//验证码生成器类名
        ValidateCodeGenerator validateCodeGenerator = validateCodeGenerators.get(generatorName);
        if (validateCodeGenerator == null) {
            throw new ValidateCodeException("验证码生成器" + generatorName + "不存在");
        }
        return (C) validateCodeGenerator.generate(request);
    }

    /**
     * 保存校验码
     *
     * @param request
     * @param validateCode
     */
    private void save(ServletWebRequest request, C validateCode) {
        ValidateCode code = new ValidateCode(validateCode.getCode(), validateCode.getExpireTime());
        validateCodeRepository.save(request, code, getValidateCodeType(request));
    }

    /**
     * 发送校验码，由子类实现
     *
     * @param request
     * @param validateCode
     * @throws Exception
     */
    protected abstract void send(ServletWebRequest request, C validateCode) throws Exception;

    /**
     * 构建验证码放入session时的key
     *
     * @param request
     * @return
     */
    private String getSessionKey(ServletWebRequest request) {
        return SESSION_KEY_PREFIX + getValidateCodeType(request).toString().toUpperCase();
    }

    /**
     * 根据请求的url获取校验码的类型
     *
     * @param request
     * @return
     */
    private ValidateCodeType getValidateCodeType(ServletWebRequest request) {
        String type = StringUtils.substringBefore(getClass().getSimpleName(), SecurityConstants.VALIDATE_CODE_SEPARATOR_WORD);
        return ValidateCodeType.valueOf(type.toUpperCase());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validate(ServletWebRequest request) {
        ValidateCodeType codeType = getValidateCodeType(request);
        C codeInSession = (C) validateCodeRepository.get(request, codeType);
        String codeInRequest;

        try {
            codeInRequest = ServletRequestUtils.getStringParameter(request.getRequest(), codeType.getParamNameOnValidate());
        } catch (ServletRequestBindingException e) {
            throw new ValidateCodeException("获取验证码的值失败");
        }

        if (StringUtils.isBlank(codeInRequest)) {
            throw new ValidateCodeException(codeType.getTypeName() + "验证码的值不能为空");
        }

        if (codeInSession == null) {
            throw new ValidateCodeException(codeType.getTypeName() + "验证码不存在");
        }

        if (codeInSession.isExpired()) {
            validateCodeRepository.remove(request, codeType);
            throw new ValidateCodeException(codeType.getTypeName() + "验证码已过期");
        }

        if (!StringUtils.equals(codeInSession.getCode(), codeInRequest)) {
            throw new ValidateCodeException(codeType.getTypeName() + "验证码不匹配");
        }

        validateCodeRepository.remove(request, codeType);
    }

}
