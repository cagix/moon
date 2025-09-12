(ns cdq.schema.one-to-one
  (:require [cdq.ctx.db :as db]
            [cdq.editor :as editor]
            [cdq.gdx.graphics :as graphics]
            [cdq.ui.editor.property :as property]
            [cdq.property]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.vis-ui.widget :as widget]))

(defn create-value [_ property-id db]
  (db/build db property-id))

(defn malli-form [[_ property-type] _schemas]
  [:qualified-keyword {:namespace (cdq.property/type->id-namespace property-type)}])

(defn- add-one-to-one-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-id]
  (let [redo-rows (fn [ctx id]
                    (group/clear-children! table)
                    (add-one-to-one-rows ctx table property-type id)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(when-not property-id
         (widget/text-button "+"
                             (fn [_actor {:keys [ctx/stage] :as ctx}]
                               (let [window (widget/window {:title "Choose"
                                                            :modal? true
                                                            :close-button? true
                                                            :center? true
                                                            :close-on-escape? true})
                                     clicked-id-fn (fn [id ctx]
                                                     (.remove window)
                                                     (redo-rows ctx id))]
                                 (table/add! window (editor/overview-table ctx property-type clicked-id-fn))
                                 (.pack window)
                                 (stage/add! stage window)))))]
      [(when property-id
         (let [property (db/get-raw db property-id)
               texture-region (graphics/texture-region graphics (property/image property))
               image-widget (widget/image texture-region
                                          {:id property-id})]
           (tooltip/add! image-widget (utils/pprint-to-str property))
           image-widget))]
      [(when property-id
         (widget/text-button "-"
                             (fn [_actor ctx]
                               (redo-rows ctx nil))))]])))

(defn create [[_ property-type] property-id ctx]
  (let [table (widget/table {:cell-defaults {:pad 5}})]
    (add-one-to-one-rows ctx table property-type property-id)
    table))

(defn value [_  widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       first))
