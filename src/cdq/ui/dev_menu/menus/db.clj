(ns cdq.ui.dev-menu.menus.db
  (:require [cdq.db :as db]
            [clojure.string :as str]))

(defn create [{:keys [ctx/db]} open-editor-overview-window!]
  (for [property-type (sort (db/property-types db))]
    {:label (str/capitalize (name property-type))
     :on-click (fn [_actor ctx]
                 ; TODO stage/add can do here ???
                 ((requiring-resolve open-editor-overview-window!) ctx property-type))}))
