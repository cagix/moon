(ns cdq.db.schema.one-to-one
  (:require [cdq.db :as db]
            [cdq.db.property :as property]
            [cdq.graphics :as graphics]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [com.badlogic.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [com.badlogic.gdx.scenes.scene2d.ui.window :as window]))

(defn malli-form [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (property/type->id-namespace property-type)}])

(defn create-value [_ property-id db]
  (db/build db property-id))

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (group/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (widget-group/pack! (window/find-ancestor table)))]
    (table/add-rows!
     table
     [[(when-not property-id
         {:actor {:actor/type :actor.type/text-button
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
                                                    (actor/remove! (window/find-ancestor actor))
                                                    (redo-rows ctx id))})))}})]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (graphics/texture-region graphics (property/image property))
               image-widget (scene2d/build
                             {:actor/type :actor.type/image
                              :image/object texture-region
                              :actor/user-object property-id})]
           {:actor (actor/add-tooltip! image-widget (property/tooltip property))}
           image-widget))]
      [(when property-id
         {:actor {:actor/type :actor.type/text-button
                  :text "-"
                  :on-clicked (fn [_actor ctx]
                                (redo-rows ctx nil))}})]])))

(defn create [[_ property-type] property-id ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defn value [_  widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       first))
