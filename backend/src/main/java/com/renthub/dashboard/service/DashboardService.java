package com.renthub.dashboard.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getOwnerDashboardStats();
    Map<String, Object> getAdminDashboardStats();
}
