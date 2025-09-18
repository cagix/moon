(ns cdq.tx.open-editor-overview
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.editor.overview-table :as overview-table]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}
   property-type]
  (stage/add! stage (scene2d/build
                     {:actor/type :actor.type/window
                      :title "Edit"
                      :modal? true
                      :close-button? true
                      :center? true
                      :close-on-escape? true
                      :pack? true
                      :rows (overview-table/create ctx
                                                   property-type
                                                   (fn [id ctx]
                                                     (ctx/handle-txs! ctx [[:tx/open-property-editor (db/get-raw db id)]])))
                      })))
