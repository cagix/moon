(ns cdq.tx.open-editor-overview
  (:require [cdq.db :as db]
            [cdq.editor.overview-table]
            [cdq.editor-window]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.vis-ui.widget :as widget]))

(defn- property-type-overview-window
  [ctx property-type]
  (let [window (widget/window {:title "Edit"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true})
        on-clicked-id (fn [id
                           {:keys [ctx/db
                                   ctx/stage]
                            :as ctx}]
                        (stage/add! stage (actor/build (cdq.editor-window/property-editor-window ctx (db/get-raw db id)))))]
    (table/add! window (cdq.editor.overview-table/create ctx property-type on-clicked-id))
    (.pack window)
    window))

(defn do!
  [{:keys [ctx/stage]
    :as ctx}
   property-type]
  (stage/add! stage (property-type-overview-window ctx property-type)))
