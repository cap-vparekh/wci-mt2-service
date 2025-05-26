/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.refsetservice.configuration;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/**
 * Configure analyzers for the Hibernate Search Lucene backend.
 */
public class LuceneCustomAnalysisConfigurer implements LuceneAnalysisConfigurer {

    /* see superclass */
    @Override
    public void configure(final LuceneAnalysisConfigurationContext context) {

        context.normalizer("lowercase").custom();
    }
}
