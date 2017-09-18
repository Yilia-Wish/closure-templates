/*
 * Copyright 2017 Google Inc.
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
package com.google.template.soy.shared.internal;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.template.soy.coredirectives.IdDirective;
import com.google.template.soy.coredirectives.NoAutoescapeDirective;
import com.google.template.soy.jbcsrc.restricted.SoyJbcSrcFunction;
import com.google.template.soy.jbcsrc.restricted.SoyJbcSrcPrintDirective;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.jssrc.restricted.SoyJsSrcPrintDirective;
import com.google.template.soy.pysrc.restricted.SoyPySrcFunction;
import com.google.template.soy.pysrc.restricted.SoyPySrcPrintDirective;
import com.google.template.soy.shared.restricted.SoyFunction;
import com.google.template.soy.shared.restricted.SoyJavaFunction;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;
import com.google.template.soy.shared.restricted.SoyPrintDirective;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SharedModuleTest {

  // pysrc has intentionally not implemented a few directives.
  private static final ImmutableSet<String> PYSRC_DIRECTIVE_BLACKLIST =
      ImmutableSet.of(NoAutoescapeDirective.NAME, IdDirective.NAME);

  private Injector injector;

  @Before
  public void setUp() {
    injector = Guice.createInjector(new SharedModule());
  }

  @Test
  public void testBuiltinPluginsSupportAllBackends() throws Exception {
    for (SoyPrintDirective directive : injector.getInstance(new Key<Set<SoyPrintDirective>>() {})) {
      assertThat(directive).isInstanceOf(SoyJsSrcPrintDirective.class);
      assertThat(directive).isInstanceOf(SoyJavaPrintDirective.class);
      assertThat(directive).isInstanceOf(SoyJbcSrcPrintDirective.class);
      if (!PYSRC_DIRECTIVE_BLACKLIST.contains(directive.getName())) {
        assertThat(directive).isInstanceOf(SoyPySrcPrintDirective.class);
      }
    }
  }

  @Test
  public void testFunctionsSupportAllBackends() {
    for (SoyFunction function : injector.getInstance(new Key<Set<SoyFunction>>() {})) {
      assertThat(function).isInstanceOf(SoyJsSrcFunction.class);
      assertThat(function).isInstanceOf(SoyJavaFunction.class);
      assertThat(function).isInstanceOf(SoyJbcSrcFunction.class);
      assertThat(function).isInstanceOf(SoyPySrcFunction.class);
    }
  }

  // This test serves to document exactly which escaping directives do and do not support streaming
  // in jbcsrc.  If someone adds a new one, they will need to update this test and document why
  // it doesn't support streaming.
  @Test
  public void testStreamingPrintDirectives() throws Exception {
    ImmutableSet.Builder<String> streamingPrintDirectives = ImmutableSet.builder();
    ImmutableSet.Builder<String> nonStreamingPrintDirectives = ImmutableSet.builder();
    for (SoyPrintDirective directive : injector.getInstance(new Key<Set<SoyPrintDirective>>() {})) {
      if (directive instanceof SoyJbcSrcPrintDirective.Streamable) {
        streamingPrintDirectives.add(directive.getName());
      } else {
        nonStreamingPrintDirectives.add(directive.getName());
      }
    }
    assertThat(streamingPrintDirectives.build())
        .containsExactly(
            "|escapeHtml",
            "|blessStringAsTrustedResourceUrlForLegacy",
            "|id",
            "|escapeCssString",
            "|normalizeHtml",
            "|escapeJsString",
            "|escapeJsRegex",
            "|text",
            "|noAutoescape",
            "|normalizeUri");
    assertThat(nonStreamingPrintDirectives.build())
        .containsExactly(
            // These all make sense to be streaming, though it might make sense to just skip
            // some of the deprecated ones.
            "|changeNewlineToBr",
            "|insertWordBreaks",
            "|truncate",
            "|escapeHtmlRcdata",
            // TODO(b/18260376): this one should be fixable since it is just doing % encoding, but
            // the current strategy relies on a guava class that doesn't support streaming.  May
            // require completely reimplementing it :/
            "|escapeUri",
            // These can't be made streaming because it would require a complex state machine or
            // they require knowing the full content to work.  For example all the filters, which
            // generally validate via a regular expression.
            // As below, we may want to make these support the Streamable interface but internally
            // buffer.
            "|filterHtmlElementName",
            "|filterCssValue",
            "|escapeJsValue",
            "|filterHtmlAttributes",
            "|filterNormalizeUri",
            "|filterNormalizeMediaUri",
            "|filterTrustedResourceUri",
            "|filterImageDataUri",
            "|filterTelUri",
            // These two could be made streaming, it would require some refactoring of the
            // Sanitizers.stripHtmlTags method but it is probably a good idea.
            "|escapeHtmlAttribute",
            "|escapeHtmlAttributeNospace",
            // These one could possibly be made streaming, but it would require a lot of work.
            // We might want to add a warning if log directives are lost due to this.
            // Or we could possibly add a version of 'streaming' which actually just buffers all
            // the commands and preserves them.  This way logging directives could be preserved
            // through the print directive.
            "|cleanHtml",
            "|bidiSpanWrap",
            "|bidiUnicodeWrap",
            "|formatNum");
  }
}
