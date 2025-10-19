(ns cdq.game.open-editor
  (:require [cdq.db :as db]
            [cdq.ui.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]}
   property-type]
  (stage/add-actor! stage
                    {:type :actor/editor-overview-window
                     :db db
                     :graphics graphics
                     :property-type property-type
                     :clicked-id-fn (fn [_actor id {:keys [ctx/stage] :as ctx}]
                                      (stage/add-actor! stage
                                                        {:type :actor/editor-window
                                                         :ctx ctx
                                                         :property (db/get-raw db id)}))}))
