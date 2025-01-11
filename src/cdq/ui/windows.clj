(ns cdq.ui.windows
  (:require [gdl.ui :as ui]
            [gdl.utils :as utils]))

(defn create [{:keys [gdl/config] :as context}]
  (ui/group {:id :windows
             :actors (map (fn [create]
                            ((utils/require-ns-resolve create) context))
                          (::actors config))}))
