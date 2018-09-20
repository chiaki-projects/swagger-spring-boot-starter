package io.github.chiakiprojects.swagger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jianglin
 * @date 2018/9/20
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "swagger")
public class SwaggerProperties {

    @Value("${appName:${spring.application.name}}")
    private String appName;

    private String description = "";

    private String version = "v1";
    /**
     * 排除基础框架接口(eg. springfox, error)
     */
    private Boolean excludeArchitectureApi = Boolean.TRUE;

    private List<String> includePackages = new ArrayList<>();

    private List<String> excludePackages = new ArrayList<>();
}
