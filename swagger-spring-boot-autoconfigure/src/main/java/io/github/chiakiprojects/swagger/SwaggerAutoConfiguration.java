package io.github.chiakiprojects.swagger;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import springfox.documentation.RequestHandler;
import springfox.documentation.annotations.ApiIgnore;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.swagger2.configuration.Swagger2DocumentationConfiguration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jianglin
 * @date 2018/9/20
 */
@Slf4j
@EnableSwagger2
@ConditionalOnClass({Swagger2DocumentationConfiguration.class})
@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerAutoConfiguration {

    private SwaggerProperties swaggerProperties;
    private GenericApplicationContext applicationContext;

    public SwaggerAutoConfiguration(SwaggerProperties swaggerProperties,
                                    GenericApplicationContext applicationContext) {
        this.swaggerProperties = swaggerProperties;
        this.applicationContext = applicationContext;
    }

    @Bean
    public Docket docket() {
        ApiSelectorBuilder builder = new Docket(DocumentationType.SWAGGER_2)
                .groupName(Docket.DEFAULT_GROUP_NAME)
                .select();
        List<Predicate<RequestHandler>> andPredicateList = new ArrayList<>();
        List<Predicate<RequestHandler>> notPredicateList = new ArrayList<>();
        if (swaggerProperties.getExcludeArchitectureApi()) {
            Set<String> packages = getComponentScanningPackages((BeanDefinitionRegistry) applicationContext.getBeanFactory());
            andPredicateList.addAll(packages.stream()
                    .filter(pkg -> !StringUtils.startsWithIgnoreCase(pkg, "springfox"))
                    .map(RequestHandlerSelectors::basePackage)
                    .collect(Collectors.toList()));
        }
        if (!swaggerProperties.getIncludePackages().isEmpty()) {
            andPredicateList.addAll(swaggerProperties.getIncludePackages()
                    .stream()
                    .map(RequestHandlerSelectors::basePackage)
                    .collect(Collectors.toList())
            );
        }
        if (!swaggerProperties.getExcludePackages().isEmpty()) {
            notPredicateList.addAll(
                    swaggerProperties.getExcludePackages()
                            .stream()
                            .map(RequestHandlerSelectors::basePackage)
                            .collect(Collectors.toList())
            );
        } else {
            notPredicateList.add(Predicates.alwaysFalse());
        }
        builder.apis(Predicates.and(
                Predicates.and(andPredicateList),
                Predicates.not(Predicates.and(notPredicateList)))
        );
        return builder.build().ignoredParameterTypes(ApiIgnore.class)
                .enableUrlTemplating(false);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(swaggerProperties.getAppName() + " APIs")
                .description(swaggerProperties.getDescription())
                .version(swaggerProperties.getVersion())
                .build();
    }


    @Bean
    public UiConfiguration uiConfig() {

        return UiConfigurationBuilder.builder()
                .deepLinking(true)
                .displayOperationId(false)
                .defaultModelsExpandDepth(1)
                .defaultModelExpandDepth(1)
                .defaultModelRendering(ModelRendering.EXAMPLE)
                .displayRequestDuration(true)
                .docExpansion(DocExpansion.LIST)
                .filter(false)
                .maxDisplayedTags(null)
                .operationsSorter(OperationsSorter.ALPHA)
                .showExtensions(false)
                .tagsSorter(TagsSorter.ALPHA)
                .supportedSubmitMethods(UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS)
                .validatorUrl(null)
                .build();
    }


    protected Set<String> getComponentScanningPackages(
            BeanDefinitionRegistry registry) {
        Set<String> packages = new LinkedHashSet<>();
        String[] names = registry.getBeanDefinitionNames();
        for (String name : names) {
            BeanDefinition definition = registry.getBeanDefinition(name);
            if (definition instanceof AnnotatedBeanDefinition) {
                AnnotatedBeanDefinition annotatedDefinition = (AnnotatedBeanDefinition) definition;
                addComponentScanningPackages(packages,
                        annotatedDefinition.getMetadata());
            }
        }
        return packages;
    }

    private void addComponentScanningPackages(Set<String> packages,
                                              AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
                .getAnnotationAttributes(ComponentScan.class.getName(), true));
        if (attributes != null) {
            addPackages(packages, attributes.getStringArray("value"));
            addPackages(packages, attributes.getStringArray("basePackages"));
            addClasses(packages, attributes.getStringArray("basePackageClasses"));
            if (packages.isEmpty()) {
                packages.add(ClassUtils.getPackageName(metadata.getClassName()));
            }
        }
    }

    private void addPackages(Set<String> packages, String[] values) {
        if (values != null) {
            Collections.addAll(packages, values);
        }
    }

    private void addClasses(Set<String> packages, String[] values) {
        if (values != null) {
            for (String value : values) {
                packages.add(ClassUtils.getPackageName(value));
            }
        }
    }

}
