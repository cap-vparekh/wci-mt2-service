/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.refsetservice.model;

/**
 * Generically represents something that has an id.
 *
 * @param <T> the
 */
public interface Copyable<T> {

  /**
   * Populate from (used by copy constructor).
   *
   * @param other the other
   */
  public void populateFrom(T other);

  /**
   * Patch from.
   *
   * @param other the other
   */
  public void patchFrom(T other);

}
