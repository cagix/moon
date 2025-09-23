(ns cdq.tx.open-editor-overview
  (:require [cdq.editor :as editor]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]}
   {:keys [property-type
           clicked-id-fn]}]
  (stage/add! stage (scene2d/build
                     {:actor/type :actor.type/window
                      :title "Edit"
                      :modal? true
                      :close-button? true
                      :center? true
                      :close-on-escape? true
                      :pack? true
                      :rows (editor/overview-table-rows db
                                                        graphics
                                                        property-type
                                                        clicked-id-fn)})))
