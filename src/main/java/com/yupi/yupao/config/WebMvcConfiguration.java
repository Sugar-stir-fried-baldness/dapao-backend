package com.yupi.yupao.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @Author:tzy
 * @Description :
 * @Date:2024/3/817:06
 */
@Configuration
@Slf4j
@Profile({"dev", "test"})
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docket() {
        log.info("进入docket函数 的swagger");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("伙伴匹配项目接口文档")
                .version("2.0")
                .description("伙伴匹配项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.yupi.yupao.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("设置静态资源映射");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    // 基本信息设置
    private ApiInfo apiInfo() {
        log.info("进入Slf4j 的基本信息设置");
        Contact contact = new Contact(
                "shayu", // 作者姓名
                "shayuyu.cn", // 作者网址
                "shayu-yusha@qq.com"); // 作者邮箱
        return new ApiInfoBuilder()
                .title("鱼泡伙伴匹配系统-接口文档") // 标题
                .description("众里寻他千百度，慕然回首那人却在灯火阑珊处") // 描述
                .termsOfServiceUrl("https://www.baidu.com") // 跳转连接
                .version("1.0") // 版本
                .license("Swagger-的使用(详细教程)")
                .licenseUrl("https://blog.csdn.net/xhmico/article/details/125353535")
                .contact(contact)
                .build();
    }

//    /**
//     * 设置静态资源映射
//     * @param registry
//     */
//    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
//
//        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
//        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
//        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
//    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //设置允许跨域的路径
        registry.addMapping("/**")
                //设置允许跨域请求的域名
                //当**Credentials为true时，**Origin不能为星号，需为具体的ip地址【如果接口不带cookie,ip无需设成具体ip】
                /* 放自己的前端域名*/
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "http://127.0.0.1:8082", "http://127.0.0.1:8083")
                //是否允许证书 不再默认开启
                .allowCredentials(true)
                //设置允许的方法
                .allowedMethods("*")
                //跨域允许时间
                .maxAge(3600);
    }
}



