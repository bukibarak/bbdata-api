package ch.derlin.bbdata.output.security

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * date: 15.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

// ------ userId resolution in controllers: @UserId userId: Int

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.VALUE_PARAMETER)
@Parameter(hidden = true) // hide it in OpenAPI doc
annotation class UserId

@Component
class UserIdHandlerMethodArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(methodParameter: MethodParameter): Boolean {
        return methodParameter.parameterType == Int::class.java &&
                methodParameter.hasParameterAnnotation(UserId::class.java)
    }

    @Throws(Exception::class)
    override fun resolveArgument(parameter: MethodParameter,
                                 @Nullable modelAndViewContainer: ModelAndViewContainer?,
                                 nativeWebRequest: NativeWebRequest,
                                 @Nullable webDataBinderFactory: WebDataBinderFactory?): Any? {
        return nativeWebRequest.getAttribute(SecurityConstants.HEADER_USER, 0)?.let {
            (it as String).toIntOrNull()
        }
    }
}


@Configuration
class UserIdWebConfig : WebMvcConfigurer {
    @Autowired
    private lateinit var useridResolver: UserIdHandlerMethodArgumentResolver

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(useridResolver)
    }
}