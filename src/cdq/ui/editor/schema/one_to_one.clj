(ns cdq.ui.editor.schema.one-to-one
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.ui.editor.overview-window :as editor-overview-window]
            [cdq.ui.tooltip :as tooltip]
            [cdq.graphics.textures :as textures]
            [clojure.scene2d.vis-ui.image :as image]
            [clojure.scene2d.vis-ui.text-button :as text-button]
            [clojure.scene2d.build.table :as build-table]
            [clojure.scene2d.ui.table :as table]
            [cdq.ui.window :as window])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (.clearChildren table)
                    (add-one-to-one-rows ctx table property-type id)
                    (.pack (window/find-ancestor table)))]
    (table/add-rows!
     table
     [[(when-not property-id
         {:actor (text-button/create
                  {:text "+"
                   :on-clicked (fn [_actor {:keys [ctx/db
                                                   ctx/graphics
                                                   ctx/stage]}]
                                 (.addActor
                                  stage
                                  (editor-overview-window/create
                                   {:db db
                                    :graphics graphics
                                    :property-type property-type
                                    :clicked-id-fn (fn [actor id ctx]
                                                     (Actor/.remove (window/find-ancestor actor))
                                                     (redo-rows ctx id))})))})})]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (textures/texture-region graphics (property/image property))
               image-widget (image/create
                             {:image/object texture-region
                              :actor/user-object property-id})]
           {:actor (tooltip/add! image-widget (property/tooltip property))}
           image-widget))]
      [(when property-id
         {:actor (text-button/create
                  {:text "-"
                   :on-clicked (fn [_actor ctx]
                                 (redo-rows ctx nil))})})]])))

(defn create [[_ property-type] property-id ctx]
  (let [table (build-table/create
               {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defn value [_  widget _schemas]
  (->> (.getChildren widget)
       (keep Actor/.getUserObject)
       first))
