(ns cdq.create.editor
  (:require [cdq.g :as g]
            [cdq.ui.editor]
            [cdq.ui.editor.overview-table]))

(defn do! [ctx]
  (extend (class ctx)
    g/EditorWindow
    {:open-editor-window! cdq.ui.editor/open-editor-window!
     :edit-property! (fn [ctx property]
                       (g/add-actor! ctx (cdq.ui.editor/editor-window property ctx)))
     :property-overview-table cdq.ui.editor.overview-table/create})
  ctx)
