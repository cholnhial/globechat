package dev.chol.globechat.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Runtime hints for GraalVM native image compilation.
 * Registers reflection, resources, and serialization hints needed at runtime.
 */
@Configuration
@ImportRuntimeHints(NativeImageConfig.GlobechatRuntimeHints.class)
public class NativeImageConfig {

    static class GlobechatRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register resource patterns
            hints.resources().registerPattern("schema.sql");
            hints.resources().registerPattern("static/*");
            hints.resources().registerPattern("templates/*");
            hints.resources().registerPattern("config/*");
            hints.resources().registerPattern("application*.yaml");
            hints.resources().registerPattern("application*.yml");
            hints.resources().registerPattern("application*.properties");

            // Register reflection hints for DTOs (for JSON serialization)
            registerDtoHints(hints);

            // Register reflection hints for entities
            registerEntityHints(hints);

            // Register JJWT reflection hints
            registerJjwtHints(hints);
        }

        private void registerJjwtHints(RuntimeHints hints) {
            // JJWT classes need reflection for native image
            String[] jjwtClasses = {
                // Core API classes
                "io.jsonwebtoken.security.Keys",
                "io.jsonwebtoken.Jwts",
                "io.jsonwebtoken.Jwts$SIG",
                "io.jsonwebtoken.Jwts$ENC",
                "io.jsonwebtoken.Jwts$KEY",
                "io.jsonwebtoken.Jwts$ZIP",
                "io.jsonwebtoken.JwtBuilder",
                "io.jsonwebtoken.JwtParser",
                "io.jsonwebtoken.Claims",
                "io.jsonwebtoken.ClaimsBuilder",
                "io.jsonwebtoken.Header",
                "io.jsonwebtoken.HeaderBuilder",
                // Implementation classes - builders
                "io.jsonwebtoken.impl.DefaultJwtBuilder",
                "io.jsonwebtoken.impl.DefaultJwtParser",
                "io.jsonwebtoken.impl.DefaultJwtParserBuilder",
                "io.jsonwebtoken.impl.DefaultClaims",
                "io.jsonwebtoken.impl.DefaultClaimsBuilder",
                "io.jsonwebtoken.impl.DefaultHeader",
                "io.jsonwebtoken.impl.DefaultHeaderBuilder",
                "io.jsonwebtoken.impl.DefaultTokenizedJwt",
                "io.jsonwebtoken.impl.DefaultJweHeader",
                "io.jsonwebtoken.impl.DefaultJweHeaderBuilder",
                "io.jsonwebtoken.impl.DefaultJwsHeader",
                "io.jsonwebtoken.impl.DefaultJwsHeaderBuilder",
                "io.jsonwebtoken.impl.DefaultJwt",
                "io.jsonwebtoken.impl.DefaultJwe",
                "io.jsonwebtoken.impl.DefaultJws",
                // Security implementation classes
                "io.jsonwebtoken.impl.security.KeysBridge",
                "io.jsonwebtoken.impl.security.DefaultKeyOperationBuilder",
                "io.jsonwebtoken.impl.security.DefaultMacAlgorithm",
                "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms",
                "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms$1",
                "io.jsonwebtoken.impl.security.StandardKeyOperations",
                "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms",
                "io.jsonwebtoken.impl.security.StandardKeyAlgorithms",
                "io.jsonwebtoken.impl.security.AesGcmKeyAlgorithm",
                "io.jsonwebtoken.impl.security.DirectKeyAlgorithm",
                "io.jsonwebtoken.impl.security.EcdhKeyAlgorithm",
                "io.jsonwebtoken.impl.security.HmacAesAeadAlgorithm",
                "io.jsonwebtoken.impl.security.GcmAesAeadAlgorithm",
                "io.jsonwebtoken.impl.security.Pbes2HsAkwAlgorithm",
                "io.jsonwebtoken.impl.security.RsaKeyAlgorithm",
                "io.jsonwebtoken.impl.security.DefaultHashAlgorithm",
                "io.jsonwebtoken.impl.security.DefaultJwkContext",
                "io.jsonwebtoken.impl.security.DefaultRequest",
                "io.jsonwebtoken.impl.security.AbstractSecureDigestAlgorithm",
                // Lang/IO classes
                "io.jsonwebtoken.impl.lang.Services",
                "io.jsonwebtoken.impl.lang.DefaultRegistry",
                "io.jsonwebtoken.impl.lang.Parameter",
                "io.jsonwebtoken.impl.lang.Parameters",
                "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms",
                "io.jsonwebtoken.impl.io.Streams",
                "io.jsonwebtoken.impl.io.DeflateCompressionAlgorithm",
                "io.jsonwebtoken.impl.io.GzipCompressionAlgorithm",
                // Jackson integration
                "io.jsonwebtoken.jackson.io.JacksonSerializer",
                "io.jsonwebtoken.jackson.io.JacksonDeserializer",
                "io.jsonwebtoken.io.Serializer",
                "io.jsonwebtoken.io.Deserializer",
                // Security algorithms interfaces
                "io.jsonwebtoken.security.MacAlgorithm",
                "io.jsonwebtoken.security.SignatureAlgorithm",
                "io.jsonwebtoken.security.KeyAlgorithm",
                "io.jsonwebtoken.security.AeadAlgorithm",
                "io.jsonwebtoken.security.SecureDigestAlgorithm",
                "io.jsonwebtoken.security.HashAlgorithm"
            };

            for (String className : jjwtClasses) {
                try {
                    hints.reflection().registerType(
                        Class.forName(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.ACCESS_DECLARED_FIELDS,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.ACCESS_PUBLIC_FIELDS
                    );
                } catch (ClassNotFoundException e) {
                    // Class may not exist in classpath, skip
                }
            }

            // Register JJWT service loader resources
            hints.resources().registerPattern("META-INF/services/io.jsonwebtoken.*");
        }

        private void registerDtoHints(RuntimeHints hints) {
            // DTOs need reflection for JSON serialization
            String[] dtoClasses = {
                "dev.chol.globechat.dto.AuthResponse",
                "dev.chol.globechat.dto.BanRequest",
                "dev.chol.globechat.dto.ChatMessageDto",
                "dev.chol.globechat.dto.CreateMoodsicRequest",
                "dev.chol.globechat.dto.CreateRoomRequest",
                "dev.chol.globechat.dto.ErrorResponse",
                "dev.chol.globechat.dto.LoginRequest",
                "dev.chol.globechat.dto.MoodsicDto",
                "dev.chol.globechat.dto.RegisterRequest",
                "dev.chol.globechat.dto.RoomDto",
                "dev.chol.globechat.dto.RoomMarkerDto",
                "dev.chol.globechat.dto.RoomMemberDto",
                "dev.chol.globechat.dto.SendMessageRequest",
                "dev.chol.globechat.dto.SetMoodsicRequest",
                "dev.chol.globechat.dto.UpdateRoomRequest",
                "dev.chol.globechat.dto.UserDto"
            };

            for (String className : dtoClasses) {
                try {
                    hints.reflection().registerType(
                        Class.forName(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.ACCESS_DECLARED_FIELDS
                    );
                } catch (ClassNotFoundException e) {
                    // Class may not exist, skip
                }
            }
        }

        private void registerEntityHints(RuntimeHints hints) {
            // Entities need reflection for JPA/Hibernate
            String[] entityClasses = {
                "dev.chol.globechat.entity.User",
                "dev.chol.globechat.entity.ChatRoom",
                "dev.chol.globechat.entity.ChatRoomMember",
                "dev.chol.globechat.entity.ChatMessage",
                "dev.chol.globechat.entity.Moodsic",
                "dev.chol.globechat.entity.RoomBan",
                "dev.chol.globechat.entity.MemberRole",
                "dev.chol.globechat.entity.MessageType"
            };

            for (String className : entityClasses) {
                try {
                    hints.reflection().registerType(
                        Class.forName(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.ACCESS_DECLARED_FIELDS,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.ACCESS_PUBLIC_FIELDS
                    );
                } catch (ClassNotFoundException e) {
                    // Class may not exist, skip
                }
            }
        }
    }
}
