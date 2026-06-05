package com.kovoit.restapi.repository;

import com.kovoit.restapi.document.CompanyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CompanyRepository extends ElasticsearchRepository<CompanyDocument, String> {}