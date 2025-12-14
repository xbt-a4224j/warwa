package com.ajohnson.rwa.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {

        Tab rwaTab = new Tab(
                new RouterLink("Internal RWA Ledger", MainView.class)
        );

        Tab onChainTab = new Tab(
                new RouterLink("On-Chain Lookup", LookupView.class)
        );

        Tabs tabs = new Tabs(rwaTab, onChainTab);
        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);

        addToNavbar(tabs);
    }
}