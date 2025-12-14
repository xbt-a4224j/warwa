package com.ajohnson.rwa.ui;

import com.ajohnson.rwa.service.TokenLedgerService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.stereotype.Component;

import java.util.Map;

@Route(value = "", layout = MainLayout.class)
@Component
public class MainView extends VerticalLayout {

    private static final String TOKEN_ID = "PCI-A";

    private final TokenLedgerService ledgerService;
    private final Grid<BalanceRow> grid = new Grid<>(BalanceRow.class, false);
    private final TextArea explanationArea = new TextArea("Explanation");

    public MainView(TokenLedgerService ledgerService) {
        this.ledgerService = ledgerService;

        setSpacing(true);
        setPadding(true);

        add(new Text("Tokenized RWA Ledger – Internal Prototype"));

        configureGrid();
        refreshGrid();

        add(grid);
        add(buildTransferForm());
        add(explanationArea);
    }

    // ------------------------------------------------------------------

    private void configureGrid() {
        grid.addColumn(BalanceRow::client).setHeader("Client");
        grid.addColumn(BalanceRow::balance).setHeader("Balance");
        grid.setAllRowsVisible(true);
    }

    private void refreshGrid() {
        Map<String, Integer> balances =
                ledgerService.balancesForToken(TOKEN_ID);

        grid.setItems(
                balances.entrySet().stream()
                        .map(e -> new BalanceRow(e.getKey(), e.getValue()))
                        .toList()
        );
    }

    private FormLayout buildTransferForm() {
        TextField fromClient = new TextField("From Client");
        TextField toClient = new TextField("To Client");
        IntegerField amount = new IntegerField("Amount");

        Button submit = new Button("Submit Transfer");

        submit.addClickListener(e -> {
            try {
                String explanation =
                        ledgerService.requestTransferWithExplanation(
                                TOKEN_ID,
                                fromClient.getValue(),
                                toClient.getValue(),
                                amount.getValue()
                        );

                explanationArea.setValue(explanation);
                refreshGrid();

            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3_000, Notification.Position.MIDDLE);
            }
        });

        FormLayout form = new FormLayout(
                fromClient,
                toClient,
                amount,
                submit
        );

        form.setColspan(submit, 2);
        return form;
    }

    // ------------------------------------------------------------------

    record BalanceRow(String client, Integer balance) {}
}