(ns cdq.db.schema.one-to-many
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.ui.tooltip :as tooltip]
            [cdq.graphics.textures :as textures]
            [clojure.scene2d :as scene2d]
            [cdq.ui.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [cdq.ui.window :as window])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn malli-form [[_ property-type] _schemas]
  [:set [:qualified-keyword {:namespace (property/type->id-namespace property-type)}]])

(defn create-value [_ property-ids db]
  (set (map (partial db/build db) property-ids)))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (.clearChildren table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (.pack (window/find-ancestor table)))]
    (table/add-rows!
     table
     [[{:actor {:actor/type :actor.type/text-button
                :text "+"
                :on-clicked (fn [_actor {:keys [ctx/db
                                                ctx/graphics
                                                ctx/stage]}]
                              (stage/add!
                               stage
                               (scene2d/build
                                {:actor/type :actor.type/editor-overview-window
                                 :db db
                                 :graphics graphics
                                 :property-type property-type
                                 :clicked-id-fn (fn [actor id ctx]
                                                  (Actor/.remove (window/find-ancestor actor))
                                                  (redo-rows ctx (conj property-ids id)))})))}}]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (textures/texture-region graphics (property/image property))
              image-widget (scene2d/build
                            {:actor/type :actor.type/image
                             :image/object texture-region
                             :actor/user-object property-id})]
          {:actor (tooltip/add! image-widget (property/tooltip property))}))
      (for [id property-ids]
        {:actor {:actor/type :actor.type/text-button
                 :text "-"
                 :on-clicked (fn [_actor ctx]
                               (redo-rows ctx (disj property-ids id)))}})])))

(defn create [[_ property-type] property-ids ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defn value [_  widget _schemas]
  (->> (.getChildren widget)
       (keep Actor/.getUserObject)
       set))
