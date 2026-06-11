package cn.edu.cn.javadiet.model.dto;

public record AdminDashboardStats(
        long userCount,
        long foodCount,
        long restaurantCount,
        long feedbackCount) {
}
