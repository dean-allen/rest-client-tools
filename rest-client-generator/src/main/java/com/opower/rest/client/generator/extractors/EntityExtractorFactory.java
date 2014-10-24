/**
 *    Copyright 2014 Opower, Inc.
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 **/
package com.opower.rest.client.generator.extractors;

import java.lang.reflect.Method;

/**
 * Create an EntityExtractor based on a method. This will allow different
 * factories to be used for different purposes, including custom user driven
 * factories.
 *
 * @author <a href="mailto:sduskis@gmail.com">Solomon Duskis</a>
 *
 * @see EntityExtractor , DefaultObjectEntityExtractor,
 * ResponseObjectEntityExtractor
 */
public interface EntityExtractorFactory {
    @SuppressWarnings("unchecked")
    public EntityExtractor createExtractor(Method method);
}
