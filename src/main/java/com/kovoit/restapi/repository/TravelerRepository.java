package com.kovoit.restapi.repository;

import com.kovoit.restapi.document.TravelerDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TravelerRepository extends ElasticsearchRepository<TravelerDocument, String> {}
