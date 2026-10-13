package com.library.membership.service;

import com.library.config.DatabaseConnectionManager;
import com.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Membership Microservice: Validates member accounts, loan limits, and membership expiry.
 */
public class MembershipService {

    public Optional<Member> getMemberById(Long memberId) throws SQLException {
        String sql = "SELECT member_id, user_id, membership_no, first_name, last_name, email, phone, " +
                     "membership_status, membership_tier, max_active_loans, current_loan_count, joined_date, expiry_date " +
                     "FROM members WHERE member_id = ?";

        try (Connection conn = DatabaseConnectionManager.getLibraryConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Member member = new Member();
                    member.setMemberId(rs.getLong("member_id"));
                    member.setUserId(rs.getLong("user_id"));
                    member.setMembershipNo(rs.getString("membership_no"));
                    member.setFirstName(rs.getString("first_name"));
                    member.setLastName(rs.getString("last_name"));
                    member.setEmail(rs.getString("email"));
                    member.setPhone(rs.getString("phone"));
                    member.setMembershipStatus(rs.getString("membership_status"));
                    member.setMembershipTier(rs.getString("membership_tier"));
                    member.setMaxActiveLoans(rs.getInt("max_active_loans"));
                    member.setCurrentLoanCount(rs.getInt("current_loan_count"));
                    java.sql.Date joined = rs.getDate("joined_date");
                    if (joined != null) member.setJoinedDate(joined.toLocalDate());
                    java.sql.Date expiry = rs.getDate("expiry_date");
                    if (expiry != null) member.setExpiryDate(expiry.toLocalDate());
                    return Optional.of(member);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a member is eligible to borrow or reserve.
     */
    public void validateMemberEligibility(Long memberId) throws SQLException {
        Optional<Member> memberOpt = getMemberById(memberId);
        if (memberOpt.isEmpty()) {
            throw new IllegalArgumentException("Member ID " + memberId + " not found.");
        }
        Member member = memberOpt.get();

        if (!"ACTIVE".equalsIgnoreCase(member.getMembershipStatus())) {
            throw new IllegalStateException("Member status is " + member.getMembershipStatus() + ". Must be ACTIVE.");
        }

        if (member.getExpiryDate() != null && member.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Membership expired on " + member.getExpiryDate());
        }

        if (member.getCurrentLoanCount() >= member.getMaxActiveLoans()) {
            throw new IllegalStateException("Member has reached maximum active loans limit (" + member.getMaxActiveLoans() + ").");
        }
    }
}
