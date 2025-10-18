(ns cdq.game.open-editor
  (:require [cdq.db :as db]
            [cdq.ui.editor.overview-window :as editor-overview-window]
            [cdq.ui.editor.window :as editor-window]
            [cdq.ui.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]}
   property-type]
  (stage/add-actor!
   stage
   (editor-overview-window/create
    {:db db
     :graphics graphics
     :property-type property-type
     :clicked-id-fn (fn [_actor id {:keys [ctx/stage] :as ctx}]
                      (stage/add-actor! stage
                                        (editor-window/create-editor-window
                                         {:ctx ctx
                                          :property (db/get-raw db id)})))})))
