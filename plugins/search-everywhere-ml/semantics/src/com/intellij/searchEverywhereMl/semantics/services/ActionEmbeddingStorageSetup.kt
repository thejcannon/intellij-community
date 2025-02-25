package com.intellij.searchEverywhereMl.semantics.services

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.searchEverywhereMl.semantics.SemanticSearchBundle
import com.intellij.searchEverywhereMl.semantics.indices.EmbeddingSearchIndex
import java.util.concurrent.atomic.AtomicReference

class ActionEmbeddingStorageSetup(
  project: Project,
  private val index: EmbeddingSearchIndex,
  private val setupTaskIndicator: AtomicReference<ProgressIndicator>
) : Task.Backgroundable(project, SETUP_TITLE) {
  override fun run(indicator: ProgressIndicator) {
    val indexableActionIds = ActionEmbeddingsStorage.getIndexableActionIds()
    if (checkEmbeddingsReady(indexableActionIds)) return

    val embeddingService = runBlockingCancellable { LocalEmbeddingServiceProvider.getInstance().getService() } ?: return
    // Cancel the previous embeddings calculation task if it's not finished
    setupTaskIndicator.getAndSet(indicator)?.cancel()

    indicator.text = SETUP_TITLE
    var indexedActionsCount = index.size
    val totalIndexableActionsCount = indexableActionIds.size

    val actionManager = ActionManager.getInstance() as ActionManagerImpl
    indexableActionIds
      .asSequence()
      .filter { it !in index }
      .map { it to actionManager.getActionOrStub(it) }
      .chunked(BATCH_SIZE)
      .forEach { batch ->
        ProgressManager.checkCanceled()
        val actionIds = batch.map { (id, _) -> id }
        val texts = batch.map { (_, action) -> action!!.templateText!! }
        val embeddings = runBlockingCancellable { embeddingService.embed(texts) }.map { it.normalized() }
        index.addEntries(actionIds zip embeddings)
        indexedActionsCount += embeddings.size
        indicator.fraction = indexedActionsCount.toDouble() / totalIndexableActionsCount
      }

    // If the indicator is already changed, then the current task is already canceled
    if (setupTaskIndicator.compareAndSet(indicator, null)) indicator.cancel()
  }

  override fun onCancel() {
    ApplicationManager.getApplication().executeOnPooledThread { index.saveToDisk() }
  }

  private fun checkEmbeddingsReady(indexableActionIds: Set<String>): Boolean {
    index.filterIdsTo(indexableActionIds)
    return index.checkAllIdsPresent(indexableActionIds)
  }

  companion object {
    private val SETUP_TITLE = SemanticSearchBundle.getMessage("search.everywhere.ml.semantic.actions.generation.label")
    private const val BATCH_SIZE = 1
  }
}