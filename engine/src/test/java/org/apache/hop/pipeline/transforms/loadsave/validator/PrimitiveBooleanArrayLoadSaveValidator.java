/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.loadsave.validator;

import java.util.Random;
import org.apache.hop.core.exception.HopException;

public class PrimitiveBooleanArrayLoadSaveValidator implements IFieldLoadSaveValidator<boolean[]> {
  private final IFieldLoadSaveValidator<Boolean> validator;
  private final Integer elements;

  public PrimitiveBooleanArrayLoadSaveValidator(IFieldLoadSaveValidator<Boolean> fieldValidator) {
    this(fieldValidator, null);
  }

  public PrimitiveBooleanArrayLoadSaveValidator(
      IFieldLoadSaveValidator<Boolean> fieldValidator, Integer elements) {
    validator = fieldValidator;
    this.elements = elements;
  }

  @Override
  public boolean[] getTestObject() {
    int max = elements == null ? new Random().nextInt(100) + 50 : elements;
    boolean[] result = new boolean[max];
    for (int i = 0; i < max; i++) {
      result[i] = validator.getTestObject();
    }
    return result;
  }

  @Override
  public boolean validateTestObject(boolean[] original, Object actual) throws HopException {
    if (original.getClass().isAssignableFrom(actual.getClass())) {
      boolean[] otherList = (boolean[]) actual;
      if (original.length != otherList.length) {
        return false;
      }
      for (int i = 0; i < original.length; i++) {
        if (!this.validator.validateTestObject(original[i], otherList[i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
}
