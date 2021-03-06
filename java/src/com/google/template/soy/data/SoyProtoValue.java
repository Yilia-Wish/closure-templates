/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.data;

import com.google.protobuf.Message;

/** A value object containing a proto. */
public interface SoyProtoValue extends SoyValue {

  /** Returns the underlying message. */
  Message getProto();

  /**
   * Gets a value for the field for the underlying proto object. Not intended for general use.
   *
   * @param name The proto field name.
   * @return The value of the given field for the underlying proto object, or NullData if either the
   *     field does not exist or the value is not set in the underlying proto (according to the jspb
   *     semantics)
   */
  SoyValue getProtoField(String name);

}
