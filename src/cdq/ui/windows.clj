(ns cdq.ui.windows
  (:require [gdl.ui :as ui]
            [gdl.utils :as utils]))

(defn create [context actors]
  (ui/group {:id :windows
             :actors (map (fn [create] (create context)) actors)}))
