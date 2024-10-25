(ns ^:no-doc moon.widgets.properties-overview
  (:require [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :as component :refer [defc]]
            [moon.db :as db]
            [moon.property :as property]))

(defn- property-widget [{:keys [property/id] :as props} clicked-id-fn extra-info-text scale]
  (let [on-clicked #(clicked-id-fn id)
        button (if-let [image (property/->image props)]
                 (ui/image-button image on-clicked {:scale scale})
                 (ui/text-button (name id) on-clicked))
        top-widget (ui/label (or (and extra-info-text (extra-info-text props)) ""))
        stack (ui/stack [button top-widget])]
    (ui/add-tooltip! button #(component/->info props))
    (a/set-touchable! top-widget :disabled)
    stack))

(defc :widgets/properties-overview
  (component/create [[_ property-type clicked-id-fn]]
    (let [{:keys [sort-by-fn
                  extra-info-text
                  columns
                  image/scale]} (property/overview property-type)
          properties (db/all property-type)
          properties (if sort-by-fn
                       (sort-by sort-by-fn properties)
                       properties)]
      (ui/table
       {:cell-defaults {:pad 5}
        :rows (for [properties (partition-all columns properties)]
                (for [property properties]
                  (try (property-widget property clicked-id-fn extra-info-text scale)
                       (catch Throwable t
                         (throw (ex-info "" {:property property} t))))))}))))
