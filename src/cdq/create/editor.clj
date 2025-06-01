(ns cdq.create.editor
  (:require [cdq.editor]
            [cdq.stage :as stage]
            [cdq.ui.editor]
            [cdq.ui.editor.overview-table]))

(defn do! [ctx]
  (extend (class ctx)
    cdq.editor/Editor
    {:open-editor-window! cdq.ui.editor/open-editor-window!
     :edit-property! (fn [ctx property]
                       (stage/add-actor! ctx (cdq.ui.editor/editor-window property ctx)))
     :property-overview-table cdq.ui.editor.overview-table/create})
  ctx)
