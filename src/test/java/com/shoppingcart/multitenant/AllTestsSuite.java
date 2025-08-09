package com.shoppingcart.multitenant;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Multi-Tenant Shopping Cart Test Suite")
@SelectPackages({
    "com.shoppingcart.multitenant.config",
    "com.shoppingcart.multitenant.service",
    "com.shoppingcart.multitenant.controller"
})
public class AllTestsSuite {
    // This class is used to run all tests together
    // The @SelectPackages annotation specifies which packages to include
}