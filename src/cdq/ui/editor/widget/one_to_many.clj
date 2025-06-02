(ns cdq.ui.editor.widget.one-to-many
  (:require [cdq.ui.editor.overview-table]
            [cdq.ui.editor.widget :as widget]
            [clojure.db :as db]
            [clojure.property :as property]
            [clojure.ui :as ui]
            [clojure.ui.stage :as stage]
            [clojure.utils :refer [pprint-to-str]]))

(defn- add-one-to-many-rows [{:keys [ctx/db]
                              :as ctx}
                             table property-type property-ids]
  (let [redo-rows (fn [ctx property-ids]
                    (ui/clear-children! table)
                    (add-one-to-many-rows ctx table property-type property-ids)
                    (ui/pack-ancestor-window! table))]
    (ui/add-rows!
     table
     [[(ui/text-button "+"
                       (fn [_actor {:keys [ctx/stage] :as ctx}]
                         (let [window (ui/window {:title "Choose"
                                                  :modal? true
                                                  :close-button? true
                                                  :center? true
                                                  :close-on-escape? true})
                               clicked-id-fn (fn [id ctx]
                                               (.remove window)
                                               (redo-rows ctx (conj property-ids id)))]
                           (ui/add! window (cdq.ui.editor.overview-table/create ctx property-type clicked-id-fn))
                           (.pack window)
                           (stage/add! stage window))))]
      (for [property-id property-ids]
        (let [property (db/build db property-id ctx)
              image-widget (ui/image->widget (property/image property)
                                             {:id property-id})]
          (ui/add-tooltip! image-widget (pprint-to-str property))))
      (for [id property-ids]
        (ui/text-button "-"
                        (fn [_actor ctx]
                          (redo-rows ctx (disj property-ids id)))))])))

(defmethod widget/create :s/one-to-many [[_ property-type] property-ids ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (add-one-to-many-rows ctx table property-type property-ids)
    table))

(defmethod widget/value :s/one-to-many [_ widget _schemas]
  (->> (ui/children widget)
       (keep ui/user-object)
       set))
