package com.ajohnson.rwa.config;

import com.ajohnson.rwa.onchain.EthereumReadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "onchain.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class OnchainConfig {

    @Bean
    public EthereumReadService ethereumReadService(
            @Value("${onchain.alchemy.eth.rpc.url}") String rpcUrl
    ) {
        System.out.println("Alchemy RPC URL = " + rpcUrl);
        return new EthereumReadService(rpcUrl);
    }
}