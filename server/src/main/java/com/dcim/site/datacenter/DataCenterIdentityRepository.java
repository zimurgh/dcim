package com.dcim.site.datacenter;

import org.springframework.data.jpa.repository.JpaRepository;

interface DataCenterIdentityRepository extends JpaRepository<DataCenterIdentity, Long> {
}
