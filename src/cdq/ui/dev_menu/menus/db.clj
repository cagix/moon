(ns cdq.ui.dev-menu.menus.db
  (:require [cdq.db :as db]
            [cdq.editor.overview-table]
            [cdq.editor.widget.map]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.string :as str]
            [clojure.vis-ui.widget :as widget]))

(defn- open-editor-overview-window!
  [{:keys [ctx/stage]
    :as ctx}
   property-type]
  (let [window (widget/window {:title "Edit"
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true})
        on-clicked-id (fn [id
                           {:keys [ctx/db
                                   ctx/stage]
                            :as ctx}]
                        (stage/add! stage (actor/build (cdq.editor.widget.map/property-editor-window ctx (db/get-raw db id)))))]
    (table/add! window (cdq.editor.overview-table/create ctx
                                                         property-type
                                                         on-clicked-id))
    (.pack window)
    (stage/add! stage window)))

(defn create [{:keys [ctx/db]} _]
  (for [property-type (sort (db/property-types db))]
    {:label (str/capitalize (name property-type))
     :on-click (fn [_actor ctx]
                 (open-editor-overview-window! ctx property-type))}))
