package com.ajohnson.rwa.ui;

import com.ajohnson.rwa.onchain.EthereumReadService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Route(value = "onchain", layout = MainLayout.class)
@Component
public class LookupView extends VerticalLayout {

    public LookupView(EthereumReadService ethService) {

        setSpacing(true);
        setPadding(true);

        add(new Text("Ethereum On-Chain Balance Lookup (Read-Only)"));

        TextField addressInput = new TextField("Address or ENS");
        addressInput.setPlaceholder("vitalik.eth or 0x...");

        TextField tokenInput = new TextField("ERC-20 Contract (optional)");
        tokenInput.setPlaceholder("0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48");

        TextArea output = new TextArea("Result");
        output.setWidthFull();
        output.setMinHeight("220px");
        output.setReadOnly(true);

        Button lookup = new Button("Lookup");

        lookup.addClickListener(e -> {
            try {
                if (addressInput.isEmpty()) {
                    Notification.show(
                            "Address or ENS is required",
                            3000,
                            Notification.Position.MIDDLE
                    );
                    return;
                }

                String resolved =
                        ethService.resolveAddress(addressInput.getValue());

                BigInteger ethBalance =
                        ethService.getEthBalance(resolved);

                StringBuilder result = new StringBuilder();
                result.append("Resolved address: ")
                        .append(resolved)
                        .append("\nETH balance (wei): ")
                        .append(ethBalance);

                if (!tokenInput.isEmpty()) {
                    BigInteger tokenBalance =
                            ethService.getErc20Balance(
                                    resolved,
                                    tokenInput.getValue()
                            );

                    String tokenName;
                    String tokenSymbol;

                    try {
                        tokenName =
                                ethService.getTokenName(tokenInput.getValue());
                        tokenSymbol =
                                ethService.getTokenSymbol(tokenInput.getValue());
                    } catch (Exception metadataEx) {
                        tokenName = "Unknown Token";
                        tokenSymbol = "?";
                    }

                    result.append("\n\nToken: ")
                            .append(tokenName)
                            .append(" (")
                            .append(tokenSymbol)
                            .append(")")
                            .append("\nRaw token balance: ")
                            .append(tokenBalance);
                }

                output.setValue(result.toString());

            } catch (Exception ex) {
                output.setValue("Error: " + ex.getMessage());
            }
        });

        add(addressInput, tokenInput, lookup, output);
    }
}