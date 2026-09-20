package com.mycompany.app.account;

import java.util.*;

public class Admin extends Account {

    public boolean blockMember() {
        // Logic to block member
        return true;
    }

    public boolean unblockMember() {
        // Logic to unblock member
        return true;
    }

    public boolean cancelMembership() {
        // Logic to cancel membership
        return true;
    }

    @Override
    public boolean resetPassword() {
        // Logic to reset admin password
        return true;
    }
}
