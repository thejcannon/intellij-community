// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.python.ui

import com.intellij.openapi.progress.ModalTaskOwner
import com.intellij.openapi.progress.TaskCancellation
import com.intellij.openapi.progress.runWithModalProgressBlocking
import com.intellij.openapi.progress.withModalProgress
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PySdkBundle

/**
 * Runs [code] in background under the modal dialog
 */
@RequiresEdt
@RequiresBlockingContext
fun <T> pyModalBlocking(modalTaskOwner: ModalTaskOwner = ModalTaskOwner.guess(), code: () -> T): T =
  runWithModalProgressBlocking(modalTaskOwner, PySdkBundle.message("python.sdk.run.wait"), TaskCancellation.nonCancellable()) {
    code.invoke()
  }

suspend fun <T> pyModalSuspend(modalTaskOwner: ModalTaskOwner = ModalTaskOwner.guess(), code: () -> T): T =
  withModalProgress(modalTaskOwner, PySdkBundle.message("python.sdk.run.wait"), TaskCancellation.nonCancellable()) {
    code.invoke()
  }