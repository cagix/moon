(ns cdq.tx.open-editor-overview
  (:require [cdq.ctx :as ctx]
            [cdq.editor.overview-table]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.vis-ui.widget :as widget]))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}
   property-type]
  (stage/add! stage (doto (widget/window {:title "Edit"
                                          :modal? true
                                          :close-button? true
                                          :center? true
                                          :close-on-escape? true})
                      (table/add! (cdq.editor.overview-table/create
                                   ctx
                                   property-type
                                   (fn [id ctx]
                                     (ctx/handle-txs! ctx [[:tx/open-property-editor id]]))))
                      (.pack))))
