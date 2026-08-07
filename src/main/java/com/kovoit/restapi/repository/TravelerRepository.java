package com.kovoit.restapi.repository;

import com.kovoit.restapi.document.TravelerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface TravelerRepository extends ElasticsearchRepository<TravelerDocument, String> {

    List<TravelerDocument> findByCompanyId(String companyId);
}
