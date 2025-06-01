(ns cdq.create.editor
  (:require [cdq.editor]
            [cdq.ui.editor]
            [cdq.ui.editor.overview-table]
            [gdl.ui.stage :as stage]))

(defn do! [ctx]
  (extend (class ctx)
    cdq.editor/Editor
    {:open-editor-window! cdq.ui.editor/open-editor-window!
     :edit-property! (fn [{:keys [ctx/stage] :as ctx} property]
                       (stage/add! stage (cdq.ui.editor/editor-window property ctx)))
     :property-overview-table cdq.ui.editor.overview-table/create})
  ctx)
