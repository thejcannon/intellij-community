// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.idea.AppMode;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import org.jetbrains.annotations.ApiStatus;

public final class WhatsNewUtil {
  @ApiStatus.Internal
  public static boolean isWhatsNewAvailable() {
    return ApplicationInfoEx.getInstanceEx().isShowWhatsNewOnUpdate() && !AppMode.isRemoteDevHost();
  }
}