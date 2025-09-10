(ns cdq.editor.widget.one-to-many
  (:require [cdq.db :as db]
            [cdq.editor.overview-table]
            [cdq.gdx.graphics :as graphics]
            [cdq.property :as property]
            [cdq.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.vis-ui.widget :as widget]))

(defn- add-one-to-many-rows
  [{:keys [ctx/db
           ctx/graphics]}
   table
   property-type
   property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (group/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (window/pack-ancestors! table))]
    (table/add-rows!
     table
     [[(widget/text-button "+"
                           (fn [_actor {:keys [ctx/stage] :as ctx}]
                             (let [window (widget/window {:title "Choose"
                                                          :modal? true
                                                          :close-button? true
                                                          :center? true
                                                          :close-on-escape? true})
                                   clicked-id-fn (fn [id ctx]
                                                   (.remove window)
                                                   (redo-rows ctx (conj property-ids id)))]
                               (table/add! window (cdq.editor.overview-table/create ctx property-type clicked-id-fn))
                               (.pack window)
                               (stage/add! stage window))))]
      (for [property-id property-ids]
        (let [property (db/get-raw db property-id)
              texture-region (graphics/texture-region graphics (property/image property))
              image-widget (widget/image texture-region {:id property-id})]
          (tooltip/add! image-widget (utils/pprint-to-str property))))
      (for [id property-ids]
        (widget/text-button "-"
                            (fn [_actor ctx]
                              (redo-rows ctx (disj property-ids id)))))])))

(defn create [[_ property-type]  _attribute property-ids ctx]
  (let [table (widget/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defn value [_  _attribute widget _schemas]
  (->> (group/children widget)
       (keep actor/user-object)
       set))
