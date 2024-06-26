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

package org.apache.hop.pipeline.transforms.switchcase;

import java.util.ArrayList;
import java.util.Set;
import org.apache.hop.core.IRowSet;

public class ContainsKeyToRowSetMap extends KeyToRowSetMap {
  protected ArrayList<String> list = new ArrayList<>();

  protected ContainsKeyToRowSetMap() {
    super();
  }

  @Override
  public Set<IRowSet> get(Object value) {
    String valueStr = (String) value;
    for (String key : list) {
      if (valueStr.contains(key)) {
        return super.get(key);
      }
    }
    return null;
  }

  @Override
  protected void put(Object key, IRowSet rowSet) {
    super.put(key, rowSet);
    list.add((String) key);
  }

  @Override
  public boolean containsKey(Object key) {
    String keyStr = (String) key;
    for (String value : list) {
      if (keyStr.contains(value)) {
        return true;
      }
    }
    return false;
  }
}
