package com.dcim.organization.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {

	@Query("""
			select u from UserHistory u
			where u.validTo is null
			order by u.userName
			""")
	List<UserHistory> findCurrentUsers();

	@Query("""
			select u from UserHistory u
			where u.userIdentity.userId = :userId and u.validTo is null
			""")
	Optional<UserHistory> findCurrentByUserId(@Param("userId") Long userId);

	List<UserHistory> findByUserIdentity_UserIdOrderByUserHistoryIdAsc(Long userId);

	@Query("""
			select count(u) > 0 from UserHistory u
			where u.validTo is null and u.status = 'Active'
			and lower(u.userName) = lower(:name)
			and (:excludeId is null or u.userIdentity.userId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);
}
