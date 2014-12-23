/**
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
package de.kaufhof.jsonhomeclient

/**
 * Represents link relation types (as defined by [RFC5988]).
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
abstract sealed class LinkRelationType {
  val name: String
}

/**
 * For a direct link there is exactly one resource of that relation type associated with the API
 * (e.g. "/widgets/").
 *
 * @param name the name of the link relation type (e.g. "http://example.org/rel/widgets")
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
case class DirectLinkRelationType(val name: String) extends LinkRelationType

/**
 * For a template link there are zero to many such resources (e.g. "/widgets/{widget_id}"),
 * see also <a href="http://tools.ietf.org/html/rfc6570">RFC6570</a>.
 *
 * @param name the name of the link relation type (e.g. "http://example.org/rel/widget")
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
case class TemplateLinkRelationType(val name: String) extends LinkRelationType
