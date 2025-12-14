package com.ajohnson.rwa.onchain;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.ens.EnsResolver;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class EthereumReadService {

    private final Web3j web3j;
    private final EnsResolver ensResolver;


    // test constructor
    EthereumReadService(Web3j web3j, EnsResolver ensResolver) {
        this.web3j = web3j;
        this.ensResolver = ensResolver;
    }

    public EthereumReadService(String rpcUrl) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.ensResolver = new EnsResolver(web3j);
    }

    public String resolveAddress(String addressOrEns) {
        if (addressOrEns.endsWith(".eth")) {
            return ensResolver.resolve(addressOrEns);
        }
        return addressOrEns;
    }

    public BigInteger getEthBalance(String address) throws Exception {
        EthGetBalance balance =
                web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return balance.getBalance();
    }

    public BigInteger getErc20Balance(
            String walletAddress,
            String tokenContract
    ) throws Exception {

        Function function = new Function(
                "balanceOf",
                List.of(new Address(walletAddress)),
                List.of(new TypeReference<Uint256>() {})
        );

        String data = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        walletAddress,
                        tokenContract,
                        data
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        List<?> decoded = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        Uint256 balance = (Uint256) decoded.get(0);
        return balance.getValue();
    }

    public String getTokenName(String tokenContract) {

        Function function = new Function(
                "name",
                List.of(),
                List.of(new TypeReference<Utf8String>() {})
        );

        String data = FunctionEncoder.encode(function);

        EthCall response = null;
        try {
            response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            tokenContract,
                            data
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();
        } catch (IOException e) {
            throw new RuntimeException("Error calling out to eth rpc", e);
        }

        Utf8String name = (Utf8String) FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()

        ).get(0);

        return name.getValue();
    }

    public String getTokenSymbol(String tokenContract) {

        Function function = new Function(
                "symbol",
                List.of(),
                List.of(new TypeReference<Utf8String>() {})
        );

        String data = FunctionEncoder.encode(function);

        EthCall response = null;
        try {
            response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            tokenContract,
                            data
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();
        } catch (IOException e) {
            throw new RuntimeException("Error calling out to eth rpc", e);
        }

        Utf8String symbol = (Utf8String) FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        ).get(0);

        return symbol.getValue();
    }
}
