(ns cdq.ui.dev-menu.menus.db
  (:require [cdq.db :as db]
            [cdq.editor]
            [clojure.string :as str]))

(defn create [db]
  (for [property-type (sort (db/property-types db))]
    {:label (str/capitalize (name property-type))
     :on-click (fn [_actor ctx]
                 (cdq.editor/open-editor-overview-window! ctx property-type))}))
