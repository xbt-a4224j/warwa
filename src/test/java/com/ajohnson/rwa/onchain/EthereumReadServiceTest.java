package com.ajohnson.rwa.onchain;

import org.junit.jupiter.api.Test;
import org.web3j.ens.EnsResolver;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EthereumReadServiceTest {

    @Test
    void resolvesEnsAndReturnsEthBalance() throws Exception {
        Web3j web3j = mock(Web3j.class);
        EnsResolver ensResolver = mock(EnsResolver.class);

        // ENS stub
        when(ensResolver.resolve("vitalik.eth"))
                .thenReturn("0xabc123");

        // balance response
        EthGetBalance balanceResp = mock(EthGetBalance.class);
        when(balanceResp.getBalance())
                .thenReturn(BigInteger.valueOf(42));

        // request wrapper (this is the missing piece)
        @SuppressWarnings("unchecked")
        Request<?, EthGetBalance> request = mock(Request.class);
        when(request.send()).thenReturn(balanceResp);

        doReturn(request).when(web3j).ethGetBalance(
                eq("0xabc123"),
                eq(DefaultBlockParameterName.LATEST));

        EthereumReadService service =
                new EthereumReadService(web3j, ensResolver);

        String resolved = service.resolveAddress("vitalik.eth");
        BigInteger balance = service.getEthBalance(resolved);

        assertEquals("0xabc123", resolved);
        assertEquals(BigInteger.valueOf(42), balance);
    }
}