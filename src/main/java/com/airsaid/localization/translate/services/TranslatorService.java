/*
 * Copyright 2021 Airsaid. https://github.com/airsaid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.airsaid.localization.translate.services;

import com.airsaid.localization.translate.AbstractTranslator;
import com.airsaid.localization.translate.impl.baidu.BaiduTranslator;
import com.airsaid.localization.translate.impl.google.GoogleTranslator;
import com.airsaid.localization.translate.impl.microsoft.MicrosoftTranslator;
import com.airsaid.localization.translate.impl.youdao.YoudaoTranslator;
import com.airsaid.localization.translate.lang.Lang;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author airsaid
 */
@Service
public final class TranslatorService {

  private static final Logger LOG = Logger.getInstance(TranslatorService.class);

  private AbstractTranslator selectedTranslator;
  private final AbstractTranslator defaultTranslator;
  private final TranslationCacheService cacheService;
  private final Map<String, AbstractTranslator> translators;
  private boolean isEnableCache = true;

  public TranslatorService() {
    translators = new LinkedHashMap<>();

    GoogleTranslator googleTranslator = new GoogleTranslator();
    translators.put(googleTranslator.getKey(), googleTranslator);
    defaultTranslator = googleTranslator;

    MicrosoftTranslator microsoftTranslator = new MicrosoftTranslator();
    translators.put(microsoftTranslator.getKey(), microsoftTranslator);

    BaiduTranslator baiduTranslator = new BaiduTranslator();
    translators.put(baiduTranslator.getKey(), baiduTranslator);

    YoudaoTranslator youdaoTranslator = new YoudaoTranslator();
    translators.put(youdaoTranslator.getKey(), youdaoTranslator);

    cacheService = TranslationCacheService.getInstance();
  }

  @NotNull
  public static TranslatorService getInstance() {
    return ServiceManager.getService(TranslatorService.class);
  }

  public AbstractTranslator getDefaultTranslator() {
    return defaultTranslator;
  }

  public Map<String, AbstractTranslator> getTranslators() {
    return translators;
  }

  public void setSelectedTranslator(@NotNull AbstractTranslator selectedTranslator) {
    if (this.selectedTranslator != selectedTranslator) {
      LOG.info(String.format("setTranslator: %s", selectedTranslator));
      this.selectedTranslator = selectedTranslator;
    }
  }

  @Nullable
  public AbstractTranslator getSelectedTranslator() {
    return selectedTranslator;
  }

  public void doTranslateByAsync(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text, @NotNull Consumer<String> consumer) {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      final String translatedText = doTranslate(fromLang, toLang, text);
      ApplicationManager.getApplication().invokeLater(() ->
          consumer.accept(translatedText));
    });
  }

  public String doTranslate(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text) {
    LOG.info(String.format("doTranslate fromLang: %s, toLang: %s, text: %s", fromLang, toLang, text));

    if (isEnableCache) {
      String cacheResult = cacheService.get(getCacheKey(fromLang, toLang, text));
      if (!cacheResult.isEmpty()) {
        LOG.info(String.format("doTranslate cache result: %s", cacheResult));
        return cacheResult;
      }
    }

    String result = selectedTranslator.doTranslate(fromLang, toLang, text);
    LOG.info(String.format("doTranslate result: %s", result));
    cacheService.put(getCacheKey(fromLang, toLang, text), result);
    return result;
  }

  public void setUseGoogleComHost(boolean isUseGoogleComHost) {
    GoogleTranslator.setUseComHost(isUseGoogleComHost);
  }

  public void setEnableCache(boolean isEnableCache) {
    this.isEnableCache = isEnableCache;
  }

  public boolean isEnableCache() {
    return isEnableCache;
  }

  public void setMaxCacheSize(int maxCacheSize) {
    cacheService.setMaxCacheSize(maxCacheSize);
  }

  private String getCacheKey(@NotNull Lang fromLang, @NotNull Lang toLang, @NotNull String text) {
    return fromLang.getCode() + "_" + toLang.getCode() + "_" + text;
  }

}
