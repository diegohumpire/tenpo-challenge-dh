package com.tenpo.dh.challenge.dhapi.config;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import io.r2dbc.postgresql.codec.EnumCodec;
import io.r2dbc.postgresql.extension.CodecRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public CodecRegistrar enumCodecRegistrar() {
        return EnumCodec.builder()
                .withEnum("audit_action_type", AuditActionType.class)
                .withEnum("call_direction", CallDirection.class)
                .build();
    }
}
