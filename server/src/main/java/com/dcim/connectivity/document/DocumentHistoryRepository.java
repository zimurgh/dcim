package com.dcim.connectivity.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

	@Query("""
			select d from DocumentHistory d
			where d.validTo is null
			order by d.documentName
			""")
	List<DocumentHistory> findCurrent();

	@Query("""
			select d from DocumentHistory d
			where d.documentIdentity.documentId = :documentId and d.validTo is null
			""")
	Optional<DocumentHistory> findCurrentByDocumentId(@Param("documentId") Long documentId);

	@Query("""
			select d from DocumentHistory d
			where d.crossConnectIdentity.crossConnectId = :crossConnectId and d.validTo is null
			order by d.documentName
			""")
	List<DocumentHistory> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	List<DocumentHistory> findByDocumentIdentity_DocumentIdOrderByDocumentHistoryIdAsc(Long documentId);
}
